package com.shutup.socketcamera.socket_transfer;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * Created by shutup on 16/5/14.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private String TAG = "CameraPreview";
    private SurfaceHolder mHolder = null;
    private Camera mCamera = null;
    private ServerActivity mServerActivity = null;
    private int prevSizeW = 0;
    private int prevSizeH = 0;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mServerActivity = (ServerActivity) context;
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
// If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Camera.Parameters parameters = mCamera.getParameters();
            List<Integer> supportPreviewFormats = parameters.getSupportedPreviewFormats();
            for (int i = 0; i < supportPreviewFormats.size(); i++) {
                Integer temp = supportPreviewFormats.get(i);
                Log.d(TAG, "supportPreviewFormat: " + temp);
            }
            List<String> supportFocusModes = parameters.getSupportedFocusModes();
            for (int i = 0; i < supportFocusModes.size(); i++) {
                String mode = supportFocusModes.get(i);
                if (mode.equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
            }
            parameters.setPreviewSize(320,240);
            mCamera.setParameters(parameters);
            mCamera.setPreviewCallback(mServerActivity);
            mCamera.startPreview();
            prevSizeW = mCamera.getParameters().getPreviewSize().width;
            prevSizeH = mCamera.getParameters().getPreviewSize().height;

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
        }
    }
}
