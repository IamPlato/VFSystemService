package com.verifone.androidsystemservicelib;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.vfi.smartpos.system_service.aidl.ISystemManager;

public class SystemServiceConnect {
    private static final String TAG = "SystemServiceConnect";
    private static final String ACTION = "com.vfi.smartpos.system_service";
    private static final String PACKAGE = "com.vfi.smartpos.system_service";
    private static final String CLASSNAME = "com.vfi.smartpos.system_service.SystemService";

    private static ISystemManager systemManager = null;
    private SystemServiceConnectListener connectListener;
    private Context mContext;

    public boolean bindSystemService(Context context, SystemServiceConnectListener connectListener) {
        if (mContext == null) {
            mContext = context;
        }
        if (connectListener != null) {
            this.connectListener = connectListener;
        }

        if (mContext == null) {
            Log.i(TAG, "context is null, system service disconnected.");
            return false;
        }
        Intent intent = new Intent();
        intent.setAction(ACTION);
        intent.setClassName(PACKAGE, CLASSNAME);

        boolean result = context.bindService(intent, mSystemServiceConnection, Context.BIND_AUTO_CREATE);
        if (!result) {
            Log.i(TAG, "system service connect failed");
        } else {
            Log.i(TAG, "system service connect success");
        }
        return result;
    }

    ServiceConnection mSystemServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "system service bind success");
            systemManager = ISystemManager.Stub.asInterface(iBinder);
            if (connectListener != null) {
                connectListener.onServiceConnected();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "system service disconnected.");
            systemManager = null;
            if (connectListener != null) {
                connectListener.onServiceDisconnected();
            }
        }
    };

    public ISystemManager getSystemManager() {
        return systemManager;
    }

    public boolean checkState() throws RemoteException {
        if (getSystemManager() == null) {
            Log.e(TAG, "bind system service ");
            bindSystemService(mContext, connectListener);
            throw new RemoteException("system service  bind failed, please try again later");
        }
        return true;
    }

    public void unBindService() {
        Log.e(TAG, "unBind system service");
        if (mContext !=null) {
            mContext.unbindService(mSystemServiceConnection);
        }
        mContext = null;
        systemManager = null;
    }
}
