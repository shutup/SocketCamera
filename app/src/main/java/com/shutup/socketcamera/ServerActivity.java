package com.shutup.socketcamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class ServerActivity extends AppCompatActivity implements Camera.PreviewCallback {

    private static String TAG = "ServerActivity";
    @InjectView(R.id.camera_preview_layout)
    FrameLayout mCameraPreviewLayout;
    @InjectView(R.id.serverCloseBtn)
    Button mServerCloseBtn;
    @InjectView(R.id.serverInfo)
    TextView mServerInfo;

    private Camera mCamera;
    private CameraPreview mPreview;
    private int CAMERA_REQUEST_CODE = 100;

    private ServerSocket mServerSocket = null;
    private Socket mSocket = null;
    private OutputStream mOutputStream = null;
    private boolean isRun = false;
    private boolean isOk = false;
    private ByteArrayOutputStream baos = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        ButterKnife.inject(this);

        initEvent();
    }

    private void initEvent() {
        isRun = true;
        new ServerWriter().start();

        openTheCameraPreview();
    }


    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
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

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //here we get the data in NV21 format
        //then we change the format to jpeg
        if (!isOk) {
            Camera.Size picSize = camera.getParameters().getPreviewSize();
            YuvImage yuv = new YuvImage(data, ImageFormat.NV21, picSize.width, picSize.height, null);
            Rect r = new Rect(0, 0, picSize.width, picSize.height);
            baos = new ByteArrayOutputStream();
            yuv.compressToJpeg(r, 100, baos);
            isOk = true;
            //we will send the data to the client
        }
    }

    @OnClick(R.id.serverCloseBtn)
    public void onClick() {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        onBackPressed();
    }

    private void openTheCameraPreview() {
        if (checkCameraHardware(ServerActivity.this)) {
            int currentPermissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (currentPermissionStatus != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);
            } else {
                if (mCamera == null) {
                    mCamera = getCameraInstance();
                    mPreview = new CameraPreview(ServerActivity.this, mCamera);
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
                mPreview = new CameraPreview(ServerActivity.this, mCamera);
                mCameraPreviewLayout.addView(mPreview);
            } else {
                // Permission Denied
                Toast.makeText(this, "please give me the permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    class ServerWriter extends Thread {

        @Override
        public void run() {
            super.run();
            try {
                mServerSocket = new ServerSocket(8888);
                mSocket = mServerSocket.accept();
                mOutputStream = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (isRun) {
                if (isOk) {
                    try {
                        mOutputStream.write(baos.toByteArray());
                        mOutputStream.flush();
                        isOk = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
