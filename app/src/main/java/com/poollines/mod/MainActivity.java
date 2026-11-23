package com.poollines.mod;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int OVERLAY_REQ_CODE = 1234;
    private static final int CAPTURE_REQ_CODE = 1235;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent permission = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(permission, OVERLAY_REQ_CODE);
        } else requestCapture();
    }

    private void requestCapture() {
        MediaProjectionManager manager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(), CAPTURE_REQ_CODE);
    }

    private void startServices(int resultCode, Intent data) {
        Intent overlay = new Intent(this, PoolOverlayService.class);
        overlay.putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode);
        overlay.putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(overlay);
        else startService(overlay);
        finish();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) requestCapture();
            else finish();
        } else if (requestCode == CAPTURE_REQ_CODE) {
            if (resultCode == RESULT_OK && data != null) startServices(resultCode, data);
            else finish();
        }
    }
}