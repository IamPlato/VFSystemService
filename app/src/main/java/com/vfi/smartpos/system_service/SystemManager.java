package com.vfi.smartpos.system_service;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import com.google.gson.Gson;
import com.socsi.SoSDKManager;
import com.verifone.exception.KeyException;
import com.verifone.exception.SDKException;
import com.verifone.smartpos.api.SdkApiHolder;
import com.verifone.smartpos.api.entities.terminal.DeviceInfo;
import com.verifone.smartpos.api.key.manager.IKeyManager;
import com.verifone.smartpos.api.system.ISystemSettings;
import com.verifone.smartpos.api.terminal.ITerminalMessage;
import com.verifone.smartpos.api.terminal.OnVerifySignCallback;
import com.verifone.smartpos.utils.AppUtils;
import com.verifone.smartpos.utils.StringUtil;
import com.verifone.smartpos.utils.SysProp;
import com.vfi.smartpos.system_service.aidl.IAppDeleteObserver;
import com.vfi.smartpos.system_service.aidl.IAppInstallObserver;
import com.vfi.smartpos.system_service.aidl.ISystemManager;
import com.vfi.smartpos.system_service.aidl.IVerifysignCallback;
import com.vfi.smartpos.system_service.aidl.networks.INetworkManager;
import com.vfi.smartpos.system_service.aidl.settings.ISettingsManager;
import com.vfi.smartpos.system_service.settings.SettingsManager;
import com.vfi.smartpos.system_service.util.AppOperateUtils;
import com.vfi.smartpos.system_service.util.BrightnessUtil;
import com.vfi.smartpos.system_service.util.LogFileRecordUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by XC on 2016/12/19.
 */

public class SystemManager extends ISystemManager.Stub {
    private String installerPkgName;
    private Boolean k21Installres;
    private boolean isSameAPP;
    private boolean homeState;
    private String installedVFServiceVer;
    private ITerminalMessage mTerminalMessage;
    private IKeyManager iKeyManager;

    private final String defaultLogcatBufferSize = "8M";
    @SuppressLint("SdCardPath")
    private final String defaultLogcatFilePath = "/sdcard/log/";
    private final String defaultLogcatRecordFilePath = "/sdcard/log/log_create_time_record.txt";
    private final String VFSERVICE_PKG = "com.vfi.smartpos.deviceservice";
    private static final String BASE_BAND_KEYWORD = "SLM755L_EU_MEIG";

    // the unique instance of this class
    public static synchronized SystemManager getInstance() {
        if (instance == null) {
            instance = new SystemManager();
        }
        return instance;
    }

/*    @Override
    public Bundle getDeviceInfo() {
        // store POS device information
        Bundle sDeviceInfo = new Bundle();
        sDeviceInfo.putString(DeviceInfoConstants.VENDOR, "VFI");
        sDeviceInfo.putString(DeviceInfoConstants.MODEL, "X990");
        sDeviceInfo.putString(DeviceInfoConstants.OS_VERSION, android.os.Build.VERSION.RELEASE);

        // Service Version
        PackageInfo packInfo = null;
        String strServiceVer = "";
        try {
            packInfo = VfiServiceApp.getContext().getPackageManager().getPackageInfo(VfiServiceApp.getContext().getPackageName(), 0);
            strServiceVer = packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        sDeviceInfo.putString(DeviceInfoConstants.ERVICE_VERSION, strServiceVer);

        // Terminal Serial Number(SN)
        String strTermSN = "";
//        DeviceInfo deviceInfo = new DeviceInfo();
//        strTermSN = deviceInfo.getProduceSN();

        strTermSN = Build.SERIAL;

        // use WI-FI MAC address represent for terminal serial number temporary,
        // will delete this block in future
        *//*----------delete start mark--------------*//*
//        String macInfo = ((WifiManager)VfiServiceApp.getContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getMacAddress();
//        macInfo = macInfo.toUpperCase(Locale.ENGLISH);
//        macInfo = macInfo.replace(':', '5');
//        //strTermSN = "VFI-" + macInfo.substring(9, 17);
//        strTermSN = macInfo.substring(9, 17);
//        Log.d(TAG, "snString:" + BinaryUtils.BinaryToHexString(strTermSN.getBytes()));
		*//*-----------delete end mark------------------*//*
        sDeviceInfo.putString(DeviceInfoConstants.SN, strTermSN);

        Log.d(TAG, "S_VER=" + android.os.Build.VERSION.RELEASE);
        Log.d(TAG, "SN= " + strTermSN);

        return sDeviceInfo;
    }*/

