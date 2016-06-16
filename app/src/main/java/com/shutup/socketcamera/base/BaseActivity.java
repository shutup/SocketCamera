package com.shutup.socketcamera.base;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.shutup.socketcamera.R;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
    }
}
