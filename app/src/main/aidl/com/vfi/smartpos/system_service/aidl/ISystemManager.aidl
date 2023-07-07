// SystemService.aidl
package com.vfi.smartpos.system_service.aidl;

import android.os.Bundle;
import com.vfi.smartpos.system_service.aidl.IAppInstallObserver;
import com.vfi.smartpos.system_service.aidl.IAppDeleteObserver;
import com.vfi.smartpos.system_service.aidl.networks.INetworkManager;
import com.vfi.smartpos.system_service.aidl.settings.ISettingsManager;
import com.vfi.smartpos.system_service.aidl.IVerifysignCallback;

interface ISystemManager {
	/**
	 * Install an apk
	 * @param apkPath - apk's absolute url.
	 * @param observer - callback handler.
	 * @param installerPackageName - packagename of installer apk.
	 * @see IAppInstallObserver
	 * @since 1.0.0
	 */
    void installApp(String apkPath, IAppInstallObserver observer, String installerPackageName);

	/**
	 * remove an apk
	 * @param packageName - package name which need to remove.
	 * @param observer - callback handler.
	 * @see IAppDeleteObserver
	 * @since 1.0.0
	 */
    void uninstallApp(String packageName, IAppDeleteObserver observer);

	/**
	 * reboot terminal
	 * @since 1.0.0
	 */
    void reboot();

    /**
     * enable/disable home key
     * @param state true-disable home key  false-enable home key
     * @since 1.0.0
     */
    void isMaskHomeKey(boolean state);

    /**
     * enable/disable system status bar
     * @param state true-disable system status bar  false-enable system status bar
     * @since 1.0.0
     */
    void isMaskStatusBard(boolean state);

    /**
     * upgrade K21 driver
     * @param sysBin - path of sys bin file
     * @param appBin - path of app bin file
     * @return result
     * @since 1.0.0
     */
    boolean chekcK21Update(String sysBin, String appBin);

    /**
     * upgrade ROM driver
     * @param zipPath - path of rom package
     * @since 1.0.0
     */
    void updateROM(String zipPath);

    /**
     * get NetworkManager object
     * @return NetworkManager object
     * @since 1.0.0
     * @see INetworkManager
     */
    INetworkManager getNetworkManager();

    /*
     * set location mode
     * <p>If turn on status 2 or 3 from 0 or 1, system will pop up a window to let user allow IZat for hardware acceleration</p>
     * @param status 0: off 1: sensor only 2: battery saving 3: high accuracy
     * @since 1.3.4
     */
    void setLocationMode( int status );

    /*
     * get ADB status
     * @return true-Adb enable false-Adb disable
     * @since 1.3.4
     */
    boolean isAdbMode();

    /**
    * kill application
    * @param packageName application packageName
    * @return true-success false-fail
    * @since 1.5.2
    */
    boolean killApplication(String packageName);

    /**
    * restart application
    * @param packageName application packageName
    * @return true-success false-fail
    * @since 1.5.2
    */
    boolean restartApplication(String packageName);

    /**
    * init logcat configuration.
    *
    * @param logcatBufferSize: set logcat buffer size.
    * <blockquote>
    * if logcatBufferSize == 0, set default logcat buffer size
    * logcatBufferSizeSuffix: set logcat buffer size suffix. 0: "M", 1: "K".
    * </bolckquote>
    * @since 1.5.2
    */
    void initLogcat(int logcatBufferSize, int logcatBufferSizeSuffix, in Bundle bundle);

    /**
    * get log buffer file
    *
    * @param logcatFileName: specify the log file name, if logcatFileName == null, set default logcat file name
    * @param compressType: set logcat compress type. 0: "none", 1: "gz".
    * <BR><b>user needs to delete log file. if the log file locates in default log path, it will be deleted 7 days after creation.</b>
    * @since 1.5.2
    */
    String getLogcat(String logcatFileName, int compressType);

    /**
    * Get the usage count of all apps to be used at the specific time range
    *
    * @param beginTime get apps info begin time
    * <br>For example, beginTime = Calendar.getInstance().setDate(date).getTimeInMillis();
    * @param endTime get apps info end time
    * <br>For example, endTime = Calendar.getInstance().getTimeInMillis();
    * @return Bundle "UsageStatsList" : json string of List<UsageStats> object.
    * @since 1.6.0
    */
    Bundle getLaunchAppsInfo(long beginTime, long endTime);

    /**
    * get ISettingsManager objet
    * <p>get SettingsManager to execute settings actions</p>
    * @return ISettingsManager
    * @since 1.6.0
    * @see ISettingsManager
    **/
    ISettingsManager getSettingsManager();

    /**
    * take capture of the screen
    * @return Bitmap data of screen capture
    * @since 1.8.0
    */
    Bitmap takeCapture();

    /**
    * device shutdown
    * @since 1.8.5
    */
    void shutdownDevice();

    boolean UpdateSecurityDriver(String updatePackagePath);

    /**
     * Whether the application is currently foreground or background
     * @since 1.11.3
     * @reture true-foreground false-background
     */
    boolean isAppForeground(String packageName);

    /**
     * update base band, will reboot device automatically after call this API, pls note: only support MG baseband
     * @param filePath
     * @return false-when copy base band file failed, when success will reboot device.
     * @throws RemoteException
     */
    boolean updateBaseBand(String filePath);

    /**
     * Get screen brightness
     * @return brightness
     * @throws RemoteException
     */
    int getScreenBrightness();

    /**
     * Input data to adjust screen brightness:
     * the data is greater than or equal to 10 and less than or equal to 255(10<=data<=255)
     * @param brightnessData
     * @throws RemoteException
     */
    void changeScreenBrightness(int brightnessData);
}
