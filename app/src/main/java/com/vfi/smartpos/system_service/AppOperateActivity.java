package com.vfi.smartpos.system_service;

import android.app.Activity;
import android.os.Bundle;

//Project internal imports

public class AppOperateActivity extends Activity {
    public static AppOperateActivity instance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//      本应用中不需要出现界面，所以不做SetContentView
        setContentView(R.layout.activity_main);
        instance = this;
    }

    @Override
    public void onBackPressed() {
        //屏蔽返回键
    }
}
