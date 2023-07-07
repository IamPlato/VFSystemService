// ISettingsManager.aidl
package com.vfi.smartpos.system_service.aidl.settings;

// Declare any non-default types here with import statements
import android.os.Bundle;
import com.vfi.smartpos.system_service.aidl.settings.ICustomerConfigurationUpdateListener;

interface ISettingsManager {

    /**
    * settings set actions
    *
    * @param settingsType: please refer to SettingType class; current include:
    * <li>SettingType.LAUNCHER</li>
    * <li>SettingType.DATE_TIME</li>
    * <li>SettingType.POWER_OPTIMIZE</li>
    * <li>SettingType.TF_ENABLE</li>
    *
    * @param bundle:  please refer to SettingActions;
    * <ul> if settingsType is SettingType.LAUNCHER
    *   <li>SettingsActions.LauncherActions.SET_LAUNCHER</li>
    * </ul
    * <ul> if settingsType is SettingType.DATE_TIME
    *   <li>SettingsActions.DATE_TIME.SET_AUTO_SYSTEM_TIME_STATE</li>
    *   <li>SettingsActions.DATE_TIME.SET_AUTO_SYSTEM_TIME_ZONE_STATE</li>
    *   <li>SettingsActions.DATE_TIME.SET_SYSTEM_TIME</li>
    * </ul
    *
    * @return 0: set success;   -1: set fail
    * @since 1.6.0
     * <pre>
     * For example:
     *       since 1.6.0.0
     *       SettingsType.DATE_TIME:
     *       //set system time whether to sync auto network time
     *       Bundle bundle = new Bundle();
     *       bundle.putString("SYSTEM_TIME_ACTIONS", SettingsActions.SystemTimeActions.SET_AUTO_SYSTEM_TIME_STATE);
     *       More informations of "SYSTEM_TIME_ACTIONS" pleae refer to SettingsActions.SystemTimeActions class;
     *       bundle.putInt("AUTO_SYSTEM_TIME", 0);
     *       0: disable sync
     *       1: sync
     *
     *       //set system time zone whether to sync auto network time zone
     *       Bundle bundle = new Bundle();
     *       bundle.putString("SYSTEM_TIME_ACTIONS", SettingsActions.SystemTimeActions.SET_AUTO_SYSTEM_TIME_ZONE_STATE);
     *       More informations of "SYSTEM_TIME_ACTIONS" pleae refer to SettingsActions.SystemTimeActions class;
     *       bundle.putInt("AUTO_SYSTEM_TIME_ZONE", 0);
     *       0: disable sync
     *       1: sync
     *
     *       since 1.6.0.1
     *       //set system time
     *       Bundle bundle = new Bundle();
     *       bundle.putString("SYSTEM_TIME_ACTIONS", SettingsActions.SystemTimeActions.SET_AUTO_SYSTEM_TIME_ZONE_STATE);
     *       More informations of "SYSTEM_TIME_ACTIONS" pleae refer to SettingsActions.SystemTimeActions class;
     *       bundle.putString("SYSTEM_DATE", "20200707"); date format is yyyyMMdd;
     *       bundle.putString("SYSTEM_TIME", "150629"); time format is HHmmss;
     *
     *       since 1.6.0.1
     *       //set launcher
     *       Bundle bundle = new Bundle();
     *       bundle.putString("LAUNCHER_ACTIONS", SettingsActions.LauncherActions.SET_LAUNCHER);
     *       More informations of "LAUNCHER_ACTIONS" pleae refer to SettingsActions.LauncherActions class;
     *       bundle.putString("LAUNCHER_PACKAGE_NAME", "com.vfi.android.payment"); // passing launcher application package name.
     *       bundle.putBoolean("RUN_PACKAGE", true); //true: run application, false: not run application.
     *
     *
     *       since 1.11.7
     *       //set power optimize
     *       Bundle bundle = new Bundle();
     *       bundle.putString("POWER_OPTIMIZE_PACKAGE", "com.vfi.android.payment");// Specified power optimization app package name.
     *       bundle.putBoolean("POWER_OPTIMIZE_ENABLE", true); //true: enable power optimize, false: close power optimize.
     *
     *       since 1.11.7
     *       //set tf enable
     *       Bundle bundle = new Bundle();
     *       bundle.putBoolean("TF_ENABLE", true); //true: enable tf, false: disable tf,default is enable.
     *
     * </pre>
    *
    **/
    int settingsSetActions(int settingsType, in Bundle bundle);

