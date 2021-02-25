package in.tbl.talkbacklabs_feedback.utils;

import android.app.Activity;
import android.content.Intent;

import in.tbl.talkbacklabs_feedback.audio.AudioListener;

public class PermissionsUtil {
    public static final String PERMISSION_KEY = "permission_key";
    private static PermissionsUtil sIntance;
    private AudioListener mListener;

    public static PermissionsUtil getInstance() {
        if (sIntance == null) sIntance = new PermissionsUtil();
        return sIntance;
    }

    public void checkPermission(Activity activity, AudioListener listener, String permission) {
        mListener = listener;
        Intent intent = new Intent(activity, PermissionsActivity.class);
        intent.putExtra(PERMISSION_KEY, new String[]{permission});
        activity.startActivity(intent);
    }

    void handlePermissionGranted() {
        mListener.handleBeginRecognition();
    }

    void handlePermissionDenied() {
        mListener.handlePermissionDenied();
    }
}