    @Override
    public void installApp(String apkPath, IAppInstallObserver observer, String installerPackageName) throws RemoteException {
        Log.i(TAG, "installApp() apkPath=[" + apkPath + "], installerPackageName=[" + installerPackageName + "]");
        Log.i(TAG, "isRoot()=[" + isRoot() + "]");

        String invokerPkg = VfiServiceApp.getContext().getPackageManager().getNameForUid(Binder.getCallingUid());
        Log.i(TAG, "invokerPkg [" + invokerPkg + "]");
        isSameAPP = installerPackageName.equals(invokerPkg);
        Log.i(TAG, "isSameAPP [" + isSameAPP + "]");

        installerPkgName = installerPackageName;
        if (apkPath == null || apkPath.trim().isEmpty()) {
            Log.i(TAG, "Invalid apkPath!");
            return;
        }

        if (!new File(apkPath).exists()) {
            Log.i(TAG, "apkPath not exist!");
            return;
        }

        String vfServiceVer = getVersionName(VfiServiceApp.getContext(), VFSERVICE_PKG);
        if (!TextUtils.isEmpty(vfServiceVer)) {
            installedVFServiceVer = vfServiceVer;
        }

        synchronized (this) {
            installObserver = observer;
            if (VfiServiceApp.getInstallStatus() != true) {
                Log.e(TAG, "SystemService installing other apk, please wait until finished");
                cbInstallObserver(null, -1);
                return;
            }

            VfiServiceApp.setInstallStatus(false);
        }

//        if (Build.VERSION.SDK_INT < 28) {//
        //launch the desired activity
//            Intent installIntent = new Intent(VfiServiceApp.getContext(), AppOperateActivity.class);
//            installIntent.putExtra("APPFILEPATH", apkPath);
//            installIntent.putExtra("INSTALLERNAME", installerPackageName);
//            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
//            installIntent.setAction("com.vfi.smartpos.InstallActivity");
//            VfiServiceApp.getContext().startActivity(installIntent);
        if (isSameAPP) {
            SoSDKManager soSDKManager = (SoSDKManager) VfiServiceApp.getContext().getSystemService("SoSDKService");
            homeState = soSDKManager.getKeyHomeState();
            //屏蔽Home键
            isMaskHomeKey(true);
            Intent installIntent = new Intent(VfiServiceApp.getContext(), AppOperateActivity.class);
            /** 使用Application的context启动activity需要添加 new task 的flag**/
            installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            VfiServiceApp.getContext().startActivity(installIntent);
        }
        AppOperateUtils.silentInstallAPK(VfiServiceApp.getContext(), apkPath, installerPkgName);
//        } else {
//            PackageManager pm = VfiServiceApp.getContext().getPackageManager();
//            PackageManagerCompatP packageManagerCompatP = new PackageManagerCompatP();
//            packageManagerCompatP.install28(VfiServiceApp.getContext(), apkPath, pm);
//        }
        return;
    }

    @Override
    public void uninstallApp(String packageName, IAppDeleteObserver observer) throws RemoteException {
        Log.i(TAG, "uninstallApp() packageName=[" + packageName + "]");

        synchronized (this) {
            deleteObserver = observer;

            if (BuildConfig.APPLICATION_ID.equals(packageName)) {
                Log.e(TAG, "Can not uninstall SystemService");
                cbDeleteObserver(null, -1);
                return;
            }

            if (VfiServiceApp.getDeleteStatus() != true) {
                Log.e(TAG, "SystemService deleting other apk, please wait until finished");
                cbDeleteObserver(null, -1);
            }

            VfiServiceApp.setDeleteStatus(false);
        }

        //launch the desired activity
//        Intent uninstallIntent = new Intent(VfiServiceApp.getContext(), AppOperateActivity.class);
//        uninstallIntent.putExtra("PACKAGENAME", packageName);
//        uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
//        uninstallIntent.setAction("com.vfi.smartpos.DeleteActivity");
//        VfiServiceApp.getContext().startActivity(uninstallIntent);
        AppOperateUtils.silentDeletePKG(VfiServiceApp.getContext(), packageName);
    }

/*    @Override
    public String getStoragePath(int type) {
        String storagePath;
        Log.i(TAG, "getStoragePath() type=[" + type + "]");
        Log.i(TAG, "SdPath.getRealInternalSDpath()=" + SdPath.getRealInternalSDpath());
        storagePath = Environment.getExternalStorageDirectory().getPath();
        return storagePath;
    }*/

    @Override
    public void reboot() {
        Log.i(TAG, "reboot()");

        PowerManager pManager = (PowerManager) VfiServiceApp.getInstance().getSystemService(Context.POWER_SERVICE);
        pManager.reboot("");
        return;
    }

    @SuppressLint("WrongConstant")
    @Override
    public void isMaskHomeKey(boolean state) throws RemoteException {
        SoSDKManager soSDKManager;
        soSDKManager = (SoSDKManager) VfiServiceApp.getContext().getSystemService("SoSDKService");
        if (state) {
            Log.i(TAG, "set home key can not be used");
            soSDKManager.setKeyHomeState(true);
        } else {
            Log.i(TAG, "set home key can be used");
            soSDKManager.setKeyHomeState(false);
        }

    }

    @SuppressLint("WrongConstant")
    @Override
    public void isMaskStatusBard(boolean state) throws RemoteException {
        SoSDKManager soSDKManager;
        soSDKManager = (SoSDKManager) VfiServiceApp.getContext().getSystemService("SoSDKService");
        if (state) {
            Log.i(TAG, "set status bar can not be used");
            soSDKManager.setStatusbarState(true);
        } else {
            Log.i(TAG, "set status bar can be used");
            soSDKManager.setStatusbarState(false);
        }
    }

