package com.shutup.socketcamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * Created by shutup on 16/5/12.
 */
public class CameraPreviewServerPush extends SurfaceView implements SurfaceHolder.Callback {
    private String TAG = "CameraPreview";

    private Context mContext = null;
    private SurfaceHolder mHolder = null;
    private Camera mCamera = null;

    private int prevSizeW;
    private int prevSizeH;
    private String header = "HTTP/1.1 200 OK\r\n" +
            "Connection: close\r\n" +
            "Server: Net-camera-1-0\r\n" +
            "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0,post-check=0, max-age=0\r\n" +
            "Pragma: no-cache\r\n" +
            "Content-Type:multipart/x-mixed-replace;boundary=www.shutup.com\r\n\r\n";

    private String frameHeader = "--www.shutup.com\r\n" +
            "Content-Type: image/jpeg\r\n" +
            "Content-Length: %d\r\n\r\n";

    private ServerSocket serverSocket = null;
    private Socket client = null;
    private OutputStream outputStream = null;

    private boolean isRun = false;
    private boolean isFirst = false;
    private byte[] dataPic;
    private boolean isOk = false;

    public CameraPreviewServerPush(Context context, Camera camera) {
        super(context);
        mContext = context;
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
            setCameraDisplayOrientation((Activity) mContext, 0, mCamera);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(final SurfaceHolder holder, int format, int width, int height) {
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
            parameters.setPreviewSize(320, 240);
            mCamera.setParameters(parameters);

            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (isOk) {
                        if (!isFirst) {
                            //in first call we need to send the response HTTP Header + Frame Header + data
                            isFirst = true;
//                            byte[] temp = rotateYUV420Degree90(data, prevSizeW,prevSizeH);
                            YuvImage yuv = new YuvImage(data, ImageFormat.NV21, prevSizeW, prevSizeH, null);
                            Rect r = new Rect(0, 0, prevSizeW, prevSizeH);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            yuv.compressToJpeg(r, 100, baos);

                            int len = baos.size();
                            int headerLen = header.length();
                            String frameHeaderTemp = String.format(frameHeader, len);
                            int frameHeaderTempLen = frameHeaderTemp.length();
                            dataPic = new byte[headerLen + frameHeaderTempLen + len];
                            int dst = 0;
                            System.arraycopy(header.getBytes(), 0, dataPic, dst, headerLen);
                            dst += headerLen;
                            System.arraycopy(frameHeaderTemp.getBytes(), 0, dataPic, dst, frameHeaderTempLen);
                            dst += frameHeaderTempLen;
                            System.arraycopy(baos.toByteArray(), 0, dataPic, dst, len);
                            isOk = false;
                        } else {
                            //then when next call ,we only need to send Frame Header + data
//                            byte[] temp = rotateYUV420Degree90(data, prevSizeW,prevSizeH);
                            YuvImage yuv = new YuvImage(data, ImageFormat.NV21, prevSizeW, prevSizeH, null);
                            Rect r = new Rect(0, 0, prevSizeW, prevSizeH);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            yuv.compressToJpeg(r, 100, baos);
                            int len = baos.size();
                            String frameHeaderTemp = String.format(frameHeader, len);
                            int frameHeaderTempLen = frameHeaderTemp.length();
                            dataPic = new byte[frameHeaderTempLen + len];
                            int dst = 0;
                            System.arraycopy(frameHeaderTemp.getBytes(), 0, dataPic, dst, frameHeaderTempLen);
                            dst += frameHeaderTempLen;
                            System.arraycopy(baos.toByteArray(), 0, dataPic, dst, len);
                            isOk = false;
                        }
                    }
                }
            });
            mCamera.startPreview();
            prevSizeW = mCamera.getParameters().getPreviewSize().width;
            prevSizeH = mCamera.getParameters().getPreviewSize().height;

            isRun = true;
            new WriterThread().start();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
        }

        isRun = false;
    }

    class WriterThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                serverSocket = new ServerSocket(8888);
                Log.d(TAG, "server socket: " + serverSocket.isBound());
                client = serverSocket.accept();
                Log.d(TAG, "run: " + client.isConnected());
                isOk = true;
                outputStream = client.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (isRun) {
                try {
                    if (!isOk) {
                        if (dataPic != null) {
                            outputStream.write(dataPic);
                            isOk = true;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }
}
