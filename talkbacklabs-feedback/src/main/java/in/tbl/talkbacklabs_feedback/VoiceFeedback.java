package in.tbl.talkbacklabs_feedback;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class VoiceFeedback {
    private static final String TAG = VoiceFeedback.class.getSimpleName();
    private static final String AUTHENTICATE_URL = "https://api.symbl.ai/oauth2/token:generate";
    private static String sAccessToken;
    private static String sEToken;

    public static void initialize(String token) {
        sEToken = token;
        authenticate();
    }

    public static String getSpeechAccessToken() {
        return sAccessToken;
    }

    public static String getEToken() {
        return sEToken;
    }

    private static void authenticate() {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                HttpsURLConnection connection = null;
                try {
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("type", "application");
                    requestBody.put("appId", BuildConfig.APP_ID);
                    requestBody.put("appSecret", BuildConfig.APP_SECRET);

                    URL url = new URL(AUTHENTICATE_URL);
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    connection.setChunkedStreamingMode(0);
                    connection.getOutputStream().write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                    connection.getOutputStream().close();

                    if (connection.getResponseCode() == 200) {
                        Log.d(TAG, "Initialization successful!");
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
                        JSONObject responseBody = new JSONObject(message);
                        sAccessToken = responseBody.getString("accessToken");
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
}
