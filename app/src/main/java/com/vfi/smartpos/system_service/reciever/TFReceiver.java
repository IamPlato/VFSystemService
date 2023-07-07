package com.vfi.smartpos.system_service.reciever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.storage.StorageManager;
import android.util.Log;

import com.vfi.smartpos.system_service.VfiServiceApp;
import com.vfi.smartpos.system_service.util.MmkvUtils;

import java.lang.reflect.Method;
import java.util.List;

import static android.content.Context.STORAGE_SERVICE;

public class TFReceiver extends BroadcastReceiver {
    public static final String TAG = "tfReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            Log.d(TAG, "tfcard mounted");
            Log.d(TAG, String.valueOf(isUsb(context)));
            //if tf disabled,do unmount
            if(!MmkvUtils.getMmkvTfEnable()){
                if (!isUsb(context)) {
                    unMount();
                }
            }

        } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
            Log.d(TAG, "tfcard unmounted");
        }
    }

    public static void unMount() {
        try {
            Log.d(TAG, "start unMount");
            StorageManager mSD = (StorageManager) VfiServiceApp.getContext().getSystemService(STORAGE_SERVICE);
            Log.d(TAG, String.valueOf(mSD));
            List<Object> list = (List<Object>) StorageManager.class.getMethod("getVolumes").invoke(mSD);
            Log.d(TAG, "list:" + list.toString());
            for (int i = 0; i < list.size(); i++) {
                Object volume = list.get(i);
                if (volume != null) {
                    String id = (String) Class.forName("android.os.storage.VolumeInfo").getMethod("getId").invoke(volume);
                    Log.d(TAG, "id:" + id);
                    int type = (int) Class.forName("android.os.storage.VolumeInfo").getMethod("getType").invoke(volume);
                    Log.d(TAG, "type:" + type);
                    if (type == 0) {
                        Log.d(TAG, "begin");
                        Log.d(TAG, mSD.toString() + id);
                        StorageManager.class.getMethod("unmount", String.class).invoke(mSD, id);
                        Log.d(TAG, "finish");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, String.valueOf(e));
        }
    }

    public static boolean isUsb(Context context) {
        boolean usb = false;
        StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        Class<?> volumeInfoClazz;
        Class<?> diskInfoClaszz;
        try {
            volumeInfoClazz = Class.forName("android.os.storage.VolumeInfo");
            diskInfoClaszz = Class.forName("android.os.storage.DiskInfo");
            Method StorageManager_getVolumes = Class.forName("android.os.storage.StorageManager").getMethod("getVolumes");
            Method VolumeInfo_GetDisk = volumeInfoClazz.getMethod("getDisk");
            Method DiskInfo_IsUsb = diskInfoClaszz.getMethod("isUsb");
            List<Object> List_VolumeInfo = (List<Object>) StorageManager_getVolumes.invoke(mStorageManager);
            assert List_VolumeInfo != null;
            for (int i = 0; i < List_VolumeInfo.size(); i++) {
                Object volumeInfo = List_VolumeInfo.get(i);
                Object diskInfo = VolumeInfo_GetDisk.invoke(volumeInfo);
                if (diskInfo == null) continue;
                usb = (boolean) DiskInfo_IsUsb.invoke(diskInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return usb;
    }


}
