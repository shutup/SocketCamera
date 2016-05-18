package com.shutup.socketcamera.socket_transfer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.shutup.socketcamera.R;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class ClientActivity extends AppCompatActivity {

    @InjectView(R.id.video_screen)
    ImageView mVideoScreen;
    @InjectView(R.id.clientCloseBtn)
    Button mClientCloseBtn;

    private Handler mHandler = null;
    private String server_addr = null;
    private Socket mSocket = null;
    private InputStream mInputStream = null;
    private boolean isRun = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        ButterKnife.inject(this);
        initEvent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInputStream != null) {
            try {
                mInputStream.close();
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

        isRun = false;
    }
    private void initEvent() {
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == 1) {
                    mVideoScreen.setImageBitmap((Bitmap) msg.obj);
                }
                return true;
            }
        });
        isRun = true;
        new ClientReader().start();
    }

    @OnClick(R.id.clientCloseBtn)
    public void onClick() {
        onBackPressed();
    }

    class ClientReader extends Thread {
        private boolean isFull = false;
        private long startTime = 0;
        private int fpsCount = 0;
        @Override
        public void run() {
            super.run();
            Intent intent = getIntent();
            server_addr = intent.getStringExtra("server_addr");
            if (server_addr == null || server_addr.equalsIgnoreCase("")) {
            }
            String[] serverComponts = server_addr.split(":");
            if (serverComponts.length == 2) {
                try {
                    mSocket = new Socket(serverComponts[0], Integer.parseInt(serverComponts[1]));
                    if (mSocket.isConnected()) {
                        mInputStream = mSocket.getInputStream();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    isRun = false;
                }
            }
            while (isRun) {
                if (!isFull) {
                    startTime = System.nanoTime();
                    isFull = true;
                }
                byte[] sizeArray = new byte[4];
                try {
                    mInputStream.read(sizeArray);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int picLength = ByteBuffer.wrap(sizeArray).asIntBuffer().get();
                if (picLength == 0){
                    isRun = false;
                }
                Log.d("ClientReader", "picLength:" + picLength);
                byte[] b = new byte[picLength];
                try {
                    int totalLen = 0;
                    int bufferSize = 4 * 1024;
                    //when the read totalLen is less than the picLength
                    while (totalLen < picLength) {
                        int len = 0;
                        //if the left data is less than bufferSize,read them all ,
                        //else read them by bufferSize
                        if (bufferSize >= picLength - totalLen) {
                            len = mInputStream.read(b, totalLen, picLength - totalLen);
                        } else {
                            len = mInputStream.read(b, totalLen, bufferSize);
                        }
                        totalLen += len;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ByteArrayInputStream inputStream = new ByteArrayInputStream(b);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                Message message = mHandler.obtainMessage();
                message.what = 1;
                if (bitmap != null) {
                    message.obj = bitmap;
                    mHandler.sendMessage(message);
                    if (fpsCount < 60){
                        fpsCount++;
                    }else {
                        isFull = false;
                        long endTime = System.nanoTime();
                        long gap = endTime - startTime;
                        long fps = 1000000000 / (gap / fpsCount);
                        Log.d("ClientActivity", "gap:" + gap);
                        Log.d("ClientActivity", "fps:" + fps);
                        fpsCount = 0;
                    }
                }
            }
        }
    }
}
