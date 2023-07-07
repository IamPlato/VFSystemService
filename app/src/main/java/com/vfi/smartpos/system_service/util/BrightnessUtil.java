package com.vfi.smartpos.system_service.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

/**
 * Screen brightness tool class
 */
public class BrightnessUtil {

    /**
     * Get system screen brightness
     * @param context
     * @return
     */
    public static int getMyBrightness(Context context) {
        int brightness = -1;
        ContentResolver resolver = context.getContentResolver();
        try {
            brightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        Log.e("brightData","getBrightness: "+brightness);
        return brightness;
    }

    /**
     * Obtain the maximum screen brightness of the system
     * @param context
     * @return
     */
    public static int getMaxBrightness(Context context) {
        int brightnessSettingMaximumId = context.getResources().getIdentifier("config_screenBrightnessSettingMaximum", "integer", "android");
        int brightnessSettingMaximum = context.getResources().getInteger(brightnessSettingMaximumId);
        Log.e("maxBrightData","getMaxBrightness: "+brightnessSettingMaximum);
        return brightnessSettingMaximum;
    }

    /**
     * Adjust the current screen brightness
     * @param lightnumber
     * @param activity
     */
    public static void setSystemLight(int lightnumber, Activity activity){
        Window window = activity.getWindow();//对当前窗口进行设置
        WindowManager.LayoutParams layoutparams = window.getAttributes();//获取窗口属性为后面亮度做铺垫作用
        layoutparams.screenBrightness =lightnumber / 255.0f;//用窗口管理（自定义的）layoutparams获取亮度值，android亮度值处于在0-255之间的整形数值
        window.setAttributes(layoutparams);//设置当前窗口屏幕亮度
    }

    /**
     * Modify the screen brightness value in Setting
     * @param context
     * @param birghtessValue
     */
    public static void ModifySettingsScreenBrightness(Context context, int birghtessValue) {
        // 首先需要设置为手动调节屏幕亮度模式
        setScreenManualMode(context);

        ContentResolver contentResolver = context.getContentResolver();
        Log.e("ccm=======>", "birghtessValue: "+birghtessValue);
        Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, birghtessValue);
    }


    /**
     * Turn off the light sense and set the manual backlight mode
     * @param context
     */
    public static void setScreenManualMode(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        try {
            int mode = Settings.System.getInt(contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Automatically adjust backlight mode
    public static void setScreenAutomaticMode(Context context){
        ContentResolver contentResolver = context.getContentResolver();
        try {
            int mode = Settings.System.getInt(contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                Settings.System.putInt(contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

}

