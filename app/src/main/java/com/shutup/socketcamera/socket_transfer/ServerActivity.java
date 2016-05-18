package com.shutup.socketcamera.socket_transfer;

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

import com.shutup.socketcamera.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mSocket != null) {
            if (!mSocket.isClosed()){
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (mServerSocket != null) {
            if (!mServerSocket.isClosed()) {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        isOk = false;
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
                    mCamera.setPreviewCallback(this);
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
                mCamera.setPreviewCallback(this);
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
                //listen on 8888
                mServerSocket = new ServerSocket(8888);
                mSocket = mServerSocket.accept();
                mOutputStream = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (isRun) {
                if (isOk) {
                    try {
                        //first write the data length to the outputStream ,it need a int size 4
                        mOutputStream.write(ByteBuffer.allocate(4).putInt(baos.size()).array());
                        //then write the data to the outputStream
                        mOutputStream.write(baos.toByteArray());
                        mOutputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        //if have any exception,close the thread
                        isRun = false;
                    }
                    isOk = false;
                }
            }
        }
    }
}
