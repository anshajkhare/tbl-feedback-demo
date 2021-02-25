package in.tbl.talkbacklabs_feedback.audio;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

import in.tbl.talkbacklabs_feedback.VoiceFeedback;
import in.tbl.talkbacklabs_feedback.utils.PermissionsUtil;

public class AudioListener {
    private static final String TAG = AudioListener.class.getSimpleName();
    private static final String URL_ENDPOINT_FORMAT = "wss://api.symbl.ai/v1/realtime/insights/%s?access_token=%s"; // uniqueMeetingId & accessToken are required

    private WebSocketClient mWebSocketClient;
    private String fullTranscript;
    private AudioRecorder mAudioRecorder;
    private Activity mActivity;
    private ExecutorService mListenerService;
    private AudioCallback mAudioCallback;
    private final AudioRecorder.Callback mCallback = new AudioRecorder.Callback() {
        @Override
        public void onVoiceStart() {
            super.onVoiceStart();
            mAudioCallback.onVoiceStart();
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (mWebSocketClient != null && mWebSocketClient.isOpen()) {
                sendSpeechSample(data);
            }
        }

        @Override
        public void onVoiceEnd() {
            super.onVoiceEnd();
            stopListening();
            mAudioCallback.onVoiceEnd();
        }
    };

    private class SpeechRecognitionSocket extends WebSocketClient {

        public SpeechRecognitionSocket(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakeData) {
            JSONObject handshakeMessage = createHandshakeMessage();
            mWebSocketClient.send(handshakeMessage.toString());
        }

        @Override
        public void onMessage(String message) {
            try {
                JSONObject data = new JSONObject(message);
                if (data.getString("type").equals("message")) {
                    JSONObject msg = data.getJSONObject("message");
                    if (msg.getString("type").equals("recognition_result")) {
                        String transcript = msg.getJSONObject("punctuated") != null ? msg.getJSONObject("punctuated").getString("transcript") : "";
                        if (msg.getBoolean("isFinal")) {
                            // Final result
                            mAudioCallback.onFullTranscript(transcript);
                            fullTranscript = transcript;
                            getSentiment(fullTranscript);
                        } else {
                            // Partial response
                            mAudioCallback.onPartialTranscript(transcript);
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {

        }

        @Override
        public void onError(Exception ex) {

        }
    }

    private void getSentiment(final String transcript) {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                HttpsURLConnection connection = null;
                try {

                    URL url = new URL("https://nlapi.expert.ai/v2/analyze/standard/en/sentiment");
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Authorization", "Bearer " + getToken());
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");

                    String data = "{\n  \"document\": {\n    \"text\": \"" + transcript + "\"\n  }\n}";

                    byte[] out = data.getBytes(StandardCharsets.UTF_8);

                    OutputStream stream = connection.getOutputStream();
                    stream.write(out);

                    if (connection.getResponseCode() == 200) {
                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(connection.getInputStream(), "utf-8")
                        );

                        String message = br.readLine();
                        if (message == null || message.length() == 0) {
                            throw new IOException(
                                    String.format(
                                            "Invalid response from %s. Code: %d",
                                            connection.getURL().toString(),
                                            connection.getResponseCode()
                                    )
                            );
                        }
                        JSONObject response = new JSONObject(message);
                        Double overallSentiment = response.getJSONObject("data").getJSONObject("sentiment").getDouble("overall");
                        mAudioCallback.onSentimentAvailable((float) ((overallSentiment + 100)/20));
                    } else {
                        throw new IOException(
                                String.format(
                                        "Error connecting to %s. Code: %d",
                                        url.toString(),
                                        connection.getResponseCode()
                                )
                        );
                    }
                } catch (IOException | JSONException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
        });
    }

    private String getToken() {
        return VoiceFeedback.getEToken();
    }

    private JSONObject createHandshakeMessage() {
        JSONObject handshake = new JSONObject();
        try {
            JSONObject config = new JSONObject();
            config.put("confidenceThreshold", 0.9);
            config.put("languageCode", "en-IN");

            handshake.put("type", "start_request");
            handshake.put("config", config);
        } catch (JSONException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return handshake;
    }

    private URI getUrl() {
        String uri = String.format(Locale.ENGLISH, URL_ENDPOINT_FORMAT, UUID.randomUUID().toString(), VoiceFeedback.getSpeechAccessToken());
        return URI.create(uri);
    }

    private synchronized void sendSpeechSample(byte[] sample) {
        if (mWebSocketClient != null && mWebSocketClient.isOpen()) mWebSocketClient.send(sample);
    }

    public void startListening(Activity activity, AudioCallback audioCallback) {
        mActivity = activity;
        mAudioCallback = audioCallback;
        // Check for audio permission
        if (activity.checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            handleBeginRecognition();
        } else {
            PermissionsUtil.getInstance().checkPermission(activity, this, Manifest.permission.RECORD_AUDIO);
        }
    }

    public void stopListening() {
        mListenerService.submit(new Runnable() {
            @Override
            public void run() {
                if (mAudioRecorder != null) {
                    mAudioRecorder.stop();
                }
                if (mWebSocketClient.isOpen()) {
                    JSONObject stopRequestObject = new JSONObject();
                    try {
                        stopRequestObject.put("type", "stop_request");
                    } catch (JSONException e) {
                        // Pass
                    }
                    mWebSocketClient.send(stopRequestObject.toString());
                    mWebSocketClient.close();
                }
            }
        });
    }

    public void handleBeginRecognition() {
        mListenerService = Executors.newFixedThreadPool(1);
        mListenerService.submit(new Runnable() {
            @Override
            public void run() {
                mWebSocketClient = new SpeechRecognitionSocket(getUrl());
                mAudioRecorder = new AudioRecorder(mCallback);
                mAudioRecorder.start();
                try {
                    mWebSocketClient.connectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void handlePermissionDenied() {
        mAudioCallback.onAudioPermissionDenied();
        Toast.makeText(mActivity, "To use the voice feature, please provide audio permission", Toast.LENGTH_SHORT).show();
    }

    public interface AudioCallback {
        void onVoiceStart();
        void onPartialTranscript(String partialTranscript);
        void onFullTranscript(String transcript);
        void onSentimentAvailable(float sentimentRating);
        void onVoiceEnd();
        void onAudioPermissionDenied();
    }
}
