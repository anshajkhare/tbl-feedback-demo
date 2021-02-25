package in.tbl.talkbacklabs_feedback.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.os.Bundle;

public class PermissionsActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(this, getIntent().getStringArrayExtra(PermissionsUtil.PERMISSION_KEY), PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PermissionsUtil.getInstance().handlePermissionGranted();
            } else {
                PermissionsUtil.getInstance().handlePermissionDenied();
            }
        }
        finish();
    }
}