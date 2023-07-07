package com.vfi.smartpos.system_service;

/**
 * Created by XC on 2016/12/21.
 */

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.tencent.mmkv.MMKV;
import com.verifone.smartpos.api.SdkApiHolder;

public class VfiServiceApp extends Application {
    private static Context serviceAppCtx;
    private static String TAG = "SystemManager";
    private static VfiServiceApp instance;
    private static boolean installStatus = true;
    private static boolean deleteStatus = true;
    public static final String BACKUP_CFG_PATH = "/oem/verifone/SystemService/";

    public static MMKV mmkvBootingAnimation;
    private static MMKV mmkvTF;

    public static boolean getInstallStatus() {
        Log.i(TAG, "getInstallStatus() --" + installStatus);
        return installStatus;
    }

    public static void setInstallStatus(boolean installStatus) {
        Log.i(TAG, "setInstallStatus() --" + installStatus);
        VfiServiceApp.installStatus = installStatus;
    }

    public static boolean getDeleteStatus() {
        Log.i(TAG, "getDeleteStatus() --" + deleteStatus);
        return deleteStatus;
    }

    public static void setDeleteStatus(boolean deleteStatus) {
        Log.i(TAG, "setDeleteStatus() --" + deleteStatus);
        VfiServiceApp.deleteStatus = deleteStatus;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        SdkApiHolder.getInstance().getDeviceMaster().init(this.getApplicationContext());

        Log.i(TAG, "-- Application onCreate() --");
        serviceAppCtx = getApplicationContext();

        MMKV.initialize(this, BACKUP_CFG_PATH);
        mmkvBootingAnimation = MMKV.mmkvWithID("ANIMATION");
        mmkvTF = MMKV.mmkvWithID("TF");

        //for test only
//        MmkvUtils.setMmkvLogo("I am Logo");
//        MmkvUtils.setMmkvSound("I am Sound");
//        MmkvUtils.setMmkvAnimation("I am Animation");

        instance = this;
    }

    public static Context getContext() {
        Log.i(TAG, "getContext()");
        return serviceAppCtx;
    }

    public static VfiServiceApp getInstance() {
        return instance;
    }

    public static void showToast(String toastMsg) {
        Message msg = msgHandler.obtainMessage(0);
        msg.getData().putString("msg", toastMsg);
        msg.sendToTarget();
    }

    private static Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg != null) {
                if (msg.getData() != null) {
                    Toast.makeText(VfiServiceApp.getContext(), msg.getData().getString("msg"), Toast.LENGTH_LONG).show();
                }
            }
        }

    };

    public static MMKV getMmkvBootingAnimation() {
        return mmkvBootingAnimation;
    }

    public static MMKV getMmkvTF() {
        return mmkvTF;
    }
}
