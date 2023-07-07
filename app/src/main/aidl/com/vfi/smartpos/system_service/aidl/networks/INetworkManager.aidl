package com.vfi.smartpos.system_service.aidl.networks;

import com.vfi.smartpos.system_service.aidl.IAddNetworkAllowedListObserver;

// Declare any non-default types here with import statements
import android.os.Bundle;
/*
 *Exposed APIs for Application to control terminal networks
 *
*/
interface INetworkManager {

    /**
     * Set network type of terminal :
     * @param mode status code:
        7 = Global
        6 = EvDo only
        5 = CDMA w/o EvDo
        4 = CDMA / EvDo auto
        3 = GSM / WCDMA auto
        2 = WCDMA only
        1 = GSM only
        0 = GSM / WCDMA preferred
        -1 = acquiring failed
     * @since 1.0.0
     */
    void setNetworkType(int mode);

    /**
     * Get current network type of terminal
     * @return networkType
     * @since 1.0.0
     */
    int getNetworkType();

    /**
     * Control WiFi
     * @param state true: open Wifi false: close Wifi
     * @since 1.0.0
     */
    void enableWifi(boolean state);

    /**
     * Control Airplane mode
     * @param state true: open AirplayMode false: close AirplayMode
     * @since 1.0.0
     */
    void enableAirplayMode(boolean state);

    /**
    * Add a new APN configuration and set it to default <br/>
    * The auth type column can have 4 values: 0 (None), 1 (PAP), 2 (CHAP)
      3 (PAP or CHAP). To avoid breaking compatibility, with already working
      APNs, the unset value (-1) will be used. If the value is -1.
      the authentication will default to 0 (if no user / password) is specified
      or to 3. Currently, there have been no reported problems with
      pre-configured APNs and hence it is set to -1 for them. Similarly,
      if the user, has added a new APN, we set the authentication type
      to -1.
    * @param infos add APN infos in bundle, example:
    * <p><pre>{@code
         Bundle infos = new Bundle();
         infos.putString("name", "test01");
         infos.putString("apn", "test01");
         infos.putString("authtype", "-1");
         //numeric is mcc and mnc
         infos.putString("numeric", "46002");
    //     infos.putString("mcc", "460");
    //     infos.putString("mnc", "02");
         infos.putString("proxy","");
         infos.putString("port","");
         infos.putString("mmsproxy", "");
         infos.putString("mmsport", "");
         infos.putString("user", "");
         infos.putString("server", "");
         infos.putString("password","");
         infos.putString("mmsc", "");
         infos.putString("current", "1");
         infos.putString("carrier_enabled", "1");
         infos.putString("protocol", "IP");
         infos.putString("roaming_protocol", "IP");
         infos.putString("bearer", "0");
         infos.putString("max_conns", "0");
         infos.putString("max_conns_time", "0");
         infos.putString("modem_cognitive", "0");
         infos.putString("localized_name", "");
         infos.putString("mvno_match_data", "");
         infos.putString("mvno_type", "");
         infos.putString("profile_id", "0");
         infos.putString("read_only", "0");
         infos.putString("sub_id", "1");
         infos.putString("type", "");
         // "SLOT"   // Add by Simon on version 1.6.0.3
         // SLOT: 1 or 2 for SIM card in slot 1 or 2.
         // using the active slot as default if there is no SLOT setting & no "fixed_numeric" setting
         infos.putString("SLOT", "1");
         // "fixed_numeric"  // add by Simon on version 1.6.0.3
         // fixed the numeric to fixed_numeric for specific SIM card
         // using the "SLOT" if there is no "fixed_numeric" setting
         infos.putString("fixed_numeric", "46002");
    * }
    * </pre>
    * @retrun result
    * @since 1.0.0
    */
    int setAPN(in Bundle infos);

    /**
     * Control Mobile data
     * @param state true: open MobileData false: close MobileData
     * @since 1.0.0
     */
    void enableMobileData( boolean state);

    /**
     * Get current selected APN configurations
     * @return current selected APN configurations
     */
    Bundle getSelectedApnInfo();

    /**
     * select which to use for mobile data
     * @param slotIdx 1 or 2 to use for mobile data
     * @return result
     * @since 1.7.0.1
     */
    int selectMobileDataOnSlot( int slotIdx);

    /**
    * Is enable MultiNetwork
    * @return true-enable multiNetwork false-disable multiNetwork
    * @since 1.8.0
    */
    boolean isMultiNetwork();

    /**
    * Control MultiNetwork
    * @param enable true: able multiNetwork false: disable multiNetwork
    * @since 1.8.0
    */
    void setMultiNetwork(boolean enable);

    /**
    * Get the prefer of MultiNetwork
    * @return the prefer of MultiNetwork
    * @since 1.8.0
    */
    String getMultiNetworkPrefer();

    /**
    * set the prefer of MultiNetwork
    * @param prefer the prefer to set
    * <p><pre>{@code
             public static final String TRANSPORT_WIFI_ETHERNET_CELLULAR = "wifi,ethernet,cellular";
             public static final String TRANSPORT_WIFI_CELLULAR_ETHERNET = "wifi,cellular,ethernet";
             public static final String TRANSPORT_CELLULAR_WIFI_ETHERNET = "cellular,wifi,ethernet";
             public static final String TRANSPORT_CELLULAR_ETHERNET_WIFI = "cellular,ethernet,wifi";
             public static final String TRANSPORT_ETHERNET_CELLULAR_WIFI = "ethernet,cellular,wifi";
             public static final String TRANSPORT_ETHERNET_WIFI_CELLULAR = "ethernet,wifi,cellular";
    * }
    * </pre>
    * @return result true for success, false for failure
    * @since 1.8.0
    */
    boolean setMultiNetworkPrefer(String prefer);

