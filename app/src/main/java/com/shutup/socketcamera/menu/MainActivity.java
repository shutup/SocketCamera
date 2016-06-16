package com.shutup.socketcamera.menu;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.shutup.socketcamera.R;
import com.shutup.socketcamera.server_push_transer.ServerPushTransferActivity;
import com.shutup.socketcamera.socket_transfer.SocketTransferActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainActivity extends AppCompatActivity {

    private MenuAdapter mMenuAdapter = null;
    private List<MenuItem> menus = null;
    @InjectView(R.id.recyclerView)
    RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        initMenus();
        initEvents();
    }

    private void initEvents() {
        mMenuAdapter = new MenuAdapter(MainActivity.this, menus);
        mMenuAdapter.setOnItemClickLitener(new MenuAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(View view, int position) {
                MenuItem menuItem = menus.get(position);
                startActivity(menuItem.getJumpIntent());
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        mRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        mRecyclerView.setAdapter(mMenuAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL_LIST));
    }

    private void initMenus() {
        menus = new ArrayList<>();
        menus.add(new MenuItem("Http Push",new Intent(MainActivity.this, ServerPushTransferActivity.class)));
        menus.add(new MenuItem("Socket Transfer",new Intent(MainActivity.this, SocketTransferActivity.class)));
    }
}
