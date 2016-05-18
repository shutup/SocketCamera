package com.shutup.socketcamera.server_push_transer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.shutup.socketcamera.R;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class ServerPushActivity extends AppCompatActivity {

    private static String TAG = "ServerPushActivity";
    @InjectView(R.id.camera_preview_layout)
    FrameLayout mCameraPreviewLayout;
    @InjectView(R.id.serverPushCloseBtn)
    Button mServerPushCloseBtn;

    private Camera mCamera;
    private CameraPreviewServerPush mPreview;
    private int CAMERA_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_push);
        ButterKnife.inject(this);
        initEvent();
    }
    private void initEvent() {

        openTheCameraPreview();
    }

    private void openTheCameraPreview() {
        if (checkCameraHardware(ServerPushActivity.this)) {
            int currentPermissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (currentPermissionStatus != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);
            } else {
                if (mCamera == null) {
                    mCamera = getCameraInstance();
                    mPreview = new CameraPreviewServerPush(ServerPushActivity.this, mCamera);
                    mCameraPreviewLayout.addView(mPreview);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                mCamera = getCameraInstance();
                mPreview = new CameraPreviewServerPush(ServerPushActivity.this, mCamera);
                mCameraPreviewLayout.addView(mPreview);
            } else {
                // Permission Denied
                Toast.makeText(this, "please give me the permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @OnClick(R.id.serverPushCloseBtn)
    public void onClick() {
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        // this device has a camera
// no camera on this device
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            Log.d(TAG, "getCameraInstance: " + e.toString());
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
}
