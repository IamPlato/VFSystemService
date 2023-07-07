package com.vfi.smartpos.system_service;
/*
 *  author: Derrick
 *  Time: 2019/7/3 15:40
 */

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.ProxyInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.verifone.smartpos.api.SdkApiHolder;
import com.verifone.smartpos.api.system.ISystemSettings;
import com.verifone.smartpos.api.system.NetworkWhitelistingListener;
import com.vfi.smartpos.system_service.aidl.networks.INetworkManager;
import com.vfi.smartpos.system_service.util.APN;
import com.vfi.smartpos.system_service.util.NetUtils;
import com.vfi.smartpos.system_service.aidl.IAddNetworkAllowedListObserver;

import org.greenrobot.eventbus.meta.SubscriberInfo;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.vfi.smartpos.system_service.util.NetUtils.getIPv4Address;

public class NetworkManager extends INetworkManager.Stub {

    private static final String TAG = "INetworkManager";

    private static NetworkManager instance;

    private static final Uri APN_URI = Uri.parse("content://telephony/carriers");
    private static final Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    private NetworkManager() {
    }


    @Override
    public void setNetworkType(int mode) throws RemoteException {
        Context context = VfiServiceApp.getContext();
        //<uses-permission android:name="android.permission.WRITE_SECURE_SETTINGS"/>
        Settings.Secure.putInt(context.getContentResolver(), "preferred_network_mode", mode);
        // change mode
        Intent intent = new Intent("com.android.phone.CHANGE_NETWORK_MODE");
        intent.putExtra("com.android.phone.NEW_NETWORK_MODE", mode);
        context.sendBroadcast(intent);
    }