    /**
     * set ethernet static ip <br/>
     * set STATIC_IP 0.0.0.0 or 0 to change connection type to DHCP
     * @param infos - add static ip infos in bundle, example:
     * <p><pre>{@code
             Bundle infos = new Bundle();
             infos.putString("STATIC_IP", "192.168.1.1");
             infos.putString("STATIC_GATEWAY", "192.168.1.1");
             infos.putString("STATIC_NETMASK", "255.255.255.0");
             infos.putString("STATIC_DNS1", "192.168.1.1");
             infos.putString("STATIC_DNS2", "192.168.1.1");
     * }
     * </pre>
     * @since 1.8.1
     */
    void setEthernetStaticIp(in Bundle bundle);

    /**
     * set wifi static ip <br/>
     * set STATIC_IP 0.0.0.0 or 0 to change connection type to DHCP
     * <p><pre>{@code
             Bundle infos = new Bundle();
             infos.putString("STATIC_IP", "192.168.1.1");
             infos.putString("STATIC_GATEWAY", "192.168.1.1");
             infos.putString("STATIC_NETMASK", "255.255.255.0");
             infos.putString("STATIC_DNS1", "192.168.1.1");
             infos.putString("STATIC_DNS2", "192.168.1.1");
     * }
     * </pre>
     * @since 1.8.5
     */
    void setWifiStaticIp(in Bundle bundle);

    /**
    * set mobile preferred network type, only the current SIM card
    * @param type 2G:only use 2G
                  3G:use 2G/3G
                  4G:use 2G/3G/4G
    * @since 1.8.4
    */
   void setMobilePreferredNetworkType(String type);

   /**
   * get mobile preferred network type, only the current SIM card
   * @return result 2G 3G 4G for success, null for failure
   * @since 1.8.4
   */
  String getMobilePreferredNetworkType();

    /**
    * Delete exist APN
    * @param apn : which you set-in setAPN()
    * @since 1.0.0
    */
    int deleteAPN(in String apn);

    /**
    * set Data Roaming state
    * @param enabled : true-enable dataRoaming; false-disable dataRoaming
    * @param slotId : the index of sim card slot
    * @since 1.0.0
    */
    int setDataRoamingEnabled(boolean enabled, int slotId);

    /**
    * Add a new Network
    * @param Bundle :
       paramName | type   | description
       ----------------------------------
        SSID     | String | network name
        password | String | password
        type     | int    | security type : 1-NONE 2-WEP 3-WPA(default)
       -----------------------------------
    * @return the ID of the newly created network description. This is used in
    *         other operations to specified the network to be acted upon.
    *         Returns {@code -1} on failure.
    * @since 1.8.12
    */
    int addNetwork(in Bundle bundle);

    /**
    * Connect to wifi
    * @param SSID : network name
    * @reture true-success, false-failed
    * @since 1.8.12
    */
    boolean connectWifi(String SSID);

	/**
	 * Add network allowed list to terminal
	 * A zip package will be used, and it should be signed by uKey.
	 * Only one file should be in the zip, and it's name must be "system.allowed_list"
	 * Notice: Terminal will reboot automatically after it done
	 *
	 * The structure of the zip is as below:
	 * ..zip
	 *   -system.allowed_list
	 *   -META-INF/
	 *     -singature files
	 *
	 * The example of "system.allowed_list":
	 * [uses-ip-1]
     * ip=192.168.100.1
     * [uses-ip-2]
     * ip=www.baidu.com
     * [uses-ip-3]
     * ip=http://192.262.2.2/test
     *
	 * @param zipPath - path of zip package
	 * @param observer - callback handler.
	 * @param isReboot - Reserved parameter, not used
	 * @see IAddNetworkAllowedListObserver
	 * @since 1.11.3
	 */
    void addNetworkAllowedList(String zipPath, IAddNetworkAllowedListObserver observer, boolean isReboot);

     /**
        * Connect to wifi
        * @param proxy :  use format ip:host
        * @param extend : for future use
        * @since 1.11.7
        */
    int setProxy(String proxy, in Bundle extend);

    /**
             *
             * @param type 0-WiFI 1-ETH
             * @return MAC address
             * @throws RemoteException
             */
    String getMacAddress(int type);

    /**
     * get Wifi proxy enable/disable, only support on V4 rom and need GE 202211291734
     * @return
     * @throws RemoteException
     */
    boolean getWifiProxyState();

    /**
     * set Wifi proxy feature enable/disable, need reboot device, only support on V4 rom and need GE 202211291734
     * @param state
     * @throws RemoteException
     */
    void setWifiProxyState(boolean state);

    /**
    * Set the non-secure wifi status
    * Minimum supported ROM version(V2:V1.1.1.202209141651,V4:3A.1.512.202212092051)
    * Notice: If this API is called, it needs to be restarted and take effect
    *
    * @param show: Whether non-secure wifi is visible
    * @param connectable: Whether non-secure wifi can be connected, connected if true
    */
    void setUnsafeWifiStatus(boolean show, boolean connectable);

}
