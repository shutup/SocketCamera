package com.shutup.socketcamera.menu;

import android.content.Intent;

/**
 * Created by shutup on 16/6/16.
 */
public class MenuItem {
    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public Intent getJumpIntent() {
        return jumpIntent;
    }

    public void setJumpIntent(Intent jumpIntent) {
        this.jumpIntent = jumpIntent;
    }

    private String menuName;
    private Intent jumpIntent;

    public MenuItem(String menuName, Intent jumpIntent) {
        this.menuName = menuName;
        this.jumpIntent = jumpIntent;
    }
}
