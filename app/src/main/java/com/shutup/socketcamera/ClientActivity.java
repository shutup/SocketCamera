package com.shutup.socketcamera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

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
        @Override
        public void run() {
            super.run();
            Intent intent = getIntent();
            server_addr = intent.getStringExtra("server_addr");
            if (server_addr == null || server_addr.equalsIgnoreCase("")) {
//                Toast.makeText(this, "no server apply", Toast.LENGTH_SHORT).show();
            }
            String[] serverComponts = server_addr.split(":");
            if (serverComponts.length == 2) {
                try {
                    mSocket = new Socket(serverComponts[0],Integer.parseInt(serverComponts[1]));
                    if (mSocket.isConnected()){
//                        Toast.makeText(this, "server connected!", Toast.LENGTH_SHORT).show();
                        mInputStream = mSocket.getInputStream();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            while (isRun) {
                Bitmap bitmap = BitmapFactory.decodeStream(mInputStream);
                Message message = mHandler.obtainMessage();
                message.what =1;
                if (bitmap != null) {
                    message.obj = bitmap;
                    mHandler.sendMessage(message);
                }
            }
        }
    }
}
