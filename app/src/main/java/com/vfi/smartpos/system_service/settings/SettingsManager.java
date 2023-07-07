package com.vfi.smartpos.system_service.settings;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.app.backup.BackupManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.util.TimeZone;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.LocaleList;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.verifone.exception.SDKException;
import com.verifone.smartpos.api.SdkApiHolder;
import com.verifone.smartpos.api.system.ISystemSettings;
import com.verifone.smartpos.api.terminal.OnCustomerConfigurationUpdateListener;
import com.verifone.smartpos.utils.StringUtil;
import com.vfi.smartpos.system_service.VfiServiceApp;
import com.vfi.smartpos.system_service.aidl.settings.ICustomerConfigurationUpdateListener;
import com.vfi.smartpos.system_service.aidl.settings.ISettingsManager;
import com.vfi.smartpos.system_service.aidl.settings.SettingsActions;
import com.vfi.smartpos.system_service.aidl.settings.SettingsType;
import com.vfi.smartpos.system_service.reciever.TFReceiver;
import com.vfi.smartpos.system_service.util.MmkvUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SettingsManager extends ISettingsManager.Stub {
    private static final String TAG = SettingsManager.class.getSimpleName();

    private SettingsManager() {
    }

    public static SettingsManager getInstance() {
        return SettingsManagerHandler.instance;
    }

    private static class SettingsManagerHandler {
        private static final SettingsManager instance = new SettingsManager();
    }

    @Override
    public int settingsSetActions(int settingsType, Bundle bundle) throws RemoteException {
        Log.d(TAG, "settings type is " + settingsType);
        String action;
        switch (settingsType) {
            case SettingsType.DATE_TIME:
                action = bundle.getString("SYSTEM_TIME_ACTIONS", "");
                Log.d(TAG, "settings action is " + action);
                int state;
                switch (action) {
                    case SettingsActions.SystemTimeActions.SET_AUTO_SYSTEM_TIME_STATE:
                        state = bundle.getInt("AUTO_SYSTEM_TIME", 1);
                        setSystemTimeAuto(state > 0 ? 1 : 0);
                        return isSystemTimeAuto() == state ? 0 : -1;
                    case SettingsActions.SystemTimeActions.SET_AUTO_SYSTEM_TIME_ZONE_STATE:
                        state = bundle.getInt("AUTO_SYSTEM_TIME_ZONE", 1);
                        setSystemTimeZoneAuto(state > 0 ? 1 : 0);
                        return isSystemTimeZoneAuto() == state ? 0 : -1;
                    case SettingsActions.SystemTimeActions.SET_SYSTEM_TIME:
                        String yyyyMMdd = bundle.getString("SYSTEM_DATE", "");
                        String HHmmss = bundle.getString("SYSTEM_TIME", "");
                        return updateSystemTime(yyyyMMdd, HHmmss) ? 0 : -1;
                    default:
                        break;
                }
                break;
            case SettingsType.LAUNCHER:
                action = bundle.getString("LAUNCHER_ACTIONS", "");
                Log.d(TAG, "settings action is " + action);
                switch (action) {
                    case SettingsActions.LauncherActions.SET_LAUNCHER:
                        String packageName = bundle.getString("LAUNCHER_PACKAGE_NAME", "");
                        boolean isRun = bundle.getBoolean("RUN_PACKAGE", false);
                        return setLauncher(packageName, isRun) ? 0 : -1;
                    default:
                        break;
                }
                break;
            case SettingsType.POWER_OPTIMIZE:
                String packageName = bundle.getString("POWER_OPTIMIZE_PACKAGE", "");
                boolean isEnableOptimize = bundle.getBoolean("POWER_OPTIMIZE_ENABLE", true);
                return doBatteryOptimize(packageName, isEnableOptimize);
            case SettingsType.TF_ENABLE:
                boolean isEnableTF = bundle.getBoolean("TF_ENABLE", true);
                MmkvUtils.setMmkvTfEnable(isEnableTF);
                if (!isEnableTF) {
                    //if tf disable, do disable manually
                    if (!TFReceiver.isUsb(VfiServiceApp.getContext())) {
                        TFReceiver.unMount();
                    }
                }
                return 0;
            default:
                break;
        }
        return -1;
    }

    private int doBatteryOptimize(String packageName, boolean isEnableOptimize) {
        if (TextUtils.isEmpty(packageName)) {
            return -1;
        }
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                Method getServiceMethod = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", new Class[]{String.class});
                Object ServiceManager = getServiceMethod.invoke(null, new Object[]{"power_ex"});
                Class<?> cStub = Class.forName("android.os.sprdpower.IPowerManagerEx$Stub");
                Method asInterface = cStub.getMethod("asInterface", IBinder.class);
                Object iPowerManagerExObj = asInterface.invoke(null, ServiceManager);

                Method setAppPowerSaveConfigWithTypeMethod = iPowerManagerExObj.getClass().getDeclaredMethod("setAppPowerSaveConfigWithType", String.class, int.class, int.class);
                setAppPowerSaveConfigWithTypeMethod.setAccessible(true);
                setAppPowerSaveConfigWithTypeMethod.invoke(iPowerManagerExObj, packageName
                        , 0, isEnableOptimize ? 1 : 0);
                return 0;
            } else if (Build.VERSION.SDK_INT <= 22) {
                PowerManager powerManager = (PowerManager) VfiServiceApp.getContext().getSystemService(Context.POWER_SERVICE);
                try {
                    Method setPowerSaveMode = powerManager.getClass().getDeclaredMethod("setPowerSaveMode", boolean.class);
                    setPowerSaveMode.setAccessible(true);
                    setPowerSaveMode.invoke(powerManager, isEnableOptimize);
                } catch (Exception e) {
                    e.printStackTrace();
                    return -1;
                }
                return 0;
            } else {
                Log.d(TAG, "not support,sdk version is " + Build.VERSION.SDK_INT);
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public Bundle settingsReadActions(int settingsType, Bundle bundle) throws RemoteException {
        Log.d(TAG, "settings type is " + settingsType);
        Bundle resultBundle = new Bundle();

        switch (settingsType) {
            case SettingsType.DATE_TIME:
                String action = bundle.getString("SYSTEM_TIME_ACTIONS", "");
                Log.d(TAG, "settings action is " + action);
                switch (action) {
                    case SettingsActions.SystemTimeActions.GET_AUTO_SYSTEM_TIME_STATE:
                        resultBundle.putInt("AUTO_SYSTEM_TIME", isSystemTimeAuto());
                    case SettingsActions.SystemTimeActions.GET_AUTO_SYSTEM_TIME_ZONE_STATE:
                        resultBundle.putInt("AUTO_SYSTEM_TIME_ZONE", isSystemTimeZoneAuto());
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        return resultBundle;
    }

    private void setSystemTimeAuto(int state) {
        Log.d(TAG, "set system time auto state, is " + (state == 1 ? "sync" : "disable sync"));
        android.provider.Settings.Global.putInt(VfiServiceApp.getContext().getContentResolver(),
                android.provider.Settings.Global.AUTO_TIME, state);
    }

    private int isSystemTimeAuto() {
        int result;
        try {
            result = android.provider.Settings.Global.getInt(VfiServiceApp.getContext().getContentResolver(),
                    android.provider.Settings.Global.AUTO_TIME) > 0 ? 1 : 0;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            result = -1;
        }
        Log.d(TAG, "get system time auto state, is " + (result == 1 ? "sync" : "disable sync"));
        return result;
    }

    private void setSystemTimeZoneAuto(int state) {
        Log.d(TAG, "set system time zone auto state, is " + (state == 1 ? "sync" : "disable sync"));
        android.provider.Settings.Global.putInt(VfiServiceApp.getContext().getContentResolver(),
                android.provider.Settings.Global.AUTO_TIME_ZONE, state);
    }

    private int isSystemTimeZoneAuto() {
        int result;
        try {
            result = android.provider.Settings.Global.getInt(VfiServiceApp.getContext().getContentResolver(),
                    android.provider.Settings.Global.AUTO_TIME_ZONE) > 0 ? 1 : 0;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            result = -1;
        }
        Log.d(TAG, "get system time auto zone state, is " + (result == 1 ? "sync" : "disable sync"));
        return result;
    }

    private boolean updateSystemTime(String date, String time) {
        Log.i(TAG, "updateSystemTime() executed, date=" + date + ", time=" + time);

        if (date == null || (date.length() != 8 && date.length() != 10)) {
            Log.i(TAG, "date format is invalid");
            return false;
        }
        if (time == null || (time.length() != 6 && time.length() != 8)) {
            Log.i(TAG, "time format is invalid");
            return false;
        }

        boolean ret = false;
        int year, month, day, hour, minute, second;

        try {
            if (date.indexOf("-") > 0 || date.indexOf("/") > 0) {
                year = Integer.parseInt(date.substring(0, 4));
                month = Integer.parseInt(date.substring(5, 7)) - 1;
                day = Integer.parseInt(date.substring(8, 10));
            } else {
                year = Integer.parseInt(date.substring(0, 4));
                month = Integer.parseInt(date.substring(4, 6)) - 1;
                day = Integer.parseInt(date.substring(6, 8));
            }

            if (time.indexOf(":") > 0) {
                // 12:34:56
                hour = Integer.parseInt(time.substring(0, 2));
                minute = Integer.parseInt(time.substring(3, 5));
                second = Integer.parseInt(time.substring(6, 8));
            } else {
                //123456
                hour = Integer.parseInt(time.substring(0, 2));
                minute = Integer.parseInt(time.substring(2, 4));
                second = Integer.parseInt(time.substring(4, 6));
            }
        } catch (Exception e) {
            Log.i(TAG, "get date time value fail, exception=" + e.getMessage());
            return false;
        }
        Log.i(TAG, "set datetime=" + year + month + day + hour + minute + second);

        if (year <= 1970 || month < 0 || month >= 12 || day <= 0 || day > 31) {
            Log.i(TAG, "date data is invalid");
            return false;
        }
        if (hour < 0 || hour > 24 || minute < 0 || minute > 60 || second < 0 || second > 60) {
            Log.i(TAG, "time data is invalid");
            return false;
        }

        // set system date
        try {
            Calendar.getInstance().clear();
            Calendar canlendardate = Calendar.getInstance();
            canlendardate.set(Calendar.YEAR, year);
            canlendardate.set(Calendar.MONTH, month);
            canlendardate.set(Calendar.DAY_OF_MONTH, day);
            canlendardate.set(Calendar.HOUR_OF_DAY, hour);
            canlendardate.set(Calendar.MINUTE, minute);
            canlendardate.set(Calendar.SECOND, second);
            long when = canlendardate.getTimeInMillis();
            ret = SystemClock.setCurrentTimeMillis(when);
        } catch (SecurityException e) {
            e.printStackTrace();
            Log.i(TAG, "set canlendardate fail, SecurityException=" + e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "set canlendardate fail, exception=" + e.getMessage());
            return false;
        }

        Log.i(TAG, "set system datetime ret=" + ret);
        return true;
    }

    private boolean setLauncher(String packageName, boolean isRun) {
        PackageManager packageManager = VfiServiceApp.getContext().getPackageManager();
        PackageInfo packageinfo = null;
        try {
            packageinfo = packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageinfo == null) {
            Log.e(TAG, "Package Manager get package info by package name fail");
            return false;
        }

        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        resolveIntent.setPackage(packageinfo.packageName);

        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(resolveIntent, 0);

        if (resolveInfoList.iterator().hasNext()) {
            ResolveInfo resolveinfo = resolveInfoList.iterator().next();
            if (resolveinfo != null) {
                String className = resolveinfo.activityInfo.name;
                if (isRun) {
                    String packName = resolveinfo.activityInfo.packageName;
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ComponentName cn = new ComponentName(packName, className);
                    intent.setComponent(cn);
                    VfiServiceApp.getContext().startActivity(intent);
                    Log.d(TAG, "start " + packageName);
                }
                return setLauncher(packageManager, packageName, className);
            } else {
                Log.e(TAG, "Resolve Info List First Element is null");
                return false;
            }
        } else {
            Log.e(TAG, "Resolve Info List doesn't has element");
            return false;
        }
    }

    private boolean setLauncher(PackageManager packageManager, String packageName, String className) {
        Log.d(TAG, "set launcher start");
        ArrayList<IntentFilter> intentList = new ArrayList<>();
        ArrayList<ComponentName> cnList = new ArrayList<>();
        packageManager.getPreferredActivities(intentList, cnList, null);
        for (int i = 0; i < cnList.size(); i++) {
            IntentFilter dhIF = intentList.get(i);
            if (dhIF.hasAction(Intent.ACTION_MAIN) && dhIF.hasCategory(Intent.CATEGORY_HOME)) {
                try {
                    String name = cnList.get(i).getPackageName();
                    packageManager.clearPackagePreferredActivities(name);
                    Log.d(TAG, "clear launchers");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "exception : " + e.getMessage());
                }
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);
        filter.addCategory(Intent.CATEGORY_DEFAULT);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> mHome = packageManager.queryIntentActivities(intent, 0);

        final int N = mHome.size();
        ComponentName[] set = new ComponentName[N];
        for (int i = 0; i < N; i++) {
            ResolveInfo r = mHome.get(i);
            set[i] = new ComponentName(r.activityInfo.packageName, r.activityInfo.name);
        }
        ComponentName launcher = new ComponentName(packageName, className);
        packageManager.addPreferredActivity(filter, 1081344, set, launcher);
        return true;
    }

    @Override
    public boolean settingPCIRebootTime(int hour, int min, int sec) throws RemoteException {
        Log.d(TAG, "setPCIRebootTime: hour:" + hour + " min:" + min + " sec:" + sec);
        if (hour < 0 || hour > 23 || min < 0 || min > 59 || sec < 0 || sec > 59) {
            Log.e(TAG, "invalid parameters");
            return false;
        }
        ISystemSettings systemSettings = SdkApiHolder.getInstance().getSystemSettings();
        systemSettings.init(VfiServiceApp.getContext());

        int time = hour * 3600 + min * 60 + sec;
        boolean result = systemSettings.setTimerRebootTime(time);
        Log.d(TAG, "setPCIRebootTime result=" + result);
        return result;
    }

    @Override
    public long getPCIRebootTime() throws RemoteException {
        Log.d(TAG, "getPCIRebootTime execute()");

        ISystemSettings systemSettings = SdkApiHolder.getInstance().getSystemSettings();
        systemSettings.init(VfiServiceApp.getContext());
        int seconds = systemSettings.getTimerRebootTime();
        Log.d(TAG, "rebootTime=" + seconds);
        return seconds;
    }

    @Override
    public void setScreenLock(boolean isLock) throws RemoteException {
        Log.d(TAG, "setScreenLock execute()");
        Settings.System.putInt(VfiServiceApp.getContext().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, isLock ? 0 : 1);
    }

    @Override
    public boolean setDeviceBrightnessLevel(int level) throws RemoteException {
        Log.i(TAG, "setDeviceBrightnessLevel execute()");
        if (level < 0 || level > 255) {
            Log.i(TAG, "level is not allow");
            return false;
        }
        return Settings.System.putInt(VfiServiceApp.getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, level);
    }

    @Override
    public boolean isShowBatteryPercent(boolean isShow) throws RemoteException {
        Log.i(TAG, "isShowBatteryPercent execute()");
        return Settings.System.putInt(VfiServiceApp.getContext().getContentResolver(), "status_bar_show_battery_percent", isShow ? 1 : 0);
    }

    @SuppressLint("PrivateApi")
    @Override
    public void enableAlertWindow(String packageName) throws RemoteException {
        Log.i(TAG, "enableAlertWindow execute()");
        if (packageName == null) {
            Log.i(TAG, "packageName is null");
            return;
        }
        AppOpsManager mAppOpsManager = (AppOpsManager) VfiServiceApp.getContext().getSystemService(Context.APP_OPS_SERVICE);
        PackageManager packageManager = VfiServiceApp.getContext().getPackageManager();
        try {
            Method method = mAppOpsManager.getClass().getDeclaredMethod("setMode", int.class, int.class, String.class, int.class);
            if (method != null) {
                method.setAccessible(true);
                method.invoke(mAppOpsManager, 24, packageManager.getPackageInfo(packageName, 0).applicationInfo.uid, packageName, AppOpsManager.MODE_ALLOWED);
            }
        } catch (IllegalAccessException | InvocationTargetException |
                 PackageManager.NameNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("PrivateApi")
    @Override
    public void clearCachesByPackageName(String packageName) {
        PackageManager packageManager = VfiServiceApp.getContext().getPackageManager();
        Method method;
        try {
            method = PackageManager.class.getDeclaredMethod("deleteApplicationCacheFiles", String.class, IPackageDataObserver.class);
            method.invoke(packageManager, packageName, new ClearUserDataObserver());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setDefaultSystemLanguage(String language, String country) throws RemoteException {
        if (language == null || country == null) {
            return;
        }

        try {
            Locale mLocale = new Locale(language, country);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                LocaleList defaultList = LocaleList.getAdjustedDefault();
                Locale locale = new Locale(language, country);
                int index = defaultList.indexOf(locale);
                if (index != 0) {
                    //重新整合localeList顺序
                    Locale[] list;
                    if (index < 0) {
                        list = new Locale[defaultList.size() + 1];
                        list[0] = mLocale;
                        for (int i = 0; i < defaultList.size(); i++) {
                            list[i + 1] = new Locale(defaultList.get(i).getLanguage(), defaultList.get(i).getCountry());
                        }
                    } else {
                        list = new Locale[defaultList.size()];
                        int j = 0;
                        list[j++] = mLocale;
                        for (int i = 0; i < defaultList.size(); i++) {
                            if (defaultList.get(i).getCountry().equals(country) && defaultList.get(i).getLanguage().equals(language)) {
                                continue;
                            } else {
                                list[j++] = new Locale(defaultList.get(i).getLanguage(), defaultList.get(i).getCountry());
                            }
                        }
                    }
                    LocaleList localeList = new LocaleList(list);

                    Class classActivityManagerNative = Class.forName("android.app.ActivityManagerNative");
                    Method getDefault = classActivityManagerNative.getDeclaredMethod("getDefault");
                    Object objIActivityManager = getDefault.invoke(classActivityManagerNative);
                    Class classIActivityManager = Class.forName("android.app.IActivityManager");
                    Method getConfiguration = classIActivityManager.getDeclaredMethod("getConfiguration");
                    Configuration config = (Configuration) getConfiguration.invoke(objIActivityManager);
                    config.setLocales(localeList);
                    Class[] clzParams = {Configuration.class};
                    Method updateConfiguration = classIActivityManager.getDeclaredMethod("updatePersistentConfiguration", clzParams);
                    updateConfiguration.invoke(objIActivityManager, config);
                }
            } else {
                Class iActivityManager = Class.forName("android.app.IActivityManager");
                Class activityManagerNative = Class.forName("android.app.ActivityManagerNative");
                Method getDefault = activityManagerNative.getDeclaredMethod("getDefault");
                Object objIActMag = getDefault.invoke(activityManagerNative);
                Method getConfiguration = iActivityManager.getDeclaredMethod("getConfiguration");
                Configuration config = (Configuration) getConfiguration.invoke(objIActMag);
                config.locale = mLocale;
                Class clzConfig = Class.forName("android.content.res.Configuration");
                java.lang.reflect.Field userSetLocale = clzConfig.getField("userSetLocale");
                userSetLocale.set(config, true);
                Class[] clzParams = {Configuration.class};
                Method updateConfiguration = iActivityManager.getDeclaredMethod("updateConfiguration", clzParams);
                updateConfiguration.invoke(objIActMag, config);
                BackupManager.dataChanged("com.android.providers.settings");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setTimeZone(String timeZoneCode) throws RemoteException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!TimeZone.getDefault().getID().equals(timeZoneCode)) {
                final AlarmManager alarmManager = (AlarmManager) VfiServiceApp.getContext().getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    try {
                        alarmManager.setTimeZone(timeZoneCode);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            final AlarmManager alarmManager = (AlarmManager) VfiServiceApp.getContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                try {
                    alarmManager.setTimeZone(timeZoneCode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean setBootingAnimation(Bundle resources) throws RemoteException {
        Log.d(TAG, "setStartingAnimation");
        boolean isBraAndroid5 = false;
        if (Build.VERSION.SDK_INT <= 22) {
            // Android 5
            if (Build.DISPLAY.endsWith("BRA")) {
                Log.d(TAG, "It's Brazil Android 5: " + Build.DISPLAY);
                isBraAndroid5 = true;
            }
        } else {
            Log.v(TAG, "Android SDK: " + Build.VERSION.SDK_INT + ", ROM: " + Build.DISPLAY);
        }
        String[] keysOnBraAndroid5 = new String[]{"logoOnAndroid5Brazil", "soundOnAndroid5Brazil", "animationOnAndroid5Brazil"};

        for (String key : keysOnBraAndroid5) {
            if (resources.containsKey(key)) {
                String path = resources.getString(key);
                if (path.length() > 0) {
                    File file = new File(path);
                    if (!file.exists()) {
                        Log.e(TAG, "Path: " + path + " for " + key + " not exist!");
                        return false;
                    }
                }

                if (isBraAndroid5) {
                    //
                } else {
                    Log.e(TAG, "doesn't support the feature (" + key + ") on current device!");
                    return false;
                }
            }
        }
        ISystemSettings systemSettings = SdkApiHolder.getInstance().getSystemSettings();
        systemSettings.init(VfiServiceApp.getContext());

        if (resources.containsKey(MmkvUtils.BOOT_LOGON_ANDROID5_BR)) {
            if (Build.DISPLAY.compareTo("V1.1.0.202109090928 BRA") < 0) {    // "V1.1.0.202109090928 BRA" // "V1.1.0.202003131201 BRA"
                Log.w(TAG, "Not support the feature(logoOnAndroid5Brazil), please upgrade the ROM!");
            } else {
                String path = resources.getString(MmkvUtils.BOOT_LOGON_ANDROID5_BR);
                try {
                    systemSettings.setNewLogo(VfiServiceApp.getContext(), path);
                    MmkvUtils.setMmkvLogo(path);
                } catch (SDKException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Set logo failed!");
                    return false;
                }
                Log.d(TAG, "Set logo finished.");
            }
        }
        if (resources.containsKey(MmkvUtils.BOOT_SOUND_ANDROID5_BR)) {
            if (Build.DISPLAY.compareTo("V1.1.0.202109090928 BRA") < 0) {    // "V1.1.0.202109090928 BRA" // "V1.1.0.202003131201 BRA"
                Log.w(TAG, "Not support the feature(soundOnAndroid5Brazil), please upgrade the ROM!");
            } else {
                String path = resources.getString(MmkvUtils.BOOT_SOUND_ANDROID5_BR);
                boolean ret = false;
                try {
                    ret = systemSettings.setBootSound(path);
                    if (ret) {
                        MmkvUtils.setMmkvSound(path);
                    }
                } catch (SDKException e) {
                    e.printStackTrace();
                    ret = false;
                }
                if (!ret) {
                    Log.e(TAG, "Set sound failed!");
                    return false;
                }
                Log.d(TAG, "Set sound finished");
            }
        }

        if (resources.containsKey(MmkvUtils.BOOT_ANIMATION_ANDROID5_BR)) {
            String path = resources.getString(MmkvUtils.BOOT_ANIMATION_ANDROID5_BR);

            boolean ret = false;
            try {
                ret = systemSettings.setBootAnimation(path);
                if (ret) {
                    MmkvUtils.setMmkvAnimation(path);
                }
            } catch (SDKException e) {
                e.printStackTrace();
                ret = false;
            }
            if (!ret) {
                Log.e(TAG, "Set animation failed!");
                return false;
            }
            Log.d(TAG, "Set animation finished.");
        }


        return true;

    }

    @Override
    public Bundle getBootingAnimation(List<String> keyList) {
        String logo = "";
        String sound = "";
        String animation = "";
        Bundle bundle = new Bundle();

        if (keyList == null || keyList.size() < 1) {
            Log.e(TAG, "input list is empty!");
        } else {
            for (String key : keyList) {
                switch (key) {
                    case MmkvUtils.BOOT_LOGON_ANDROID5_BR:
                        logo = getFilename(MmkvUtils.getMmkvLogo());
                        Log.d(TAG, "Found logo: " + logo);
                        bundle.putString(MmkvUtils.BOOT_LOGON_ANDROID5_BR, logo);
                        break;
                    case MmkvUtils.BOOT_SOUND_ANDROID5_BR:
                        sound = getFilename(MmkvUtils.getMmkvSound());
                        Log.d(TAG, "Found sound: " + sound);
                        bundle.putString(MmkvUtils.BOOT_SOUND_ANDROID5_BR, sound);
                        break;
                    case MmkvUtils.BOOT_ANIMATION_ANDROID5_BR:
                        animation = getFilename(MmkvUtils.getMmkvAnimation());
                        Log.d(TAG, "Found animation:" + animation);
                        bundle.putString(MmkvUtils.BOOT_ANIMATION_ANDROID5_BR, animation);
                        break;
                }
            }
        }
        return bundle;
    }

    @Override
    public boolean loadCertificate(String certPath) throws RemoteException {
        Log.d(TAG, "loadCertificate execute");
        File file = new File(certPath);

        if (file.exists()) {
            int length = (int) file.length();
            byte[] certBuffer = new byte[length];
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                int read = fis.read(certBuffer);
                DevicePolicyManager dpm = (DevicePolicyManager) VfiServiceApp.getContext().getSystemService(
                        Context.DEVICE_POLICY_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    try {
                        // Verify cert is not currently installed.
                        if (dpm.hasCaCertInstalled(null, certBuffer)) {
                            Log.e(TAG, "Cert already on device");
                            return false;
                        }
                        if (!dpm.installCaCert(null, certBuffer)) {
                            Log.e(TAG, "installCaCert returned false.");
                            return false;
                        }
                        if (!dpm.hasCaCertInstalled(null, certBuffer)) {
                            Log.e(TAG, "Cannot find cert after installation.");
                            return false;
                        }
                        return true;

                    } catch (Exception e) {
                        Log.e(TAG, "Exception raised duing ACTION_INSTALL_CERT", e);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean setSettingPwd(String pwd) throws RemoteException {
        Log.d(TAG, "setSettingPwd execute");
        ISystemSettings mSystemSettings = SdkApiHolder.getInstance().getSystemSettings();
        mSystemSettings.init(VfiServiceApp.getContext());
        try {
            return mSystemSettings.setSettingsEntryPasskey(pwd);
        } catch (SDKException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * @param state
     * @throws RemoteException
     */
    @Override
    public void setWifiSettingState(boolean state) throws RemoteException {
        Log.d(TAG, "setWifiSettingState execute, set state=" + state);
        ISystemSettings mSystemSettings = SdkApiHolder.getInstance().getSystemSettings();
        mSystemSettings.setWifiSettingState(state);
    }

    /**
     * @return
     * @throws RemoteException
     */
    @Override
    public boolean getWifiSettingState() throws RemoteException {
        Log.d(TAG, "getsetWifiSettingState execute");
        ISystemSettings mSystemSettings = SdkApiHolder.getInstance().getSystemSettings();
        String res = mSystemSettings.getWifiSettingState();
        Log.d(TAG, "getsetWifiSettingState= " + res);
        if (res.toLowerCase().equals("false"))
            return false;
        else
            return true;
    }

    @Override
    public void setAllowChargingUsbDevices(boolean powerSupply) throws RemoteException {
        Log.d(TAG, "setPowerSupply() execute, set powerSupply=" + powerSupply);
        ISystemSettings mSystemSettings = SdkApiHolder.getInstance().getSystemSettings();
        try {
            mSystemSettings.setStopPowerState(VfiServiceApp.getContext(), !powerSupply);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return
     * @throws RemoteException
     */
    @Override
    public boolean getAllowChargingUsbDevicesStatus() throws RemoteException {
        Log.d(TAG, "getAllowChargingUsbDevicesStatus() execute");
        ISystemSettings mSystemSettings = SdkApiHolder.getInstance().getSystemSettings();
        try {
            String res = mSystemSettings.getStopPowerState(VfiServiceApp.getContext());
            if (res.toLowerCase().equals("false")) { // 停止供电=false，那就是在供电状态，所以返回true
                Log.d(TAG, "AllowChargingUsbDevicesStatus result=" + true);
                return true;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "AllowChargingUsbDevicesStatus result=" + false);
        return false;
    }

    /**
     * Send AT command
     *
     * @param command
     * @return
     * @throws RemoteException
     */
    @Override
    public String senAtCommand(String command) throws RemoteException {
        if (StringUtil.isEmpty(command))
            return "error param";

        Log.d(TAG, "senAtCommand() execute, command=" + command);
        ISystemSettings iSystemSettings = SdkApiHolder.getInstance().getSystemSettings();
        String res = iSystemSettings.senAtCommand(command);
        Log.d(TAG, "sendATCommand result:" + res);
        return res;
    }

    /**
     * Set device model
     *
     * @param model
     * @throws RemoteException
     */
    @Override
    public void setDeviceModel(String model) throws RemoteException {
        Log.d(TAG, "setDeviceModel() execute");
        ISystemSettings iSystemSettings = SdkApiHolder.getInstance().getSystemSettings();
        iSystemSettings.setDeviceModel(model);
    }

    /**
     * Get Bluetooth setting item status
     *
     * @return
     * @throws RemoteException
     */
    @Override
    public String getBTSettingState() throws RemoteException {
        Log.d(TAG, "getBTSettingState() execute");
        ISystemSettings iSystemSettings = SdkApiHolder.getInstance().getSystemSettings();
        String res = iSystemSettings.getBTSettingState();
        return res;
    }

    /**
     * Set Bluetooth setting item status
     *
     * @param state
     * @throws RemoteException
     */
    @Override
    public void setBTSettingState(boolean state) throws RemoteException {
        Log.d(TAG, "setBTSettingState() execute");
        ISystemSettings iSystemSettings = SdkApiHolder.getInstance().getSystemSettings();
        iSystemSettings.setBTSettingState(state);
    }

    /**
     * @param wallpaper
     * @throws RemoteException
     */
    @Override
    public boolean setWallpaper(Bitmap wallpaper) throws RemoteException {
        if (wallpaper == null) {
            Log.e(TAG, "wallpaper can't be null");
            return false;
        }
        int imageWidth = wallpaper.getWidth();
        int imageHeight = wallpaper.getHeight();

        //获取屏幕的宽和高
        WindowManager windowManager = (WindowManager) VfiServiceApp.getContext().getSystemService(Context.WINDOW_SERVICE);
        int screenWidth = windowManager.getDefaultDisplay().getWidth();
        int screenHeight = windowManager.getDefaultDisplay().getHeight();

        if (imageWidth != screenWidth || imageHeight != screenHeight) {
            Log.e(TAG, "The resolution of the picture must be " + screenHeight + " * " + screenWidth);
            return false;
        }
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(VfiServiceApp.getContext());
        try {
            wallpaperManager.setBitmap(wallpaper);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void upgradeCustomPackages(String path, final ICustomerConfigurationUpdateListener listener) throws RemoteException {
        SdkApiHolder.getInstance().getTerminalManager().upgradeCustomPackages(path, new OnCustomerConfigurationUpdateListener() {
            @Override
            public void onError(int i, String s) {
                try {
                    Log.d(TAG, "upgradeCustomPackages:" + i + "-->" + s);
                    if (listener != null)
                        listener.onError(i, s);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Return whether screen is Locked.
     *
     * @return {@code true}: yes<br>{@code false}: no
     * @throws RemoteException
     */
    @Override
    public boolean isScreenLock() throws RemoteException {
        Log.d(TAG, "isScreenLock execute");
        KeyguardManager km = (KeyguardManager) VfiServiceApp.getContext().getSystemService(Context.KEYGUARD_SERVICE);
        if (km == null) return false;
        return km.inKeyguardRestrictedInputMode();
    }

    /**
     * Set the duration of sleep.
     *
     * @param duration the duration in milliseconds.
     * @throws RemoteException
     */
    @Override
    public void setSleepDuration(int duration) throws RemoteException {
        Log.d(TAG, "setSleepDuration execute");
        if (duration < 10000) {
            duration = 10000;
        }
        if (duration == Integer.MAX_VALUE) {
            duration = Integer.MAX_VALUE - 1;
        }
        Settings.System.putInt(
                VfiServiceApp.getContext().getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT,
                duration);
    }

    /**
     * Return the duration of sleep.
     *
     * @return the duration of sleep.
     * @throws RemoteException
     */
    @Override
    public int getSleepDuration() throws RemoteException {
        Log.d(TAG, "getSleepDuration execute");
        try {
            return Settings.System.getInt(VfiServiceApp.getContext().getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Exception raised during getSleepDuration", e);
            return -123;
        }
    }

    public List getUserCaCertificate() throws RemoteException {
        Log.d(TAG, "getUserCaCertificate execute");

        DevicePolicyManager dpm = (DevicePolicyManager) VfiServiceApp.getContext().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            try {
                List<byte[]> cas = dpm.getInstalledCaCerts(null);
                if (cas == null || cas.size() == 0) {
                    return null;
                }
                return cas;

            } catch (Exception e) {
                Log.e(TAG, "Exception raised during ACTION_GET_CERT", e);
            }
        }
        return null;
    }

    static class ClearUserDataObserver extends IPackageDataObserver.Stub {

        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            Log.i(TAG, "IPackageDataObserver succeeded: " + succeeded);
        }
    }

    String getFilename(String path) {
        if (null == path) {
            return "";
        } else if (path.indexOf('/') >= 0) {
            return path.substring(path.lastIndexOf('/') + 1);
        } else {
            return path;
        }
    }

}
