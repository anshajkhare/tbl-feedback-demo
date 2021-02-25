# Voice Feedback Demo

This is a demo to showcase the talkbacklabs-feedback SDK. The SDK would allow any developer to add a voice feedback feature to their app.

To try out the demo,
- Sign up on the expert.ai dev portal and create an access token
- Clone this repository and run the app by following the instructions provided below

The app has to initialize the SDK using a token generated from the expert.ai developer portal. It can be done in the Application class or the MainActivity.

```
VoiceFeedback.initialize("<token>");
```

When the user is ready to speak out the review to the app, the app would need to instantiate a `AudioListener` instance and call the `startListening()` API, and pass the current activity and an instance of `AudioCallback`.
The callback will notify the app as and when the user spoken text transcript and the sentiment is available.

The `sentimentRating` is the user's review on a scale of 1 to 10.

```
audioListener = new AudioListener();
audioListener.startListening(this, new AudioListener.AudioCallback() {
                @Override
                public void onVoiceStart() {
                    
                }

                @Override
                public void onPartialTranscript(String partialTranscript) {
                // This is a partial transcript
                }

                @Override
                public void onFullTranscript(String transcript) {
                // This is the fully available transcript
                }

                @Override
                public void onSentimentAvailable(float sentimentRating) {
                // Sentiment rating of the user, given as a rating from a scale of 1 to 10
                }

                @Override
                public void onVoiceEnd() {

                }

                @Override
                public void onAudioPermissionDenied() {
                // User denied audio permission
                }
            });
```

When the user has finished speaking and wants to end their review, the app should call `stopListening()` on the AudioListener instance.

```
audioListener.stopListening();
```
