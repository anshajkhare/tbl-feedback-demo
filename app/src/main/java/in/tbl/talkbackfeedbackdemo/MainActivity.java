package in.tbl.talkbackfeedbackdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import in.tbl.talkbacklabs_feedback.VoiceFeedback;
import in.tbl.talkbacklabs_feedback.audio.AudioListener;

public class MainActivity extends AppCompatActivity {

    private AudioListener audioListener;
    private boolean isListening = false;
    private TextView tv;
    private RatingBar ratingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = findViewById(R.id.transcript_text);
        tv.setTextColor(0xff7f7f7f);
        tv.setText("Your spoken text will appear here.");
        ratingBar = findViewById(R.id.rating_bar);
        audioListener = new AudioListener();
        String token = "<TOKEN_VALUE>";
        VoiceFeedback.initialize(token);
    }

    public void startListening(View view) {
        if (isListening) {
            audioListener.stopListening();
            ((Button) view).setText("Tell us your experience!");
            isListening = false;
        } else {
            audioListener.startListening(this, new AudioListener.AudioCallback() {
                @Override
                public void onVoiceStart() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv.setTextColor(0xff7f7f7f);
                            tv.setText("Your spoken text will appear here.");
                            ratingBar.setVisibility(View.INVISIBLE);
                        }
                    });
                }

                @Override
                public void onPartialTranscript(String partialTranscript) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv.setTextColor(0xFF000000);
                            tv.setText(partialTranscript);
                        }
                    });
                }

                @Override
                public void onFullTranscript(String transcript) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv.setTextColor(0xFF000000);
                            tv.setText(transcript);
                        }
                    });
                }

                @Override
                public void onSentimentAvailable(float sentimentRating) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ratingBar.setMax(10);
                            ratingBar.setRating(sentimentRating/2);
                            ratingBar.setVisibility(View.VISIBLE);
                        }
                    });
                }

                @Override
                public void onVoiceEnd() {

                }

                @Override
                public void onAudioPermissionDenied() {

                }
            });
            ((Button) view).setText("End review");
            isListening = true;
        }
    }
}