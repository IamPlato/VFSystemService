package com.vfi.smartpos.system_service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.vfi.smartpos.system_service.SystemManager;
import com.vfi.smartpos.system_service.aidl.ISystemManager;

/**
 * Created by XC on 2016/12/19.
 */

public class SystemService extends Service{
    private static String TAG = "SystemService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG, "systemService onCreate()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        //绑定服务
        Log.i(TAG, "systemService onBind()");

        //returns the interface
        return SystemManager.getInstance();
    }

    @Override
    public void onRebind(Intent intent) {
        //绑定服务
        Log.i(TAG, "systemService onRebind()");
        return ;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //解绑服务
        Log.i(TAG, "systemService onUnbind()");

        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "systemService onDestroy()");
    }
}