    @Override
    public boolean chekcK21Update(String sysBin, String appBin) throws RemoteException {
        File sysBinFile = new File(sysBin);
        if (sysBinFile.exists() && sysBinFile.getName().endsWith("vfuup")) {
            return UpdateSecurityDriver(sysBin);
        }
        File appBinFile = new File(appBin);
        if (appBinFile.exists() && sysBinFile.getName().endsWith("vfuup")) {
            return UpdateSecurityDriver(appBin);
        }

        if (sysBinFile.exists() && appBinFile.exists()) {
            //
        } else {
            Log.e(TAG, "File not exist: " + sysBin + ", or " + appBin);
            return false;
        }

        String k21AppVersion = "";
        String k21SysVersion = "";
        // 和福州永福讨论k21文件命名格式为x.x.x（例如2.9.9，而且它的下一个版本是3.0.0而不是2.9.10）
        String k21SysFileVersion = sysBin.substring(sysBin.lastIndexOf("V") + 1, sysBin.lastIndexOf("_")).replace(".", "");
        String k21AppFileVersion = appBin.substring(appBin.lastIndexOf("V") + 1, appBin.lastIndexOf("_")).replace(".", "");

        boolean isUpdateApp = false;
        boolean isUpdateSys = false;

        String k21UpdateStatus = "";
        try {
            FileReader fr = new FileReader("/sdcard/k21updatestatus.txt");
            if (fr != null) {
                char[] status = new char[126];
                int len = fr.read(status);
                fr.close();
                if (len > 0) {
                    if (len > 126) {
                        len = 126;
                    }
                    k21UpdateStatus = new String(status).substring(0, len);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "last k21UpdateStatus=" + k21UpdateStatus);

        if (TextUtils.isEmpty(k21UpdateStatus) || "ok".equals(k21UpdateStatus)) {
            try {
                k21AppVersion = SdkApiHolder.getInstance().getTerminalManager().getDeviceInfo().getAppVersion();
            } catch (SDKException e) {
                e.printStackTrace();
            }

            try {
                k21SysVersion = SdkApiHolder.getInstance().getTerminalManager().getDeviceInfo().getFirmwareVersion();
            } catch (SDKException e) {
                e.printStackTrace();
            }

            try {
                //k21AppVersion=X990-V1.4.9
                //k21SysVersion=X990F_V0.17 (May 27 2017 20:27:43)
                Log.i(TAG, "k21AppVersion=" + k21AppVersion + ", k21SysVersion=" + k21SysVersion);
                if (k21AppVersion.length() >= 11) {
//                    String v1[] = (k21AppVersion.substring(k21AppVersion.indexOf("V") + 1)).split("\\.");
//                    String appVersion = new StringBuilder().append(v1[0]).append(".").append(v1[1]).append(v1[2]).toString();
                    String appVersion = k21AppVersion.substring(k21AppVersion.indexOf("V") + 1, k21AppVersion.indexOf("(")).trim().replace(".", "");
                    String appFileVersion = k21AppFileVersion;
                    Log.i(TAG, "k21 app version=" + appVersion + ", k21 app file version=" + appFileVersion);
                    if (appFileVersion.compareToIgnoreCase(appVersion) > 0) {
                        isUpdateApp = true;
                    }
                }
                if (k21SysVersion.length() >= 11) {
                    String v1 = k21SysVersion.substring(k21SysVersion.indexOf("V") + 1, k21SysVersion.indexOf("(")).trim().replace(".", "");
                    String v2 = k21SysFileVersion;
                    Log.i(TAG, "k21 sys version=" + v1 + ", k21 sys file version=" + v2);
                    if (v2.compareToIgnoreCase(v1) > 0) {
                        isUpdateSys = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            //强制更新
            if ("app fail".equals(k21UpdateStatus)) {
                isUpdateApp = true;
            } else if ("sys fail".equals(k21UpdateStatus)) {
                isUpdateSys = true;
            } else {
                isUpdateApp = true;
                isUpdateSys = true;
            }
        }
        Log.i(TAG, "update k21,isUpdateApp=" + isUpdateApp + ",isUpdateSys=" + isUpdateSys);

//        isUpdateApp = true; isUpdateSys = true;
        if (isUpdateApp || isUpdateSys) {

            Bundle bdl = new Bundle();
            bdl.putBoolean("isUpdateApp", isUpdateApp);
            bdl.putBoolean("isUpdateSys", isUpdateSys);
            bdl.putString("sysBin", sysBin);
            bdl.putString("appBin", appBin);
            return doUpdateSecurityDriver(bdl);

        }
        return false;
    }

    @Override
    public boolean UpdateSecurityDriver(String updatePackagePath) throws RemoteException {
        if (TextUtils.isEmpty(updatePackagePath))
            return false;

        Bundle bdl = new Bundle();
        bdl.putString("updatePackagePath", updatePackagePath);
        return doUpdateSecurityDriver(bdl);
    }

    @Override
    public boolean isAppForeground(String packageName) throws RemoteException {
        ActivityManager activityManager = (ActivityManager) VfiServiceApp.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = null;
        if (activityManager != null) {
            appProcesses = activityManager.getRunningAppProcesses();
        }
        if (appProcesses == null || appProcesses.isEmpty()) {
            return false;
        }
        boolean isForeground = false;
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(packageName)) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                        || appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    isForeground = true;
                } else {
                    isForeground = false;
                }
            }
        }
        return isForeground;
    }

    /**
     * update base band, will reboot device automatically after call this API
     *
     * @param filePath
     * @return false-when copy base band file failed, when success will reboot device.
     * @throws RemoteException
     */
    @Override
    public boolean updateBaseBand(final String filePath) throws RemoteException {
        Log.d(TAG, "updateBaseBand:" + filePath);
        if (!isMgBaseband()) {
            VfiServiceApp.showToast("only support MG baseband, error!");
            return false;
        }
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            VfiServiceApp.showToast("load file...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ISystemSettings systemSettings = SdkApiHolder.getInstance().getSystemSettings();
                    systemSettings.updateRom(filePath);
                }
            }).start();
            return true;
        }
        Log.e(TAG, "file cannot find!!");
        return false;
    }

    private boolean isMgBaseband() {
        return getBasebandVersion().contains(BASE_BAND_KEYWORD);
    }

    private String getBasebandVersion() {
        try {
            Class<?> systemPropertiesClazz = Class.forName("android.os.SystemProperties");
            Method getMethod = systemPropertiesClazz.getMethod("get", String.class, String.class);
            Object invoke = getMethod.invoke(null, "gsm.version.baseband", "");
            return String.valueOf(invoke);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean doUpdateSecurityDriver(Bundle bundle) {
        EventBus.getDefault().register(this);

        Intent intent = new Intent(VfiServiceApp.getContext(), UpdateK21Activity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        intent.putExtras(bundle);
        VfiServiceApp.getContext().startActivity(intent);

        Log.d(TAG, "wait install ....");
        synchronized (this) {
            try {
                wait();
                Log.d(TAG, "install finish....");
                EventBus.getDefault().unregister(this);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return k21Installres;

    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessageEvent(MessageEvent event) {
        Log.d(TAG, "Install Finished");
        HashMap<String, Boolean> installRes = event.getMessage();
        Set<Map.Entry<String, Boolean>> entrys = installRes.entrySet();

        for (Map.Entry<String, Boolean> entry : entrys) {
            Log.d(TAG, entry.getKey() + "--" + entry.getValue());
            if (!entry.getValue()) {
                k21Installres = false;
                break;
            } else
                k21Installres = true;
        }

        synchronized (this) {
            this.notify();
        }
    }

    /*
    @Override
    public void updateSystem(String filePath, int type) {
        Log.i(TAG, "updateSystem() filePath=[" + filePath + "], type=[" + type + "]");
        new SystemUtils().updateSystem(VfiServiceApp.getContext(), SdPath.getRealInternalSDpath() + "/X9_1229_ota_full.zip", new SystemUtils.UpdateSysteListener() {
            @Override
            public void onError(int errorCode, String errorMessage) {
                Log.i(TAG, "updateSystem errorCode=[" + errorCode + "]," + errorMessage);
            }

            @Override
            public void onSuccess() {
                Log.i(TAG, "updateSystem Success");
            }
        });
        return;
    }


    @Override
    public void backupByLevel(int level, IBackupObserver observer) throws RemoteException {
        int returnVal;
        List<String> tmpPackagename = new ArrayList<String>();
        String callingPackageName;
        String backupZipFileName;
        String pkgNameList = "";

        Log.i(TAG, "backupByLevel(), level=" + level);

        //获取调用方的Packagename(需要备份调用方的数据, 因此自己的Packagename没有意义)
        callingPackageName = VfiServiceApp.getContext().getPackageManager().getNameForUid(Binder.getCallingUid());
        Log.i(TAG, "callingPackageName=" + callingPackageName);
        tmpPackagename.add(callingPackageName);

        backupZipFileName = "/Backup" + GetDatetimeString() + ".zip";
        Log.i(TAG, "backupPkgFileName=" + backupZipFileName);

        Log.i(TAG, "tmpPackagename.size()=" + tmpPackagename.size());
        for (int i = 0; i < tmpPackagename.size(); i++) {
            pkgNameList += (String) tmpPackagename.get(i);
            pkgNameList += ",";
        }
        Log.i(TAG, "backup[" + pkgNameList + "]");

        if (tmpPackagename.isEmpty()) {
            Log.i(TAG, "tmpPackagename empty!");
            return;
        }


//        if(path == null || (path.length() <= 0)) {
//            Log.i(TAG, "null or invalid path!");
//            return -3;
//        }
        Log.i(TAG, "The final backup full path name:[" + Environment.getExternalStorageDirectory().getPath() + backupZipFileName + "]");
        returnVal = mtdaSys.backup(tmpPackagename, Environment.getExternalStorageDirectory().getPath() + backupZipFileName);

        Log.i(TAG, "mtdaSys.backup returnVal=" + returnVal);

        if (returnVal == 0)
            observer.onBackupFinished(0, backupZipFileName);
        else
            observer.onBackupFinished(-1, backupZipFileName);

        return;
    }

    @Override
    public void restore(String filePath, IRestoreObserver observer) throws RemoteException {
        int RetValue;

        //增加对路径合法性的差错处理? @2016-11-9
        Log.i(TAG, "restore() from path: [" + filePath + "]");
        RetValue = mtdaSys.restore(filePath);
        Log.i(TAG, "mtdaSys.restore() = [" + RetValue + "]");

        if (RetValue == 0)
            observer.onRestoreFinished(0);
        else
            observer.onRestoreFinished(-1);
        return;
    }

    @Override
    public void getSystemMangeLogs(String filePath) {
        Log.i(TAG, "getSystemMangeLogs() filePath=[" + filePath + "]");

        return;
    }*/


    public void cbInstallObserver(String packageName, int returnCode) throws RemoteException {
        Log.i(TAG, "cbInstallObserver() packageName=[" + packageName + "], returnCode=[" + returnCode + "]" + ",installerPkgName=[" + installerPkgName + "]");
        if (isSameAPP) {
            isMaskHomeKey(homeState);
            if (AppOperateActivity.instance != null) {
                AppOperateActivity.instance.finish();
            }
        }
        if (installObserver != null) {
            if (returnCode == 1) {
                doExtraOperation(packageName);
                Log.i(TAG, "==========star android.intent.action.MAIN=======");
                if (installerPkgName != null && installerPkgName.equals(packageName)) { // 安装自己需要重启自己
                    Log.i(TAG, "installerPkgName=" + installerPkgName + ",be installed PkgName=" + packageName);
                    VfiServiceApp.setInstallStatus(true);
                    doStartApplicationWithPackageName(packageName);
                }
                VfiServiceApp.setInstallStatus(true);
                installObserver.onInstallFinished(packageName, 0);
            } else if (returnCode == 0) {
                //returnCode == 0不返回任何信息
            } else {
                VfiServiceApp.setInstallStatus(true);
                installObserver.onInstallFinished(packageName, returnCode);
            }
        }
    }

    public void cbDeleteObserver(String packageName, int returnCode) throws RemoteException {
        Log.i(TAG, "cbDeleteObserver() packageName=[" + packageName + "], returnCode=[" + returnCode + "]");
        if (deleteObserver != null) {
            if (returnCode == 1) {
                deleteObserver.onDeleteFinished(packageName, 0);
            } else {
                deleteObserver.onDeleteFinished(packageName, returnCode);
            }
        }
        VfiServiceApp.setDeleteStatus(true);
    }

    //Private members and methods
    private static SystemManager instance = null;
    private static String TAG = "SystemManager";
    private static IAppInstallObserver installObserver;
    private static IAppDeleteObserver deleteObserver;

    private SystemManager() {
        mTerminalMessage = SdkApiHolder.getInstance().getTerminalMessage();
        iKeyManager = SdkApiHolder.getInstance().getKeyManager();
    }

    /**
     * 判断手机是否拥有Root权限。
     *
     * @return 有root权限返回true，否则返回false。
     */
    private boolean isRoot() {
        boolean bool = false;
        try {
            bool = new File("/system/bin/su").exists() || new File("/system/xbin/su").exists();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bool;
    }

    @SuppressLint("SimpleDateFormat")
    private String GetDatetimeString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        String str = formatter.format(curDate);
        Log.i(TAG, "date time str = " + str);

        return str;
    }

    private void doStartApplicationWithPackageName(String packagename) {

        // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
        PackageInfo packageinfo = null;
        try {
            packageinfo = VfiServiceApp.getContext().getPackageManager().getPackageInfo(packagename, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageinfo == null) {
            return;
        }

        // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        resolveIntent.setPackage(packageinfo.packageName);

        // 通过getPackageManager()的queryIntentActivities方法遍历
        List<ResolveInfo> resolveinfoList = VfiServiceApp.getContext().getPackageManager()
                .queryIntentActivities(resolveIntent, 0);

        if (resolveinfoList.iterator().hasNext()) {
            ResolveInfo resolveinfo = resolveinfoList.iterator().next();
            if (resolveinfo != null) {
                // packagename = 参数packname
                String packageName = resolveinfo.activityInfo.packageName;
                // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packagename.mainActivityname]
                String className = resolveinfo.activityInfo.name;
                // LAUNCHER Intent
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // 设置ComponentName参数1:packagename参数2:MainActivity路径
                ComponentName cn = new ComponentName(packageName, className);

                intent.setComponent(cn);
                VfiServiceApp.getContext().startActivity(intent);

            }
        }
    }

    private void copyRomUpgradeFile2Cache(final String zipPath) {

        File desFile = new File(zipPath);

        if (desFile.exists()) {
            Log.d("TAG", "Copy_File " + desFile.getName() + " is exist.");
        } else {
            Log.d("TAG", "Copy_File " + desFile.getName() + " is not exist.");

        }

        try {
            //Copy update file to internal storage
            FileInputStream input = new FileInputStream(desFile);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("/cache/update.zip"));
            byte[] bytes = new byte[1024];
            int n;
            while ((n = input.read(bytes)) != -1) {
                bos.write(bytes, 0, n);
            }
            input.close();
            bos.close();


            Log.d("TAG", "copy RomUpgradeFile to /cache successful");

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("TAG", "copy RomUpgradeFile to /cache failed");

        }
    }

    private void deleteFile(String filePath) {
        String fileName = filePath.substring(filePath.lastIndexOf("/"));

        File file = new File(filePath);

        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                Log.d("TAG", "delete" + fileName + "successful");
            } else {
                Log.d("TAG", "delete" + fileName + "failed");
            }
        } else {
            Log.d("TAG", "delete failed: " + fileName + "does not exist");
        }
    }

    //updateROM
    public void updateROM(String zipPath) {

        File desFile = new File(zipPath);

        if (desFile.exists()) {
            Log.d("TAG", "File " + desFile.getName() + "Rom package exists.");
        } else {
            Log.d("TAG", "File " + desFile.getName() + "Rom package doesn't exist.");
        }

        /*
         *verify upgrade package
         * 验证包，可能时间会比较久，不能放在主线程用
         * param：desFile: 升级包
         *        progressListner ：显示进度的progressbar
         *        deviceCertsZipFile: 如果程序包由与此文件中的任何公钥对应的私钥签名，则验证成功，如果为null，则用系统默认的签名文件，/system/etc/security/otacerts.zip
         *
         *
         * */
        //verify Upgrade package TODO
//        try {
//            RecoverySystem.verifyPackage(desFile, progressListner, deviceCertsZipFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.d("TAG", "updateROM: verify failed");
//        } catch (GeneralSecurityException e) {
//            e.printStackTrace();
//            Log.d("TAG", "updateROM: verify failed");
//        }

        //update ROM
        try {
            RecoverySystem.installPackage(VfiServiceApp.getContext(), desFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public INetworkManager getNetworkManager() throws RemoteException {
        return NetworkManager.getInstance();
    }

    @Override
    public void setLocationMode(int status) throws RemoteException {

        Log.d(TAG, "Start to set location mode... ");

        switch (status) {

            case 0:
                // Turn off location
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Settings.Secure.putInt(
                            VfiServiceApp.getContext().getContentResolver(),
                            Settings.Secure.LOCATION_MODE,
                            0);
                } else {
                    Settings.Secure.putString(
                            VfiServiceApp.getContext().getContentResolver(),
                            Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
                            "");
                    Log.d(TAG, "The location mode turns to : off");
                }

                break;
            case 1:
                // Location mode : only sensors
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Settings.Secure.putInt(
                            VfiServiceApp.getContext().getContentResolver(),
                            Settings.Secure.LOCATION_MODE,
                            1);
                } else {
                    Settings.Secure.putString(
                            VfiServiceApp.getContext().getContentResolver(),
                            Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
                            "gps");

                }
                Log.d(TAG, "The location mode turns to : sensors only");
                break;
            case 2:
                // Location mode: battery saving ()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Settings.Secure.putInt(
                            VfiServiceApp.getContext().getContentResolver(),
                            Settings.Secure.LOCATION_MODE,
                            2);
                } else {
                    Settings.Secure.putString(
                            VfiServiceApp.getContext().getContentResolver(),
                            Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
                            "network");
                }

                Log.d(TAG, "The location mode turns to : battery saving");
                break;
            case 3:
                // Turn off location
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Settings.Secure.putInt(
                            VfiServiceApp.getContext().getContentResolver(),
                            Settings.Secure.LOCATION_MODE,
                            3);
                } else {
                    Settings.Secure.putString(
                            VfiServiceApp.getContext().getContentResolver(),
                            Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
                            "gps,network");
                }

                Log.d(TAG, "The location mode turns to : high accuracy");
                break;

        }

    }

    @Override
    public boolean isAdbMode() throws RemoteException {
        boolean isAdbMode = false;
        String str = SysProp.get("sys.usb.state", "");
        Log.i(TAG, "get android.os.SystemProperties(sys.usb.state)=" + str);
        if (str.contains("adb")) {
            isAdbMode = true;
        }
        Log.i(TAG, "adb mode=" + isAdbMode);
        return isAdbMode;
    }

    @Override
    public boolean killApplication(String packageName) throws RemoteException {
        if (packageName != null && packageName.length() != 0) {
            Context context = VfiServiceApp.getContext();
            ActivityManager manager = (ActivityManager)
                    context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                try {
                    @SuppressLint("PrivateApi")
                    Method forceStopPackage = manager.getClass().
                            getDeclaredMethod("forceStopPackage", String.class);
                    forceStopPackage.setAccessible(true);
                    forceStopPackage.invoke(manager, packageName);
                    return true;
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public boolean restartApplication(String packageName) throws RemoteException {
        if (packageName != null && packageName.length() != 0) {
            Context context = VfiServiceApp.getContext();
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(intent);
                return true;
            }
        }
        return false;
    }

    @Override
    public void initLogcat(int logcatBufferSize, int logcatBufferSizeSuffix, Bundle logcatParam)
            throws RemoteException {
        String resetLogRingBufferSize;
        if (logcatBufferSize > 0) {
            if (logcatBufferSizeSuffix == 0) {
                resetLogRingBufferSize = "logcat -G " + logcatBufferSize + "M";
            } else {
                resetLogRingBufferSize = "logcat -G " + logcatBufferSize + "K";
            }
        } else {
            resetLogRingBufferSize = "logcat -G " + defaultLogcatBufferSize;
        }
        execCommand(resetLogRingBufferSize);
    }

    @Override
    public String getLogcat(String logcatFileName, int compressType) throws RemoteException {
        LogFileRecordUtil logFileRecordUtil = LogFileRecordUtil.getInstance(defaultLogcatRecordFilePath);
        deleteExpiredDefaultLogFile(logFileRecordUtil);
        String path = getLogFilePath(logcatFileName);
        Log.d(TAG, "get logcat path is " + path);
        File file = createNewFile(path);
        if (file != null && file.exists()) {
            if (recordLog(path)) {
                return compressFile(path, compressType, logFileRecordUtil);
            } else {
                Log.d(TAG, path + " log file not exist");
                return null;
            }
        } else {
            return null;
        }
    }

    private void deleteExpiredDefaultLogFile(LogFileRecordUtil logUtil) {
        File defaultLogDirectory = new File(defaultLogcatFilePath);
        for (File file : defaultLogDirectory.listFiles()) {
            String name = file.getName();
            Log.d(TAG, "file name : " + name);
            String createTime = logUtil.getProperty(name, null);
            Log.d(TAG, "file value : " + createTime);
            if (createTime != null && createTime.length() != 0) {
                int days = (int) (System.currentTimeMillis() - Long.valueOf(createTime)) / (1000 * 3600 * 24);
                if (days >= 7) {
                    Log.d(TAG, "delete file : " + name);
                    logUtil.remove(name);
                    file.delete();
                }
            }
        }
    }

    /**
     * @param path
     * @return if the path starts with "/" and ends with "/", the path is directory.
     * if the path starts with "/" and doesn't end with "/", the path is file which has fully path.
     * if the path doesn't start with "/", the path is a package name.
     * if the length of path is 0, use default path and name.
     */
    private String getLogFilePath(String path) {
        Log.d(TAG, "original path : " + path);
        if (path != null && path.length() != 0) {
            if (path.startsWith("/")) {
                if (path.endsWith("/")) {
                    path = path + generateLogFileName();
                    Log.d(TAG, "log file path : " + path);
                    return path;
                } else {
                    Log.d(TAG, "log file path : " + path);
                    return path;
                }
            } else {
                path = defaultLogcatFilePath + path;
                Log.d(TAG, "log file path : " + path);
                return path;
            }
        } else {
            path = defaultLogcatFilePath + generateLogFileName();
            Log.d(TAG, "fully log path: " + path);
            return path;
        }
    }

    private String generateLogFileName() {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String logcatFileName = "paxstore_log_" + sdf.format(new Date()) + ".log";
        Log.d(TAG, "logcatFileName: " + logcatFileName);
        return logcatFileName;
    }

    private File createNewFile(String path) {
        String directoryPath = path.substring(0, path.lastIndexOf("/"));
        Log.d(TAG, "directory path is " + directoryPath);
        File directoryFile = new File(directoryPath);
        if (!directoryFile.exists()) {
            directoryFile.mkdirs();
        }
        Log.d(TAG, "path: " + path);
        File file = new File(directoryPath, path.substring(path.lastIndexOf("/") + 1));
        if (!file.exists()) {
            try {
                file.createNewFile();
                return file;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return file;
        }
    }

    //record log file
    private boolean recordLog(String path) {
        String cmd_record_log = "logcat -v threadtime -f " + path + " -d";
        execCommand(cmd_record_log);
        return waitLogWriting(path);
    }

    //if log file no exist, wait 1 seconds. after waiting, if the log file exist, waiting the os writing the log file.
    //if the log file writing finished or writing over 2 minutes, executing finished.
    private boolean waitLogWriting(String path) {
        File logFile = new File(path);
        if (!logFile.exists()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!logFile.exists()) {
            Log.d(TAG, path + " log file not exist");
            return false;
        }
        long lastModified;
        do {
            lastModified = logFile.lastModified();
            Log.d(TAG, path + " last modified time " + lastModified);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (logFile.lastModified() > lastModified
                && logFile.lastModified() - lastModified < 2 * 60 * 1000);
        return true;
    }

    private String compressFile(String path, int compressType, LogFileRecordUtil logUtil) {
        if (compressType == 0) {
            if (path.contains(defaultLogcatFilePath)) {
                logUtil.setProperty(path.substring(path.lastIndexOf("/") + 1), String.valueOf(System.currentTimeMillis()));
            }
            Log.d(TAG, "fully log file path: " + path);
            return path;
        } else {
            Log.d(TAG, "fully log file path before compressing: " + path);
            String cmd_gz_log_file = "gzip " + path;
            execCommand(cmd_gz_log_file);
            path = path + ".gz";
            if (path.contains(defaultLogcatFilePath)) {
                String name = path.substring(path.lastIndexOf("/") + 1);
                String time = String.valueOf(System.currentTimeMillis());
                logUtil.setProperty(name, time);
                Log.d(TAG, "set log properties : " + "key : " + name + " value : " + time);
            }
            Log.d(TAG, "fully log file path after compressing: " + path);
            return path;
        }
    }

    private void execCommand(String cmd) {
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public Bundle getLaunchAppsInfo(long beginTime, long endTime) {
        Bundle bundle = new Bundle();
        UsageStatsManager usageStatsManager = (UsageStatsManager)
                VfiServiceApp.getContext().getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager != null) {
            List<UsageStats> usageStatsList = new ArrayList<>();
            Map<String, UsageStats> usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(
                    beginTime, endTime);
            for (Map.Entry<String, UsageStats> map : usageStatsMap.entrySet()) {
                Log.d(TAG, map.getKey() + ": " + map.getValue());
                usageStatsList.add(map.getValue());
            }
            Gson gson = new Gson();
            String statesList = gson.toJson(usageStatsList);
//            String statesList = JSON.toJSONString(usageStatsList);
            Log.d(TAG, "UsageStatsList : " + statesList);
            bundle.putString("UsageStatsList", statesList);
            return bundle;
        } else {
            Log.w(TAG, "get UsageStatsList is null");
            return bundle;
        }
    }

    @Override
    public ISettingsManager getSettingsManager() throws RemoteException {
        return SettingsManager.getInstance();
    }

    @Override
    public Bitmap takeCapture() throws RemoteException {
        Resources resources = VfiServiceApp.getContext().getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();

        String surfaceClassName = "";
        if (Build.VERSION.SDK_INT <= 17) {
            surfaceClassName = "android.view.Surface";
        } else {
            surfaceClassName = "android.view.SurfaceControl";
        }

        Method method;
        Bitmap bitmap = null;

        try {
            Class<?> c = Class.forName(surfaceClassName);
            if (Build.VERSION.SDK_INT >= 28) {
                method = c.getMethod("screenshot", new Class[]{Rect.class, int.class, int.class, int.class});
                method.setAccessible(true);
                bitmap = (Bitmap) method.invoke(null, new Rect(), dm.widthPixels, dm.heightPixels, Surface.ROTATION_0);
            } else {
                method = c.getMethod("screenshot", new Class[]{int.class, int.class});
                method.setAccessible(true);
                bitmap = (Bitmap) method.invoke(null, dm.widthPixels, dm.heightPixels);
            }
        } catch (NoSuchMethodException
                 | IllegalAccessException
                 | InvocationTargetException
                 | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    @Override
    public void shutdownDevice() throws RemoteException {
        try {
            PowerManager pManager = (PowerManager) VfiServiceApp.getContext().getSystemService(Context.POWER_SERVICE);
            if (pManager != null) {
                Method method = pManager.getClass().getMethod("shutdown", boolean.class, String.class, boolean.class);
                method.invoke(pManager, false, null, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getInstallerPkgName() {
        return installerPkgName;
    }

    private void doExtraOperation(String packageName) {
        Log.d(TAG, "doExteraOperation, package=" + packageName);
        if (packageName.equalsIgnoreCase(VFSERVICE_PKG)) {
            /*
              密钥搬移的条件，ROM版本大于V1.1.1.202006081848 INTLv7， K21版本要大于等于200， 兆讯默认直接搬移
             */
            if (getCommandServerVersion() > 543 &&
                    (!TextUtils.isEmpty(getSPVersion()) || Integer.parseInt(getK21AppVersion()) >= 200) &&
                    isVfserviceVer2to3() && isCorrectSponsor()) {
                Log.i(TAG, "execute key move!!");
                //密钥迁移
                try {
                    iKeyManager.decreaseKeysIndex();
                } catch (SDKException | KeyException e) {
                    e.printStackTrace();
                }
            } else {
                Log.i(TAG, "no need to move key!!");
            }
        }
    }

    /**
     * 返回packageName对应的versionName
     *
     * @param context
     * @param packageName
     * @return
     */
    private String getVersionName(Context context, String packageName) {
        String versionName = "";
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packInfo = packageManager.getPackageInfo(packageName, 0);
            versionName = packInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionName;
    }

    private int getCommandServerVersion() {
        int commandVer = AppUtils.getVersionCode(VfiServiceApp.getContext(), "com.socsi.smartposcommanddriver");
        Log.d(TAG, "command server ver=" + commandVer);
        return commandVer;
    }

    private String getSPVersion() {
        Log.d(TAG, "getSPVersion execute");
        String vfuupVer = mTerminalMessage.getVfuupVersion();
        if (!TextUtils.isEmpty(vfuupVer))
            return vfuupVer;
        else
            return "";
    }

    private String getK21AppVersion() {
        try {
            DeviceInfo deviceInfo = SdkApiHolder.getInstance().getTerminalManager().getDeviceInfo();
            String patternStr = "[0-9]{1,}[A-Z]{0,1}\\.[0-9]{1,}\\.{0,1}[0-9]{0,}\\.{0,1}[A-Z]{0,}";
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcherApp = pattern.matcher(deviceInfo.getAppVersion());
            if (matcherApp.find()) {
                String appVer = matcherApp.group().replace(".", "");
                Log.i(TAG, "app:" + appVer);
                return appVer;
            }
        } catch (SDKException e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean isVfserviceVer2to3() { // 判断service是否是从2.0升级到3.0
        String newVersion = getVersionName(VfiServiceApp.getContext(), VFSERVICE_PKG);
        String patternStr = "[0-9]{1,}[A-Z]{0,1}\\.[0-9]{1,}\\.{0,1}[0-9]{0,}\\.{0,1}[0-9]{0,}";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcherOldVer = pattern.matcher(installedVFServiceVer);
        Matcher matcherNewVer = pattern.matcher(newVersion);
        if (matcherNewVer.find() && matcherOldVer.find()) {
            int oldVer = Integer.parseInt(matcherOldVer.group().split("\\.")[0]);
            int newVer = Integer.parseInt(matcherNewVer.group().split("\\.")[0]);
            Log.d(TAG, "VFService[old version] = " + oldVer);
            Log.d(TAG, "VFService[new version] = " + newVer);
            if (oldVer == 2 && newVer - oldVer >= 1) {
                return true;
            }
        }
        return false;
    }

    private boolean isCorrectSponsor() {
        Log.i(TAG, "isCorrectSponsor executed()");
        String sponsorHashValue = "";
        try {
            sponsorHashValue = StringUtil.byte2HexStr(iKeyManager.getSponserCertificateHashValue());
            if (sponsorHashValue.equalsIgnoreCase("B4F51E731957C367E6ED345D5E968639262441597F6CDF0F2DC723419ABCD068")
                    || sponsorHashValue.equalsIgnoreCase("2EA763865C3644F1335F7664AF9669F4C076F9D1D32B0AB3AA0C304DF1EA68AF")) {
                Log.i(TAG, "isCorrectSponsor = true");
                return true;
            }
        } catch (SDKException | KeyException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get screen brightness
     *
     * @return brightness
     * @throws RemoteException
     */
    public int getScreenBrightness() throws RemoteException {
        Log.i(TAG, "getBrightness executed()");
        int brightness = BrightnessUtil.getMyBrightness(VfiServiceApp.getContext());
        Log.i(TAG, "The brightness of the screen is" + brightness);
        return brightness;
    }

    /**
     * Input data to adjust screen brightness:
     * the data is greater than or equal to 10 and less than or equal to 255(10<=data<=255)
     *
     * @param brightnessData
     * @throws RemoteException
     */
    public void changeScreenBrightness(int brightnessData) throws RemoteException {
        Log.i(TAG, "changeBrightness executed()");
        Log.d(TAG, "brightnessData is:" + brightnessData);
        try {
            if (brightnessData >= 10 && brightnessData <= 255) {
                BrightnessUtil.ModifySettingsScreenBrightness(VfiServiceApp.getContext(), brightnessData);
                Log.d(TAG, "changeBrightness() succeed,the brightness is: " + brightnessData);
            } else {
                Log.d(TAG, "ERROR:The data you entered is out of range. Please enter data between 10 and 255");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean verifySignSync(String path, boolean enableDualVerify) throws RemoteException {
        try {
            Log.i(TAG, "verifySignSync executed()");
            return SdkApiHolder.getInstance().getTerminalManager().verifySignSync(path, enableDualVerify);
        } catch (SDKException e) {
            throw new RuntimeException(e);
        }
    }

    public void verifySign(String path, boolean enableDualVerify, final IVerifysignCallback callback) throws RemoteException {
        try {
            Log.i(TAG, "verifySign executed()");
            SdkApiHolder.getInstance().getTerminalManager().verifySign(path, enableDualVerify, new OnVerifySignCallback() {
                @Override
                public void onVerifySignResult(boolean b) {
                    try {
                        Log.d(TAG, "onVerifySignResult:" + "-->" + b);
                        if (callback != null)
                            callback.onVerifySignResult(b);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (SDKException e) {
            throw new RuntimeException(e);
        }
    }
}