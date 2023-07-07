package com.vfi.smartpos.system_service.util;

import com.vfi.smartpos.system_service.VfiServiceApp;

import static com.vfi.smartpos.system_service.VfiServiceApp.getMmkvBootingAnimation;

public class MmkvUtils {
    public static final String MMKV_KEY_LOGO = "LOGO";
    public static final String MMKV_KEY_SOUND = "SOUND";
    public static final String MMKV_KEY_ANIMATION = "ANIMATION";
    public static final String MMKV_KEY_TF_ENABLE = "TF_ENABLE";

    public static final String BOOT_LOGON_ANDROID5_BR = "logoOnAndroid5Brazil";
    public static final String BOOT_SOUND_ANDROID5_BR = "soundOnAndroid5Brazil";
    public static final String BOOT_ANIMATION_ANDROID5_BR = "animationOnAndroid5Brazil";

    public static String getMmkvLogo() {
        return getMmkvBootingAnimation().decodeString(MMKV_KEY_LOGO, "");
    }

    public static void setMmkvLogo(String mmkvLogo) {
        getMmkvBootingAnimation().encode(MMKV_KEY_LOGO, mmkvLogo);
    }

    public static String getMmkvSound() {
        return getMmkvBootingAnimation().decodeString(MMKV_KEY_SOUND, "");
    }

    public static void setMmkvSound(String mmkvSound) {
        getMmkvBootingAnimation().encode(MMKV_KEY_SOUND, mmkvSound);
    }

    public static String getMmkvAnimation() {
        return getMmkvBootingAnimation().decodeString(MMKV_KEY_ANIMATION, "");
    }

    public static void setMmkvAnimation(String mmkvAnimation) {
        getMmkvBootingAnimation().encode(MMKV_KEY_ANIMATION, mmkvAnimation);
    }

    public static boolean getMmkvTfEnable() {
        return VfiServiceApp.getMmkvTF().decodeBool(MMKV_KEY_TF_ENABLE, true);
    }

    public static void setMmkvTfEnable(boolean enable) {
        VfiServiceApp.getMmkvTF().encode(MMKV_KEY_TF_ENABLE, enable);
    }
}