    @Override
    public int getNetworkType() throws RemoteException {
        Context context = VfiServiceApp.getContext();
        try {
            return Settings.Secure.getInt(context.getContentResolver(), "preferred_network_mode");
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean getMobileDataState() {
        try {
            TelephonyManager telephonyService = (TelephonyManager) VfiServiceApp.getContext().getSystemService(Context.TELEPHONY_SERVICE);
            Method getMobileDataEnabledMethod = telephonyService.getClass().getDeclaredMethod("getDataEnabled");
            if (null != getMobileDataEnabledMethod) {
                boolean mobileDataEnabled = (Boolean) getMobileDataEnabledMethod.invoke(telephonyService);
                return mobileDataEnabled;
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error getting mobile data state", ex);
        }
        return false;
    }

    public void enableMobileData(boolean isEnable) {
        try {
            TelephonyManager telephonyService = (TelephonyManager) VfiServiceApp.getContext().getSystemService(Context.TELEPHONY_SERVICE);
            Method setMobileDataEnabledMethod = telephonyService.getClass().getDeclaredMethod("setDataEnabled", boolean.class);
            if (null != setMobileDataEnabledMethod) {
                setMobileDataEnabledMethod.invoke(telephonyService, isEnable);
                Log.d(TAG, "enableMobileData to " + isEnable + ", result:" + getMobileDataState());
            } else {
                Log.e(TAG, "cannot call the setDataEnabled");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error setting mobile data state", ex);
        }
    }

    public void enableMobileData_(boolean isEnable) {
        Context context = VfiServiceApp.getContext();

        ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Class<?> cmClass = mConnectivityManager.getClass();
        Class<?>[] argClasses = new Class[1];
        argClasses[0] = boolean.class;

        // 反射ConnectivityManager中hide的方法setMobileDataEnabled，可以开启和关闭GPRS网络
        Method method;
        try {
            method = cmClass.getMethod("setMobileDataEnabled", argClasses);
            method.invoke(mConnectivityManager, isEnable);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Bundle getSelectedApnInfo() throws RemoteException {
        return new APN().getSelectedAPNConfigs();
    }


    @Override
    public void enableWifi(boolean state) throws RemoteException {
        Context context = VfiServiceApp.getContext();
        WifiManager wifiManager;
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
// 调用函数打开/关闭WiFi,status为boolean变量true/false
        wifiManager.setWifiEnabled(state);
    }

    @Override
    public void enableAirplayMode(boolean state) throws RemoteException {
        Context context = VfiServiceApp.getContext();
        ContentResolver cr = context.getContentResolver();
        int stateValue;
        if (state) {
            stateValue = 1;
        } else {
            stateValue = 0;
        }
        Settings.Global.putString(cr, Settings.Global.AIRPLANE_MODE_ON, stateValue + "");
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", state);
        context.sendBroadcast(intent);
    }

    @Override
    public int setAPN(Bundle infos) throws RemoteException {
        return new APN().setAPN(infos);
    }

    @Override
    public int deleteAPN(String apn) throws RemoteException {
        return new APN().deleteAPN(apn);
    }

    @Override
    public int setDataRoamingEnabled(boolean enabled, int slotId) throws RemoteException {
        SubscriptionManager subscriptionManager = (SubscriptionManager) VfiServiceApp.getContext().getSystemService(Service.TELEPHONY_SUBSCRIPTION_SERVICE);
        int subid = -1;
        try {
            Class<?> classType = SubscriptionManager.class;
            Method getMethod = classType.getDeclaredMethod("getSubId", int.class);
            int[] subids = (int[]) getMethod.invoke(classType, 0);
            if (subids != null && subids.length >= 1) {
                subid = subids[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Method method;
        int result = -1;
        try {
            method = subscriptionManager.getClass().getMethod("setDataRoaming", int.class, int.class);
            result = (int) method.invoke(subscriptionManager, enabled ? 1 : 0, subid);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "setDataRoamingEnabled: " + result);
        return result;
    }

    @Override
    public int selectMobileDataOnSlot(int slot) throws RemoteException {
        --slot;
        return new APN().selectMobileDataOnSlot(slot);
    }

    @Override
    public boolean isMultiNetwork() throws RemoteException {
        try {
            Log.i(TAG, "isMultiNetwork() executed");
            ISystemSettings systemSettings = SdkApiHolder.getInstance().getSystemSettings();
            systemSettings.init(VfiServiceApp.getContext());
            return systemSettings.isMultiNetwork();
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    @Override
    public void setMultiNetwork(boolean enable) throws RemoteException {
        try {
            Log.i(TAG, "setMultiNetwork() executed");
            ISystemSettings systemSettings = SdkApiHolder.getInstance().getSystemSettings();
            systemSettings.init(VfiServiceApp.getContext());
            systemSettings.setMultiNetwork(enable);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public String getMultiNetworkPrefer() throws RemoteException {
        Log.i(TAG, "getMultiNetworkPrefer() executed");
        ISystemSettings systemSettings = SdkApiHolder.getInstance().getSystemSettings();
        systemSettings.init(VfiServiceApp.getContext());
        return systemSettings.getMultiNetworkPrefer();
    }

    @Override
    public boolean setMultiNetworkPrefer(String prefer) throws RemoteException {
        Log.i(TAG, "setMultiNetworkPrefer() executed");
        if (prefer == null) {
            Log.i(TAG, "prefer is null");
            return false;
        }
        ISystemSettings systemSettings = SdkApiHolder.getInstance().getSystemSettings();
        systemSettings.init(VfiServiceApp.getContext());
        return systemSettings.setMultiNetworkPrefer(prefer);
    }

    @SuppressLint("PrivateApi")
    @Override
    public void setEthernetStaticIp(Bundle bundle) throws RemoteException {
        Log.i(TAG, "setEthernetStaticIp() executed");
        if (bundle == null) {
            Log.i(TAG, "bundle is null");
            return;
        }
        final String STATIC_IP = bundle.getString("STATIC_IP", "");
        final String STATIC_GATEWAY = bundle.getString("STATIC_GATEWAY", "");
        final String STATIC_NETMASK = bundle.getString("STATIC_NETMASK", "");
        final String STATIC_DNS1 = bundle.getString("STATIC_DNS1", "");
        final String STATIC_DNS2 = bundle.getString("STATIC_DNS2", "");
        Log.i(TAG, "use Static IP and Change IP to " + STATIC_IP);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Class<?> ethernetManagerCls = Class.forName("android.net.EthernetManager");

                    //获取EthernetManager实例
                    Object ethManager = VfiServiceApp.getContext().getSystemService("ethernet");

                    if (ethManager == null) {
                        Log.i(TAG, "ethManager is null setEthernetStaticIp failed");
                        return;
                    }

                    Class<?> ipConfigurationCls = Class.forName("android.net.IpConfiguration");
                    Object ipConfiguration = ipConfigurationCls.newInstance();
                    //获取ipAssignment、proxySettings的枚举值
                    Map<String, Object> ipConfigurationEnum = getIpConfigurationEnum(ipConfigurationCls);
                    Field ipAssignment = ipConfigurationCls.getField("ipAssignment");
                    if (STATIC_IP.equals("0.0.0.0") || STATIC_IP.equals("0")) {
                        Log.i(TAG, "set connection type to DHCP");
                        //设置ipAssignment
                        ipAssignment.set(ipConfiguration, ipConfigurationEnum.get("IpAssignment.DHCP"));
                    } else {
                        Log.i(TAG, "set connection type to STATIC IP");
                        //设置ipAssignment
                        ipAssignment.set(ipConfiguration, ipConfigurationEnum.get("IpAssignment.STATIC"));

                        Object staticIpConfiguration = getStaticIpConfiguration(STATIC_IP, STATIC_GATEWAY, STATIC_NETMASK, STATIC_DNS1, STATIC_DNS2);
                        if (staticIpConfiguration == null) {
                            Log.i(TAG, "staticIpConfiguration is null, setWifiStaticIp failed");
                            return;
                        }
                        //设置StaticIpConfiguration
                        Field staticIpConfigurationField = ipConfigurationCls.getField("staticIpConfiguration");
                        staticIpConfigurationField.set(ipConfiguration, staticIpConfiguration);
                    }
                    //设置proxySettings
                    Field proxySettings = ipConfigurationCls.getField("proxySettings");
                    proxySettings.set(ipConfiguration, ipConfigurationEnum.get("ProxySettings.NONE"));
                    if (Build.VERSION.SDK_INT >= 28) {
                        Method setConfigurationMethod = ethernetManagerCls.getDeclaredMethod("setConfiguration", String.class, ipConfiguration.getClass());
                        //设置静态IP或DHCP
                        setConfigurationMethod.invoke(ethManager, "eth0", ipConfiguration);
                        refreshEthernet();
                    } else {
                        Method setConfigurationMethod = ethernetManagerCls.getDeclaredMethod("setConfiguration", ipConfiguration.getClass());
                        //设置静态IP或DHCP
                        setConfigurationMethod.invoke(ethManager, ipConfiguration);
                    }

                } catch (NoSuchFieldException | IllegalAccessException | InstantiationException |
                         InvocationTargetException | ClassNotFoundException |
                         NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public void refreshEthernet() {
        Class<?> emClass = null;
        try {
            emClass = Class.forName("android.net.EthernetManager");
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Object emInstance = VfiServiceApp.getContext().getSystemService("ethernet");
        Method start = null;
        Method stop = null;
        Log.d(TAG, "setEthernet:emInstance: " + emInstance);
        try {
            stop = emClass.getMethod("stop");
            stop.setAccessible(true);
            start = emClass.getMethod("start");
            start.setAccessible(true);
            stop.invoke(emInstance);
            Thread.sleep(500);
            start.invoke(emInstance);
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 获取IpConfiguration的枚举值
     */
    private Map<String, Object> getIpConfigurationEnum(Class<?> ipConfigurationCls) {
        Map<String, Object> enumMap = new HashMap<>();
        Class<?>[] enumClass = ipConfigurationCls.getDeclaredClasses();
        for (Class<?> enumC : enumClass) {
            Object[] enumConstants = enumC.getEnumConstants();
            if (enumConstants == null) continue;
            for (Object enu : enumConstants) {
                enumMap.put(enumC.getSimpleName() + "." + enu.toString(), enu);
            }
        }
        return enumMap;
    }

    @SuppressLint("PrivateApi")
    @Override
    public void setWifiStaticIp(Bundle infos) throws RemoteException {
        Log.i(TAG, "setWifiStaticIp() executed");
        if (infos == null) {
            Log.i(TAG, "infos is null");
            return;
        }
        final String STATIC_IP = infos.getString("STATIC_IP", "");
        final String STATIC_GATEWAY = infos.getString("STATIC_GATEWAY", "");
        final String STATIC_NETMASK = infos.getString("STATIC_NETMASK", "");
        final String STATIC_DNS1 = infos.getString("STATIC_DNS1", "");
        final String STATIC_DNS2 = infos.getString("STATIC_DNS2", "");
        Log.i(TAG, "use Static IP and Change IP to " + STATIC_IP);

        WifiManager wifiManager = (WifiManager) VfiServiceApp.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wifiConfig = null;
        if (wifiManager == null) {
            Log.i(TAG, "wifiManager is null, setWifiStaticIp failed");
            return;
        }
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();//得到连接的wifi网络

        @SuppressLint("MissingPermission") List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration conf : configuredNetworks) {
            if (conf.networkId == connectionInfo.getNetworkId()) {
                wifiConfig = conf;
                break;
            }
        }
        if (wifiConfig == null) {
            Log.i(TAG, "wifiConfig is null, setWifiStaticIp failed");
            return;
        }
        try {
            Class ipAssignmentCls = Class.forName("android.net.IpConfiguration$IpAssignment");
            //设置ipAssignment
            Object ipAssignment = null;
            if (STATIC_IP.equals("0.0.0.0") || STATIC_IP.equals("0")) {
                Log.i(TAG, "set connection type to DHCP");
                ipAssignment = Enum.valueOf(ipAssignmentCls, "DHCP");
            } else {
                Log.i(TAG, "set connection type to STATIC IP");
                ipAssignment = Enum.valueOf(ipAssignmentCls, "STATIC");

                Object staticIpConfiguration = getStaticIpConfiguration(STATIC_IP, STATIC_GATEWAY, STATIC_NETMASK, STATIC_DNS1, STATIC_DNS2);
                if (staticIpConfiguration == null) {
                    Log.i(TAG, "staticIpConfiguration is null, setWifiStaticIp failed");
                    return;
                }
                Method setStaticIpConfigurationMethod = wifiConfig.getClass().getDeclaredMethod("setStaticIpConfiguration", staticIpConfiguration.getClass());
                //设置静态IP
                setStaticIpConfigurationMethod.invoke(wifiConfig, staticIpConfiguration);
            }
            Method setIpAssignmentMethod = wifiConfig.getClass().getDeclaredMethod("setIpAssignment", ipAssignmentCls);
            //设置静态IP
            setIpAssignmentMethod.invoke(wifiConfig, ipAssignment);

            int netId = wifiManager.addNetwork(wifiConfig);
            wifiManager.disableNetwork(netId);
            wifiManager.enableNetwork(netId, true);
        } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException |
                 NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("PrivateApi")
    private Object getStaticIpConfiguration(final String STATIC_IP, final String STATIC_GATEWAY, final String STATIC_NETMASK, final String STATIC_DNS1, final String STATIC_DNS2) {
        try {
            Inet4Address inetAddr = getIPv4Address(STATIC_IP);
            int prefixLength = NetUtils.maskStr2InetMask(STATIC_NETMASK);
            InetAddress gatewayAddr = getIPv4Address(STATIC_GATEWAY);
            InetAddress dnsAddr = getIPv4Address(STATIC_DNS1);

            Class[] cl = new Class[]{InetAddress.class, int.class};
            Constructor cons = null;

            Class<?> clazz = Class.forName("android.net.LinkAddress");

            if (inetAddr == null || inetAddr.getAddress() == null || gatewayAddr == null || dnsAddr == null || Arrays.toString(inetAddr.getAddress()).isEmpty() || prefixLength == 0 || gatewayAddr.toString().isEmpty() || dnsAddr.toString().isEmpty() || clazz == null) {
                Log.i(TAG, "getStaticIpConfiguration failed");
                return null;
            }

            //取得所有构造函数
            try {
                cons = clazz.getConstructor(cl);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            if (cons == null) {
                Log.i(TAG, "cons is null, getStaticIpConfiguration failed");
                return null;
            }
            //给传入参数赋初值
            Object[] x = {inetAddr, prefixLength};


            Class<?> staticIpConfigurationCls = Class.forName("android.net.StaticIpConfiguration");
            //实例化StaticIpConfiguration
            Object staticIpConfiguration = null;

            staticIpConfiguration = staticIpConfigurationCls.newInstance();
            Field ipAddress = staticIpConfigurationCls.getField("ipAddress");
            Field gateway = staticIpConfigurationCls.getField("gateway");
            Field dnsServers = staticIpConfigurationCls.getField("dnsServers");
            //设置ipAddress
            ipAddress.set(staticIpConfiguration, (LinkAddress) cons.newInstance(x));
            //设置网关
            gateway.set(staticIpConfiguration, gatewayAddr);
            //设置dns
            ArrayList<InetAddress> dnsList = (ArrayList<InetAddress>) dnsServers.get(staticIpConfiguration);
            dnsList.add(dnsAddr);
            if (!STATIC_DNS2.isEmpty()) {
                dnsList.add(getIPv4Address(STATIC_DNS2));
            }
            Log.d(TAG, "chanson mStaticIpConfiguration  ====" + staticIpConfigurationCls);
            return staticIpConfiguration;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchFieldException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final int NETWORK_MODE_WCDMA_PREF = 0; /* GSM/WCDMA (WCDMA preferred) */
    private static final int NETWORK_MODE_GSM_ONLY = 1; /* GSM only */
    private static final int NETWORK_MODE_WCDMA_ONLY = 2; /* WCDMA only */
    private static final int NETWORK_MODE_GSM_UMTS = 3; /* GSM/WCDMA (auto mode, according to PRL)
                                        AVAILABLE Application Settings menu*/
    private static final int NETWORK_MODE_CDMA = 4; /* CDMA and EvDo (auto mode, according to PRL)
                                        AVAILABLE Application Settings menu*/
    private static final int NETWORK_MODE_CDMA_NO_EVDO = 5; /* CDMA only */
    private static final int NETWORK_MODE_EVDO_NO_CDMA = 6; /* EvDo only */
    private static final int NETWORK_MODE_GLOBAL = 7; /* GSM/WCDMA, CDMA, and EvDo (auto mode, according to PRL)
                                        AVAILABLE Application Settings menu*/
    private static final int NETWORK_MODE_LTE_CDMA_EVDO = 8; /* LTE, CDMA and EvDo */
    private static final int NETWORK_MODE_LTE_GSM_WCDMA = 9; /* LTE, GSM/WCDMA */
    private static final int NETWORK_MODE_LTE_CMDA_EVDO_GSM_WCDMA = 10; /* LTE, CDMA, EvDo, GSM/WCDMA */
    private static final int NETWORK_MODE_LTE_ONLY = 11; /* LTE Only mode. */
    private static final int NETWORK_MODE_LTE_WCDMA = 12; /* LTE/WCDMA */
    private static final int NETWORK_MODE_TD_SCDMA_ONLY = 13; /* TD-SCDMA only */
    private static final int NETWORK_MODE_TD_SCDMA_WCDMA = 14; /* TD-SCDMA and WCDMA */
    private static final int NETWORK_MODE_TD_SCDMA_LTE = 15; /* TD-SCDMA and LTE */
    private static final int NETWORK_MODE_TD_SCDMA_GSM = 16; /* TD-SCDMA and GSM */
    private static final int NETWORK_MODE_TD_SCDMA_GSM_LTE = 17; /* TD-SCDMA,GSM and LTE */
    private static final int NETWORK_MODE_TD_SCDMA_GSM_WCDMA = 18; /* TD-SCDMA, GSM/WCDMA */
    private static final int NETWORK_MODE_TD_SCDMA_WCDMA_LTE = 19; /* TD-SCDMA, WCDMA and LTE */
    private static final int NETWORK_MODE_TD_SCDMA_GSM_WCDMA_LTE = 20; /* TD-SCDMA, GSM/WCDMA and LTE */
    private static final int NETWORK_MODE_4G_3G_2G = 22; /* TD-SCDMA/LTE/GSM/WCDMA, CDMA, and EvDo */
    private static final int NETWORK_MODE_3G_2G = 21; /* TD-SCDMA,EvDo,CDMA,GSM/WCDMA */
    private static final int NETWORK_MODE_2G = 24; /* GSM/CDMA */

    @SuppressLint("PrivateApi")
    @Override
    public void setMobilePreferredNetworkType(String type) throws RemoteException {
        Log.i(TAG, "setMobilePreferredNetworkType() executed");
        if (Settings.System.getInt(VfiServiceApp.getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1) {
            Toast.makeText(VfiServiceApp.getContext(), "airplane mode is currently on", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "airplane mode is currently on");
            return;
        }
        if (type == null) {
            Log.i(TAG, "type is null");
            return;
        }
        int NETWORK_MODE_4G_3G_2G = 22; /* TD-SCDMA/LTE/GSM/WCDMA, CDMA, and EvDo */
        int NETWORK_MODE_3G_2G = 21; /* TD-SCDMA,EvDo,CDMA,GSM/WCDMA */
        int NETWORK_MODE_2G = 24; /* GSM/CDMA */
        int networkType = 0;
        switch (type.toUpperCase()) {
            case "2G":
                networkType = NETWORK_MODE_2G;
                break;
            case "3G":
                networkType = NETWORK_MODE_3G_2G;
                break;
            case "4G":
                networkType = NETWORK_MODE_4G_3G_2G;
                break;
        }
        Log.i(TAG, "networkType: " + networkType);
        TelephonyManager mTelephonyManager = (TelephonyManager) VfiServiceApp.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        try {
            boolean isSuccess = false;
            if (mTelephonyManager != null && networkType > 0) {
                Method method = null;
                //Android 7 以上可自动获取
                int subId = 1; //default subId
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    subId = SubscriptionManager.getDefaultDataSubscriptionId();
                    if (mTelephonyManager != null) {
                        method = mTelephonyManager.getClass().getDeclaredMethod("setPreferredNetworkType", int.class, int.class);
                        method.setAccessible(true);
                        isSuccess = (boolean) method.invoke(mTelephonyManager, subId, networkType);
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (mTelephonyManager != null) {
                        method = mTelephonyManager.getClass().getDeclaredMethod("setPreferredNetworkType", int.class);
                        method.setAccessible(true);
                        isSuccess = (boolean) method.invoke(mTelephonyManager, networkType);
                    }
                }
                Log.i(TAG, "setMobilePreferredNetworkType() isSuccess = " + isSuccess);
//                if(isSuccess){
//                    Settings.Global.putInt(VfiServiceApp.getContext().getContentResolver(),
//                            "preferred_network_mode" + subId, networkType);
//                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    @SuppressLint("PrivateApi")
    public String getMobilePreferredNetworkType() throws RemoteException {
        Log.i(TAG, "getMobilePreferredNetworkType() executed");
        if (Settings.System.getInt(VfiServiceApp.getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1) {
            Toast.makeText(VfiServiceApp.getContext(), "airplane mode is currently on", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "airplane mode is currently on");
            return null;
        }
        int type = -1;
        TelephonyManager mTelephonyManager = (TelephonyManager) VfiServiceApp.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        try {
            int subId = 1; //default subId
            Method method = null;
            //Android 7 以上可自动获取
            if (mTelephonyManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    subId = SubscriptionManager.getDefaultDataSubscriptionId();
                    method = mTelephonyManager.getClass().getDeclaredMethod("getPreferredNetworkType", int.class);
                    method.setAccessible(true);
                    type = (int) method.invoke(mTelephonyManager, subId);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    method = mTelephonyManager.getClass().getDeclaredMethod("getPreferredNetworkType");
                    method.setAccessible(true);
                    type = (int) method.invoke(mTelephonyManager);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (type == -1) {
            return null;
        }
        String networkType = null;
        switch (type) {
            case NETWORK_MODE_4G_3G_2G:
            case NETWORK_MODE_TD_SCDMA_GSM_WCDMA_LTE:
            case NETWORK_MODE_TD_SCDMA_WCDMA_LTE:
            case NETWORK_MODE_TD_SCDMA_GSM_LTE:
            case NETWORK_MODE_TD_SCDMA_LTE:
            case NETWORK_MODE_LTE_WCDMA:
            case NETWORK_MODE_LTE_ONLY:
            case NETWORK_MODE_LTE_CMDA_EVDO_GSM_WCDMA:
            case NETWORK_MODE_LTE_GSM_WCDMA:
            case NETWORK_MODE_LTE_CDMA_EVDO:
                networkType = "4G";
                break;
            case NETWORK_MODE_WCDMA_PREF:
            case NETWORK_MODE_WCDMA_ONLY:
            case NETWORK_MODE_GSM_UMTS:
            case NETWORK_MODE_CDMA:
            case NETWORK_MODE_EVDO_NO_CDMA:
            case NETWORK_MODE_GLOBAL:
            case NETWORK_MODE_TD_SCDMA_ONLY:
            case NETWORK_MODE_TD_SCDMA_WCDMA:
            case NETWORK_MODE_TD_SCDMA_GSM:
            case NETWORK_MODE_TD_SCDMA_GSM_WCDMA:
            case NETWORK_MODE_3G_2G:
                networkType = "3G";
                break;
            case NETWORK_MODE_2G:
            case NETWORK_MODE_GSM_ONLY:
            case NETWORK_MODE_CDMA_NO_EVDO:
                networkType = "2G";
                break;
        }
        return networkType;
    }

    @Override
    public int addNetwork(Bundle bundle) {
        String SSID = bundle.getString("SSID");
        String password = bundle.getString("password");
        int type = bundle.getInt("type", 3);
        if (TextUtils.isEmpty(SSID) || (type != 1 && TextUtils.isEmpty(password))) {
            Log.i(TAG, "addNetwork : params is error");
            return -1;
        }
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        WifiManager wifiManager;
        wifiManager = (WifiManager) VfiServiceApp.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        for (WifiConfiguration tempConfig : wifiManager.getConfiguredNetworks()) {
            if (tempConfig.SSID.equals("\"" + SSID + "\"")) {
                wifiManager.removeNetwork(tempConfig.networkId);
            }
        }

        if (type == 1) // WIFICIPHER_NOPASS
        {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (type == 2) // WIFICIPHER_WEP
        {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (type == 3) // WIFICIPHER_WPA
        {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return wifiManager.addNetwork(config);
    }

    @Override
    public boolean connectWifi(String SSID) {
        WifiConfiguration tempConfig = null;
        WifiManager wifiManager;
        wifiManager = (WifiManager) VfiServiceApp.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        for (WifiConfiguration config : wifiManager.getConfiguredNetworks()) {
            if (config.SSID.equals("\"" + SSID + "\"")) {
                tempConfig = config;
            }
        }
        if (tempConfig != null) {
            for (WifiConfiguration c : wifiManager.getConfiguredNetworks()) {
                wifiManager.disableNetwork(c.networkId);
            }
            return wifiManager.enableNetwork(tempConfig.networkId, true);
        } else {
            Log.i(TAG, "Wifi SSID is not exist.");
            return false;
        }
    }

    @Override
    public void addNetworkAllowedList(String zipPath, final IAddNetworkAllowedListObserver observer, boolean isReboot) {
        Log.i(TAG, "addNetworkAllowedList;  zipPath:" + zipPath + "; isReboot:" + isReboot);
        ISystemSettings systemSettings = SdkApiHolder.getInstance().getSystemSettings();
        systemSettings.init(VfiServiceApp.getContext());
        systemSettings.downloadNetworkWhitelisting(zipPath, new NetworkWhitelistingListener() {
            @Override
            public void onDownloadResult(int i) {
                Log.i(TAG, "onDownloadResult ret:" + i);
                try {
                    observer.onResult(i);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public int setProxy(String proxy, Bundle extend) throws RemoteException {
        Log.i(TAG, "setProxy proxy:" + proxy);

        if (TextUtils.isEmpty(proxy) || ":0".equals(proxy)) {
            ConnectivityManager cm = (ConnectivityManager) VfiServiceApp.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            try {
                Method methodSetGlobalProxy = cm.getClass().getDeclaredMethod("setGlobalProxy", ProxyInfo.class);
                methodSetGlobalProxy.setAccessible(true);
                methodSetGlobalProxy.invoke(cm, (ProxyInfo) null);
                return 0;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                return -1;
            }
        } else {
            if (proxy.contains(":")) {
                String[] splitProxy = proxy.split(":");
                if (splitProxy.length == 2) {
                    String host = splitProxy[0];
                    int port = Integer.parseInt(splitProxy[1]);
                    ConnectivityManager cm = (ConnectivityManager) VfiServiceApp.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    try {
                        ProxyInfo proxyInfo = ProxyInfo.buildDirectProxy(host, port);
                        Method methodSetGlobalProxy = cm.getClass().getDeclaredMethod("setGlobalProxy", ProxyInfo.class);
                        methodSetGlobalProxy.setAccessible(true);
                        methodSetGlobalProxy.invoke(cm, proxyInfo);
                        return 0;
                    } catch (NoSuchMethodException | IllegalAccessException |
                             InvocationTargetException e) {
                        e.printStackTrace();
                        return -1;
                    }
                } else {
                    Log.w(TAG, "proxy format error,length =" + splitProxy.length);
                }
            } else {
                Log.w(TAG, "proxy format error");
                return -1;
            }
        }
//        try {
//            execCommand("settings put global http_proxy " + proxy);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return -1;
//        }
        return 0;
    }

    @Override
    public String getMacAddress(int type) throws RemoteException {
        if (type == 0) {
            String strMacAddr = null;
            try {
                InetAddress ip = getLocalInetAddress();
                byte[] b = NetworkInterface.getByInetAddress(ip).getHardwareAddress();
                StringBuffer buffer = new StringBuffer();
                for (int i = 0; i < b.length; i++) {
                    if (i != 0) {
                        buffer.append(':');
                    }
                    String str = Integer.toHexString(b[i] & 0xFF);
                    buffer.append(str.length() == 1 ? 0 + str : str);
                }
                strMacAddr = buffer.toString().toUpperCase();
            } catch (Exception e) {
            }
            return strMacAddr;

        } else {
            String s = null;
            NetworkInterface nif = null;
            try {
                nif = NetworkInterface.getByName("eth0");
            } catch (SocketException e) {
                e.printStackTrace();
            }
            if (nif != null) {
                byte[] macBytes = new byte[0];
                try {
                    macBytes = nif.getHardwareAddress();
                    s = byteHexString(macBytes);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "s1 :" + s);
            return s;

        }
    }

    /**
     * get Wifi proxy enable/disable, only support on V4 rom and need GE 202211291734
     *
     * @return
     * @throws RemoteException
     */
    @Override
    public boolean getWifiProxyState() throws RemoteException {
        Log.d(TAG, "getWifiProxyState execute");
        ISystemSettings systemSettings = SdkApiHolder.getInstance().getSystemSettings();
        String res = systemSettings.getWiFiProxyState();
        Log.d(TAG, "getWifiProxyState= " + res);
        if (res.toLowerCase().equals("false")) return false;
        return true;
    }

    /**
     * set Wifi proxy feature enable/disable, need reboot device, only support on V4 rom and need GE 202211291734
     *
     * @param state
     * @throws RemoteException
     */
    @Override
    public void setWifiProxyState(boolean state) throws RemoteException {
        Log.d(TAG, "setWifiProxyState execute, state=" + state);
        ISystemSettings systemSettings = SdkApiHolder.getInstance().getSystemSettings();
        systemSettings.setWiFiProxyState(state);
    }

    @Override
    public void setUnsafeWifiStatus(boolean show, boolean connectable) throws RemoteException {
        Log.d(TAG, "setUnsafeWifiStatus execute, show=" + show + ", connectable=" + connectable);
        ISystemSettings systemSettings = SdkApiHolder.getInstance().getSystemSettings();
        systemSettings.setUnsafeWifiStatus(show, connectable);
    }

    private InetAddress getLocalInetAddress() {
        InetAddress ip = null;
        try {
            Enumeration<NetworkInterface> en_netInterface = NetworkInterface.getNetworkInterfaces();
            while (en_netInterface.hasMoreElements()) {// 是否还有元素
                NetworkInterface ni = (NetworkInterface) en_netInterface.nextElement();// 得到下一个元素
                Enumeration<InetAddress> en_ip = ni.getInetAddresses();// 得到一个ip地址的列举
                while (en_ip.hasMoreElements()) {
                    ip = en_ip.nextElement();
                    if (!ip.isLoopbackAddress() && ip.getHostAddress().indexOf(":") == -1) break;
                    else ip = null;
                }
                if (ip != null) {
                    break;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ip;
    }

    /*
     * 字节数组转16进制字符串
     */
    public String byteHexString(byte[] array) {
        StringBuilder builder = new StringBuilder();

        for (byte b : array) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            builder.append(hex);
        }
        String origin = builder.toString().toUpperCase();
        return origin.replaceAll("(.{2})", ":$1").substring(1);
    }


//    private void execCommand(String cmd) {
//        try {
//            Runtime.getRuntime().exec(cmd);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