    /**
    * settings read actions
    *
    * @param settingsType: please refer to SettingType; current only 'SettingsType.DATE_TIME'
    *
    * @param bundle: please refer to SettingActions; current include:
    *   <li>SettingActions.SystemTimeActions.GET_AUTO_SYSTEM_TIME_STATE</li>
    *   <li>SettingActions.SystemTimeActions.GET_AUTO_SYSTEM_TIME_ZONE_STATE</li>
    *
    * @return
    * <ul> if SettingActions is SystemTimeActions.GET_AUTO_SYSTEM_TIME_STATE
    *   <li>SYSTEM_TIME_ACTIONS<int> 1: sync auto system time;    0: disable sync auto system time</li>
    * </ul>
    * <ul> if SettingActions is SystemTimeActions.GET_AUTO_SYSTEM_TIME_ZONE_STATE
    *   <li>AUTO_SYSTEM_TIME_ZONE<int> 1: sync auto system time zone;    0: disable sync auto system time zone</li>
    * </ul>
    *
    * @since 1.6.0
    * <pre>
    *   For example:
    *   SettingsType.DATE_TIME:
    *       //get system time whether to sync auto network time
    *       Bundle bundle = new Bundle();
    *       bundle.putString("SYSTEM_TIME_ACTIONS", SettingsActions.SystemTimeActions.GET_AUTO_SYSTEM_TIME_STATE);
    *       More informations of "SYSTEM_TIME_ACTIONS" pleae refer to SettingsActions.SystemTimeActions class;
    *
    *   @return bundle
    *       int state = bundle.getInt("AUTO_SYSTEM_TIME");
    *       state: 1: sync auto system time;    0: disable sync auto system time
    *
    *       //get system time zone whether to sync auto network time zone
    *       Bundle bundle = new Bundle();
    *       bundle.putString("SYSTEM_TIME_ACTIONS", SettingsActions.SystemTimeActions.GET_AUTO_SYSTEM_TIME_ZONE_STATE);
    *       More informations of "SYSTEM_TIME_ACTIONS" pleae refer to SettingsActions.SystemTimeActions class;
    *
    *   @return bundle
    *       int state = bundle.getInt("AUTO_SYSTEM_TIME_ZONE");
    *       state: 1: sync auto system time zone;    0: disable sync auto system time zone
    * </pre>
    **/
    Bundle settingsReadActions(int settingsType, in Bundle bundle);


    /**
    * set PCI reboot time.
    * @param hour(24-hour format range:0~23)
    * @param min(range:0~59)
    * @param sec(range:0~59)
    * @return result
    * @since 1.7.0
    **/
    boolean settingPCIRebootTime(int hour, int min, int sec);

    /**
    * get PCI reboot time.
    * @return seconds for unit
    * @since 1.7.0
    **/
    long getPCIRebootTime();

    /**
     * set Screen Lock
     * @param isLock true lock screen, false not lock
     * @since 1.8.2
     **/
    void setScreenLock(boolean isLock);

    /**
    * set device brightness level
    * @param level 0~255
    * @return true for success, false for failure
    * @since 1.8.2
    **/
    boolean setDeviceBrightnessLevel(int level);

    /**
    * set is show battery percent in status bar
    * @param isShow, true for show, false for hide
    * @return true for success, false for failure
    * @since 1.8.2
    **/
    boolean isShowBatteryPercent(boolean isShow);

     /**
     * enable the permission by packageName
     * <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
     * @param packageName, the packageName of application
     * @since 1.8.3
     **/
     void enableAlertWindow(String packageName);

     /**
     * clear application caches by packageName
     * @param packageName, the packageName of application
     * @since 1.8.8
     **/
     void clearCachesByPackageName(String packageName);

     /**
      * set default system language
      * @param language ,  language name
      * @param country ,  country name
      * @since 1.10.1
      *
      * Below is language and country define
      *   zh_CN en_AU en_IN fr_FR it_IT es_ES et_EE de_DE nl_NL cs_CZ pl_PL ja_JP
      *   zh_TW en_US zh_HK ru_RU ko_KR nb_NO es_US da_DK el_GR tr_TR pt_PT pt_BR rm_CH sv_SE bg_BG
      *   ca_ES en_GB fi_FI hi_IN hr_HR hu_HU in_ID iw_IL lt_LT lv_LV ro_RO sk_SK sl_SI sr_RS uk_UA
      *   vi_VN tl_PH ar_EG fa_IR th_TH sw_TZ ms_MY af_ZA zu_ZA am_ET hi_IN en_XA ar_XB fr_CA km_KH
      *   lo_LA ne_NP si_LK mn_MN hy_AM az_AZ ka_GE my_MM mr_IN ml_IN is_IS mk_MK ky_KG eu_ES gl_ES
      *   bn_BD ta_IN kn_IN te_IN uz_UZ ur_PK kk_KZ
      *
      * For example: if you want to set Chinese language, you can call like this: setDefaultSystemLanguage("zh", "CN");
      */
     void setDefaultSystemLanguage(String language, String country);

     /**
      * Sets the system's persistent default time zone. This is the time zone for all apps, even after a reboot.
      * @param timeZoneCode represents the assigned time zone id, such as "Asia/Shanghai or Asia/Tokyo".
      * @since 1.11.3
      **/
      void setTimeZone(String timeZoneCode);

