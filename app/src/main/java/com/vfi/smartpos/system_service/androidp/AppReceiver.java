package com.vfi.smartpos.system_service.androidp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.RemoteException;
import android.util.Log;

import com.vfi.smartpos.system_service.SystemManager;

public class AppReceiver extends BroadcastReceiver {

    private static final String TAG = AppReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        if (intent != null) {
            int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            if (status == PackageInstaller.STATUS_SUCCESS) {
                String packName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);
                Log.i(TAG, "install success, package name is " + packName);
                if (packName != null && packName.length() > 0) {
                    try {
                        SystemManager.getInstance().cbInstallObserver(packName, 1);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Log.e(TAG, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
            }
        }
    }
}
