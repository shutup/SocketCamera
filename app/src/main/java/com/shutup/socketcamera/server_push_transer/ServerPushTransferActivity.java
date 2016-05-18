package com.shutup.socketcamera.server_push_transer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import com.shutup.socketcamera.R;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class ServerPushTransferActivity extends AppCompatActivity {

    @InjectView(R.id.mainAsServerPushBtn)
    Button mMainAsServerPushBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_push_transfer);
        ButterKnife.inject(this);
    }

    @OnClick(R.id.mainAsServerPushBtn)
    public void onClick() {
        startActivity(new Intent(this,ServerPushActivity.class));
    }
}
