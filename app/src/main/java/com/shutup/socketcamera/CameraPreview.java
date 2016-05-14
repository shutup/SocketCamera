package com.shutup.socketcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
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
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private String TAG = "CameraPreview";
    private SurfaceHolder mHolder = null;
    private Camera mCamera = null;
    private RenderScript rs;

    private int prevSizeW;
    private int prevSizeH;
    private String header = "HTTP/1.1 200 OK\r\n" +
//            "Connection: close\r\n" +
//            "Server: Net-camera-1-0\r\n" +
//            "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0,post-check=0, max-age=0\r\n" +
//            "Pragma: no-cache\r\n" +
            "Content-Type:multipart/x-mixed-replace;boundary=www.shutup.com\r\n\r\n";

    private String frameHeader = "--www.shutup.com\r\nContent-Type: image/jpeg\nContent-Length: %d\n\n";

    private ServerSocket serverSocket = null;
    private Socket client = null;
    private OutputStream outputStream = null;

    private boolean isRun = false;
    private boolean isFirst = false;
    private byte[] dataPic;
    private boolean isOk = false;

    public CameraPreview(Context context, Camera camera) {
        super(context);

        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

//        rs = RenderScript.create(context);
//        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));


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
            parameters.setPreviewSize(320,240);
            mCamera.setParameters(parameters);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    YuvImage yuv = new YuvImage(data, ImageFormat.NV21, prevSizeW, prevSizeH, null);
                    Rect r = new Rect(0, 0, prevSizeW, prevSizeH);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    yuv.compressToJpeg(r, 100, baos);

                    if (isOk) {
                        if (!isFirst) {
                            isFirst = true;
                            int len = baos.size();
                            String frameHeaderTemp = String.format(frameHeader, len);
                            dataPic = new byte[header.getBytes().length + frameHeaderTemp.getBytes().length + len];
                            int dst = 0;
                            System.arraycopy(header.getBytes(), 0, dataPic, dst, header.getBytes().length);
                            dst += header.getBytes().length;
                            System.arraycopy(frameHeaderTemp.getBytes(), 0, dataPic, dst, frameHeaderTemp.getBytes().length);
                            dst += frameHeaderTemp.getBytes().length;
                            System.arraycopy(baos.toByteArray(), 0, dataPic, dst, len);
                            isOk = false;
                        } else {
                            int len = baos.size();
                            String frameHeaderTemp = String.format(frameHeader, len);
                            Log.d(TAG, "onPreviewFrame: "+frameHeaderTemp);
                            dataPic = new byte[frameHeaderTemp.getBytes().length + len];
                            int dst = 0;
                            System.arraycopy(frameHeaderTemp.getBytes(), 0, dataPic, dst, frameHeaderTemp.getBytes().length);
                            dst += frameHeader.getBytes().length;
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
                Log.d(TAG, "server socket: "+serverSocket.isBound());
                client = serverSocket.accept();
                Log.d(TAG, "run: "+ client.isConnected());
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
}
