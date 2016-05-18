package com.shutup.socketcamera.socket_transfer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.shutup.socketcamera.R;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class SocketTransferActivity extends AppCompatActivity {

    @InjectView(R.id.mainServerAddressEditText)
    EditText mMainServerAddressEditText;
    @InjectView(R.id.mainAsClientBtn)
    Button mMainAsClientBtn;
    @InjectView(R.id.mainAsServerBtn)
    Button mMainAsServerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket_transfer);
        ButterKnife.inject(this);
    }

    @OnClick({R.id.mainAsClientBtn, R.id.mainAsServerBtn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.mainAsClientBtn:
                Intent intent = new Intent(this, ClientActivity.class);
                intent.putExtra("server_addr", mMainServerAddressEditText.getText().toString().trim());
                startActivity(intent);
                break;
            case R.id.mainAsServerBtn:
                startActivity(new Intent(this, ServerActivity.class));
                break;
        }
    }
}
