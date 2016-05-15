package com.shutup.socketcamera;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {


    @InjectView(R.id.mainServerAddressEditText)
    EditText mMainServerAddressEditText;
    @InjectView(R.id.mainAsClientBtn)
    Button mMainAsClientBtn;
    @InjectView(R.id.mainAsServerBtn)
    Button mMainAsServerBtn;
    @InjectView(R.id.mainAsServerPushBtn)
    Button mMainAsServerPushBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
    }

    @OnClick({R.id.mainAsClientBtn, R.id.mainAsServerBtn, R.id.mainAsServerPushBtn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.mainAsClientBtn:
                Intent intent = new Intent(MainActivity.this, ClientActivity.class);
                intent.putExtra("server_addr", mMainServerAddressEditText.getText().toString().trim());
                startActivity(intent);
                break;
            case R.id.mainAsServerBtn:
                startActivity(new Intent(MainActivity.this, ServerActivity.class));
                break;
            case R.id.mainAsServerPushBtn:
                startActivity(new Intent(MainActivity.this,ServerPushActivity.class));
                break;
        }
    }
}