    /**
    * change the default Animiation while device is starting
    * @param resources,
    * logoOnAndroid5Brazil(String), format: .img. Accepted on ROM V1.1.0.202109090928 BRA or higher. Return false on other ROM (not Brazil Android 5). Skip to set if ROM early than 202109090928 BRA.
    * soundOnAndroid5Brazil(String), format: .wav. Accepted on ROM V1.1.0.202109090928 BRA or higher. Return false on other ROM (not Brazil Android 5). Skip to set if ROM early than 202109090928 BRA.
    * animationOnAndroid5Brazil(String), format: .zip. desc.txt is need to be archived into the zip. Return false on other ROM (not Brazil Android 5).
    * @return true for success, false for failure.
    * @since 1.11.4
    */
    boolean setBootingAnimation(in Bundle resources);

    /**
    * Get Animiation parameters / the filename which was set by API setBootingAnimation
    * @param keyList, logoOnAndroid5Brazil(String), soundOnAndroid5Brazil(String), animationOnAndroid5Brazil(String)
    * @return Bundle, values of booting animation parameters
    * <pre>
    *   For example:
    *       List<String> keyList = Arrays.asList("logoOnAndroid5Brazil", "soundOnAndroid5Brazil", "animationOnAndroid5Brazil");
    *       Bundle bundle = getBootingAnimation(keyList);
    *
    *   @return bundle
    *       String logo = bundle.getString("logoOnAndroid5Brazil");
    *       String sound = bundle.getString("soundOnAndroid5Brazil");
    *       String animation = bundle.getString("animationOnAndroid5Brazil");
    *
    * </pre>
    * @since 1.11.4
    */
    Bundle getBootingAnimation(in List<String> keyList);

   /**
    * Installs the given certificate as a user CA.
    * @param the path of cetificate
    * @since 1.11.7
    */
    boolean loadCertificate(String certPath);

   /**
    * Set the password set by the system, and remove the password when passing a null value.
    * @param password to set (limit 2~10 number)
    * @since 1.11.9
    * note:ROM version needs to be upgraded to 20220804 or above
    */
    boolean setSettingPwd(String pwd);

    /**
     * just for Korean ROM, show Wifi setting items, WIFI state is false by default.
     * if set state is true, will show WIFI setting items, pls notice:this API need to reboot device to let this setting take effect.
     * @param state
     * @throws RemoteException
     */
    void setWifiSettingState(boolean state);

    /**
     * just for Korean ROM, get current Wifi setting state.
     * @return
     * @throws RemoteException
     */
    boolean getWifiSettingState();


    /**
     * enable device power supply to other device(just support V2 device), need to reboot device to let this setting take effect.
     * @param powerSupply
     * @throws RemoteException
     */
    void setAllowChargingUsbDevices(boolean powerSupply);

    /**
     * get Power Supply State
     * @return true-enable power supply; false-disable power supply
     * @throws RemoteException
     */
    boolean getAllowChargingUsbDevicesStatus();

    /**
     * just for Korean ROM(equel or greater than V1.1.1.202211251342), send AT command
     * @param command
     * @return
     * @throws RemoteException
     */
    String senAtCommand(String command);

    /**
     * just for Korean ROM(equel or greater than V1.1.1.202211251342), set device model
     * @param model
     * @throws RemoteException
     */
    void setDeviceModel(String model);

    /**
     * just for Korean ROM(equel or greater than V1.1.1.202211251342), get Bluetooth setting item status
     * @return
     * @throws RemoteException
     */
    String getBTSettingState();

    /**
     * just for Korean ROM(equel or greater than V1.1.1.202211251342),
     * set Bluetooth setting item status, need to reboot device to let this setting take effect.
     * @param state - true, display bluetooth setting items; false, do not display bluetooth setting items
     * @throws RemoteException
     */
    void setBTSettingState(boolean state);

    /**
     * set wallpager.
     * @param wallpaper
     * @throws RemoteException
     */
    boolean setWallpaper(in Bitmap wallpaper);

    /**
     * upgrade custom packages
     * @param path
     * @param ICustomerConfigurationUpdateListener
     * @throws RemoteException
     */
    void upgradeCustomPackages(String path, ICustomerConfigurationUpdateListener listener);

    /**
     * Return whether screen is Locked.
     * @return {@code true}: yes<br>{@code false}: no
     * @throws RemoteException
     */
    boolean isScreenLock();

    /**
     * Set the duration of sleep.
     * @param duration the duration in milliseconds,value range:10000~2147483646
     * @throws RemoteException
     */
    void setSleepDuration(int duration);

    /**
     * Return the duration of sleep.
     * @return the duration of sleep.
     * @throws RemoteException
     */
    int getSleepDuration();
}
