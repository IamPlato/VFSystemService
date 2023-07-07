package com.vfi.smartpos.system_service.util;
/*
 *  author: Derrick
 *  Time: 2019/7/1 16:59
 */

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.vfi.smartpos.system_service.R;
import com.vfi.smartpos.system_service.VfiServiceApp;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.content.Context.TELEPHONY_SERVICE;

public class APN {

    //取得全部的APN列表：
    public static final Uri APN_URI = Uri.parse("content://telephony/carriers");
    //取得当前设置的APN：
    public static final Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");
    private static final String TAG = "APNManager";

    private static final String PREF_FILE_APN = "preferred-apn";
    private static final String COLUMN_APN_ID = "apn_id";


    private Context context = VfiServiceApp.getContext();
    private String sNotSet = "";
    //    private SQLiteDatabase mDatabase = ApnDatabase.getApnDatabase().getWritableDatabase();
    private TelephonyManager mTelephonyManager = (TelephonyManager) VfiServiceApp.getContext().getSystemService(TELEPHONY_SERVICE);
    ;
    private Uri mUri;
    private Cursor mCursor;
    private Context mContext = VfiServiceApp.getContext();

    /**
     * Standard projection for the interesting columns of a normal note.
     */
    private static final String[] sProjection = new String[]{Telephony.Carriers._ID,     // 0
            Telephony.Carriers.NAME,    // 1
            Telephony.Carriers.APN,     // 2
            Telephony.Carriers.PROXY,   // 3
            Telephony.Carriers.PORT,    // 4
            Telephony.Carriers.USER,    // 5
            Telephony.Carriers.SERVER,  // 6
            Telephony.Carriers.PASSWORD, // 7
            Telephony.Carriers.MMSC, // 8
            Telephony.Carriers.MCC, // 9
            Telephony.Carriers.MNC, // 10
            Telephony.Carriers.NUMERIC, // 11
            Telephony.Carriers.MMSPROXY,// 12
            Telephony.Carriers.MMSPORT, // 13
            Telephony.Carriers.AUTH_TYPE, // 14
            Telephony.Carriers.TYPE, // 15
            Telephony.Carriers.PROTOCOL, // 16
            Telephony.Carriers.CARRIER_ENABLED, // 17
            Telephony.Carriers.BEARER, // 18
            //Telephony.Carriers.BEARER_BITMASK, // 19
            Telephony.Carriers.ROAMING_PROTOCOL, // 20
            Telephony.Carriers.MVNO_TYPE,   // 21
            Telephony.Carriers.MVNO_MATCH_DATA,  // 22
            //Telephony.Carriers.EDITED   // 23
    };

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int PROXY_INDEX = 3;
    private static final int PORT_INDEX = 4;
    private static final int USER_INDEX = 5;
    private static final int SERVER_INDEX = 6;
    private static final int PASSWORD_INDEX = 7;
    private static final int MMSC_INDEX = 8;
    private static final int MCC_INDEX = 9;
    private static final int MNC_INDEX = 10;
    private static final int MMSPROXY_INDEX = 12;
    private static final int MMSPORT_INDEX = 13;
    private static final int AUTH_TYPE_INDEX = 14;
    private static final int TYPE_INDEX = 15;
    private static final int PROTOCOL_INDEX = 16;
    private static final int CARRIER_ENABLED_INDEX = 17;
    private static final int BEARER_INDEX = 18;
    // private static final int BEARER_BITMASK_INDEX = 19;
    private static final int ROAMING_PROTOCOL_INDEX = 20;
    private static final int MVNO_TYPE_INDEX = 21;
//    private static final int MVNO_MATCH_DATA_INDEX = 22;
    // private static final int EDITED_INDEX = 23;


    public int setAPN(Bundle infos) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return validateAndSave(infos);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String apn = infos.getString("apn");
            int checkAPN = isAPNExist(apn);
            if (checkAPN == -1) {
                int newAPNId = addAPN(infos);
                if (newAPNId == -1) {
                    Log.d(TAG, "Name & APN cannot be null !");
                    return -1;
                }
                Log.d(TAG, "The new APN id = " + newAPNId);
                if (setDefaultAPN(newAPNId)) {
                    Log.d(TAG, "Set Current APN to " + newAPNId);
                    return newAPNId;
                } else {
                    Log.d(TAG, "Set Current APN to new APN failed");
                    return -1;
                }
            } else {
                deleteAPN(apn);
                int newAPNID = addAPN(infos);
                setDefaultAPN(newAPNID);
                Log.d(TAG, "This APN is already exist, id = " + newAPNID);
                return 1;
            }
        }
        return -1;
//        if (validateAndSave(infos)){
//            return 0;
//        }else {
//            return -1;
//        }
    }

    private SubscriptionInfo GetNumericBySlot(int slot) {
        String numberic = "";
        //获取SubscriptionManager对象
        SubscriptionManager sm = SubscriptionManager.from(context);
        @SuppressLint("MissingPermission") List<SubscriptionInfo> list = sm.getActiveSubscriptionInfoList();//获取所有sim卡的信息集合
        if (list != null && list.size() > 0) {
            int subscriptionId = list.get(0).getSubscriptionId();
            for (int i = 0; i < list.size(); i++) {
                SubscriptionInfo info1 = list.get(i);
                Log.d("TAG", "Get, " + info1.getDisplayName() + "slot:" + info1.getSimSlotIndex());
                if (slot == info1.getSimSlotIndex()) {
                    numberic = String.format("%03d%02d", info1.getMcc(), info1.getMnc());
                    Log.d(TAG, "numberic: " + numberic);
                    return info1;
//                    break;
                }
            }
        }
        return null;
    }


    /**
     * Copied from Rom, will try to replace current code in the future.
     * Check the key fields' validity and save if valid.
     *
     * @return true if the data was saved
     */
    @SuppressLint("MissingPermission")
    public int validateAndSave(Bundle infos) {
//        mUri = Telephony.Carriers.CONTENT_URI;
        if (Build.VERSION.SDK_INT < 29) {
            mUri = mContext.getContentResolver().insert(Telephony.Carriers.CONTENT_URI, new ContentValues());
            if (mUri == null) {
                Log.w(TAG, "Failed to insert new telephony provider");
                return -1;
            }
        } else {
            mUri = Telephony.Carriers.CONTENT_URI;
        }

        mCursor = mContext.getContentResolver().query(mUri, sProjection, null, null, null);
        mCursor.moveToFirst();

        String name = checkNotSet(infos.getString("name"));
        String apn = checkNotSet(infos.getString("apn"));

//        // If no name & APN, return -1 directly
//        if (name == null || name.equals("") || apn == null || apn.equals("")) {
//            Log.d(TAG, "no name & APN, return -1 directly");
//            return -1;
//        }

        String proxy = mCursor.getString(PROXY_INDEX);
        String port = mCursor.getString(PORT_INDEX);
        String user;
        String password;
        if (TextUtils.isEmpty(infos.getString("user"))) {
            user = mCursor.getString(USER_INDEX);
        } else {
            user = infos.getString("user");
        }
        String server = mCursor.getString(SERVER_INDEX);
        if (TextUtils.isEmpty(infos.getString("password"))) {
            password = mCursor.getString(PASSWORD_INDEX);
        } else {
            password = infos.getString("password");
        }
        String mmsproxy = mCursor.getString(MMSPROXY_INDEX);
        String mmsport = mCursor.getString(MMSPORT_INDEX);
        String mmsc = mCursor.getString(MMSC_INDEX);
        String mCurMnc = mCursor.getString(MCC_INDEX);
        String mCurMcc = mCursor.getString(MNC_INDEX);
        String type = mCursor.getString(TYPE_INDEX);
        // "SLOT"   // Add by Simon on version 1.6.0.2
        // SLOT: 1 or 2 for SIM card in slot 1 or 2.
        // using the slot 1 as default if there is no "fixed_numeric" setting
        String slot = checkNotSet(infos.getString("SLOT"));

        // "fixed_numeric"  // add by Simon on version 1.6.0.2
        // fixed the numeric to fixed_numeric for specific SIM card
        // using the "SLOT" if there is no "fixed_numeric" setting
        String fixed_numeric = checkNotSet(infos.getString("fixed_numeric"));

        String numeric = "";
        String subID = "";
        if (fixed_numeric.length() > 0) {
            numeric = fixed_numeric;
        } else {
            if (slot.equals("2")) {
                SubscriptionInfo subscriptionInfo = GetNumericBySlot(1);
                if (subscriptionInfo != null) {
                    numeric = String.format("%03d%02d", subscriptionInfo.getMcc(), subscriptionInfo.getMnc());
                    Log.d(TAG, "numberic: " + numeric);
                    subID = Integer.toString(subscriptionInfo.getSubscriptionId());
                }
//                numeric = GetNumericBySlot(1);
            } else if (slot.equals("1")) {
                // slot 1
                SubscriptionInfo subscriptionInfo = GetNumericBySlot(0);
                if (subscriptionInfo != null) {
                    numeric = String.format("%03d%02d", subscriptionInfo.getMcc(), subscriptionInfo.getMnc());
                    Log.d(TAG, "numberic: " + numeric);
                    subID = Integer.toString(subscriptionInfo.getSubscriptionId());
                }

//                numeric = GetNumericBySlot(0);
            } else {
                numeric = mTelephonyManager.getSimOperator();   // 这个api是获取活动状况的卡槽，不一定是卡槽1
                subID = mTelephonyManager.getSubscriberId();
            }
            if (numeric.length() == 0) {
                return -1;
            }
        }
        Log.d(TAG, "validateAndSave: fixed numeric = " + numeric + "  subId = " + subID);
        // MCC is first 3 chars and then in 2 - 3 chars of MNC
        if (numeric != null && numeric.length() > 4) {
            // Country code
            String mcc = numeric.substring(0, 3);
            // Network code
            String mnc = numeric.substring(3);
            // Auto populate MNC and MCC for new entries, based on what SIM reports
            mCurMnc = mnc;
            mCurMcc = mcc;
        }
        String authType = infos.getString("authtype", "0");

        String protocol = mCursor.getString(PROTOCOL_INDEX);
        String roamingProtocol = mCursor.getString(ROAMING_PROTOCOL_INDEX);
        int carrierEnabled = mCursor.getInt(CARRIER_ENABLED_INDEX);
        int bearer = mCursor.getInt(BEARER_INDEX);

//        HashSet<String> bearers = new HashSet<String>();
//        int bearerBitmask = mCursor.getInt(BEARER_BITMASK_INDEX);
//        if (bearerBitmask == 0) {
//            if (bearer == 0) {
//                bearers.add("" + 0);
//            }
//        } else {
//            int i = 1;
//            while (bearerBitmask != 0) {
//                if ((bearerBitmask & 1) == 1) {
//                    bearers.add("" + i);
//                }
//                bearerBitmask >>= 1;
//                i++;
//            }
//        }
//
//        if (bearer != 0 && bearers.contains("" + bearer) == false) {
//            // add bearer to bearers
//            bearers.add("" + bearer);
//        }

        String mvnoType = mCursor.getString(MVNO_TYPE_INDEX);
//        String mvnoMatchDataStr = mCursor.getString(MVNO_MATCH_DATA_INDEX);

        if (!mCursor.moveToFirst()) {
            Log.w(TAG, "Could not go to the first row in the Cursor when saving data.");
            return -1;
        }

        // If it's a new APN and a name or apn haven't been entered, then erase the entry
        if (name.length() < 1 && apn.length() < 1) {
            mContext.getContentResolver().delete(mUri, null, null);
            return -1;
        }

        ContentValues values = new ContentValues();

        // Add a dummy name "Untitled", if the user exits the screen without adding a name but
        // entered other information worth keeping.
        values.put(Telephony.Carriers.NAME, name.length() < 1 ? "defaultAPN" : name);
        values.put(Telephony.Carriers.APN, apn);
        values.put(Telephony.Carriers.PROXY, checkNotSet(proxy));
        values.put(Telephony.Carriers.PORT, checkNotSet(port));
        values.put(Telephony.Carriers.MMSPROXY, checkNotSet(mmsproxy));
        values.put(Telephony.Carriers.MMSPORT, checkNotSet(mmsport));
        values.put(Telephony.Carriers.USER, checkNotSet(user));
        values.put(Telephony.Carriers.SERVER, checkNotSet(server));
        values.put(Telephony.Carriers.PASSWORD, checkNotSet(password));
        values.put(Telephony.Carriers.MMSC, checkNotSet(mmsc));


//        if (authVal != null) {
//            values.put(Telephony.Carriers.AUTH_TYPE, Integer.parseInt(authVal));
//        }
        values.put(Telephony.Carriers.AUTH_TYPE, authType);

        values.put(Telephony.Carriers.PROTOCOL, protocol);
        values.put(Telephony.Carriers.ROAMING_PROTOCOL, roamingProtocol);

        values.put(Telephony.Carriers.TYPE, type);

        values.put(Telephony.Carriers.MCC, mCurMcc);
        values.put(Telephony.Carriers.MNC, mCurMnc);

        values.put(Telephony.Carriers.NUMERIC, mCurMcc + mCurMnc);

        values.put(Telephony.Carriers.BEARER, bearer);

        values.put(Telephony.Carriers.MVNO_TYPE, mvnoType);
//        values.put(Telephony.Carriers.MVNO_MATCH_DATA, mvnoMatchDataStr);

        values.put(Telephony.Carriers.CARRIER_ENABLED, carrierEnabled);

        //solve read-only issue
        values.put("EDITED", 1);
//        values.put(Telephony.Carriers.EDITED_STATUS, Telephony.Carriers.USER_EDITED);

        if (Build.VERSION.SDK_INT < 29) {
            mContext.getContentResolver().update(mUri, values, null, null);
        } else {
            mContext.getContentResolver().insert(mUri, values);
        }

        mCursor = mContext.getContentResolver().query(mUri, new String[]{Telephony.Carriers._ID, Telephony.Carriers.APN, Telephony.Carriers.NAME}, "apn = '" + apn + "' and name = '" + name + "'", null, null);
        mCursor.moveToFirst();

        String id = mCursor.getString(ID_INDEX);
        Log.d(TAG, "validateAndSave: new added apn id = " + id);

//        setSelectedApnKey(id);
//        if (TextUtils.isEmpty(id)) {
//            return -1;
//        }
//        setDefaultAPN(Integer.parseInt(id));

        if (Build.VERSION.SDK_INT < 29) {
            setPreferredApn(id, subID);
        }else{
            if (TextUtils.isEmpty(id)) {
                return -1;
            }
            setDefaultAPN(Integer.parseInt(id));
        }


        return 1;
    }

    private void setPreferredApn(String pos, String subId) {
        if (pos.length() == 0 || subId.length() == 0) {
            Log.e(TAG, "Invalid index:" + pos + ", " + subId);
            return;
        }
        Uri PREFERAPN_NO_UPDATE_URI_USING_SUBID = Uri.parse("content://telephony/carriers/preferapn_no_update/subId/");
//        if (!mCanSetPreferApn) {
//            log("setPreferredApn: X !canSEtPreferApn");
//            return;
//        }

        // 由于id只有一个，所以首先要删除过去的id
//        String subId = Long.toString(mPhone.getSubId());
        // 注意PREFERAPN_NO_UPDATE_URI_USING_SUBID中包含了NO_UPDATE，说明此修改无需将数据库的变化通知相应的监听器
        Uri uri = Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID, subId);
        Log.d(TAG, "setPreferredApn: delete" + uri);
        ContentResolver resolver = mContext.getContentResolver();
        // 删除数据库中的数据
        resolver.delete(uri, null, null);

//        if (pos >= 0)
        {
            // 如果pos为-1，则不执行插入，否则插入新的preferredapn，pos即为id
            Log.d(TAG, "setPreferredApn: insert");
            ContentValues values = new ContentValues();
            values.put("apn_id", pos); // APN_ID = "apn_id"
            resolver.insert(uri, values);
        }
    }

    private void setSelectedApnKey(String id) {

    }

    private void setSelectedApnKey2(String id) {

        ContentResolver resolver = mContext.getContentResolver();

        ContentValues values = new ContentValues();
        values.put("apn_id", id);
        resolver.update(PREFERRED_APN_URI, values, null, null);


    }

    private String checkNotSet(String value) {
        if (value == null || value.equals(sNotSet)) {
            return "";
        } else {
            return value;
        }
    }

    // If exist, return the id of new apn. otherwise return -1
    private int isAPNExist(String apn) {
        int id = -1;
        ContentResolver resolver = context.getContentResolver();
        Cursor c = resolver.query(APN_URI, new String[]{"_id", "name", "apn"}, "apn like '%" + apn + "%'", null, null);

        if (c != null && c.moveToNext()) {
            int idIdx = c.getColumnIndex("_id");
            c.moveToFirst();
            id = c.getShort(idIdx);
            return id;
        } else {
            return id;
        }
    }

    public int deleteAPN(String apn) {
        ContentResolver resolver = context.getContentResolver();
        int result = resolver.delete(APN_URI, "apn = ?", new String[]{apn});
        Log.d(TAG, "apn = " + apn + "   result = " + result);
        return result;
    }

    private int addAPN(Bundle infos) {

        //Get current selected data 
        // TODO: 2020/4/30 Cannot get current configs 
//        Bundle default_APN = getSelectedAPNConfigs();
        Bundle default_APN = new Bundle();

        // Get parameter data
        String name = infos.getString("name");
        String apn = infos.getString("apn");

        // If no name & APN, return -1 directly
        if (name == null || name.equals("") || apn == null || apn.equals("")) {
            Log.d(TAG, "no name & APN, return -1 directly");
            return -1;
        }

        // Set fields from pramas, if null, from selected configs
        String authtype = infos.getString("authtype", default_APN.getString("authtype"));

        // Get mcc and mnc, if null, split numerics to mcc and mnc, if numerics
        String numeric = infos.getString("numeric", default_APN.getString("numeric"));
        String mcc = infos.getString("mcc", default_APN.getString("mcc"));
        String mnc = infos.getString("mnc", default_APN.getString("mnc"));

        Log.d(TAG, "numeric=" + numeric + ",mcc=" + mcc + ",mnc=" + mnc);
        if (numeric == null) {
            if (mcc != null && mnc != null) {
                numeric = mcc + mnc;
            } else {
                return -1;
            }
        }

        if (mcc == null || mnc == null) {
            mcc = numeric.substring(0, 3);
            mnc = numeric.substring(3);
        }


        String proxy = infos.getString("proxy", default_APN.getString("proxy"));
        String port = infos.getString("port", default_APN.getString("port"));
        String mms_proxy = infos.getString("mms_proxy", default_APN.getString("mms_proxy"));
        String mms_port = infos.getString("mms_port", default_APN.getString("mms_port"));
        String user = infos.getString("user", default_APN.getString("user"));
        String server = infos.getString("server", default_APN.getString("server"));
        String password = infos.getString("password", default_APN.getString("password"));
        String mmsc = infos.getString("mmsc", default_APN.getString("mmsc"));
        String current = infos.getString("current", default_APN.getString("current"));
        String carrier_enabled = infos.getString("carrier_enabled", default_APN.getString("carrier_enabled"));
        String protocol = infos.getString("protocol", default_APN.getString("protocol"));
        String roaming_protocol = infos.getString("roaming_protocol", default_APN.getString("roaming_protocol"));
        String bearer = infos.getString("bearer", default_APN.getString("bearer"));
        String max_conns = infos.getString("max_conns", default_APN.getString("max_conns"));
        String max_conns_time = infos.getString("max_conns_time", default_APN.getString("max_conns_time"));
        String modem_cognitive = infos.getString("modem_cognitive", default_APN.getString("modem_cognitive"));
        String localized_name = infos.getString("localized_name", default_APN.getString("localized_name"));
        String mvno_match_data = infos.getString("mvno_match_data", default_APN.getString("mvno_match_data"));
        String mvno_type = infos.getString("mvno_type", default_APN.getString("mvno_type"));
        String profile_id = infos.getString("profile_id", default_APN.getString("profile_id"));
        String read_only = infos.getString("read_only", default_APN.getString("read_only"));
        String sub_id = infos.getString("sub_id", default_APN.getString("sub_id"));
        String type = infos.getString("type", default_APN.getString("type"));
        String ppp_number = infos.getString("ppp_number", default_APN.getString("ppp_number"));
        String visit_area = infos.getString("visit_area", default_APN.getString("visit_area"));
        String wait_time = infos.getString("wait_time", default_APN.getString("wait_time"));

        int apnId = -1;
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();

        /*
         *
         * Deleted some fields for not existing in Android 7!
         *
         * */
        // Configs showed in Rom settings
        values.put("name", name);
        values.put("apn", apn);
//        values.put("name", name);
//        values.put("apn", apn);
        values.put("proxy", proxy);
        values.put("port", port);
        values.put("user", user);
        values.put("password", password);
        values.put("numeric", numeric);
        values.put("server", server);
        values.put("mmsc", mmsc);
        values.put("mmsproxy", mms_proxy);
        values.put("mmsport", mms_port);
        values.put("mcc", mcc);
        values.put("mnc", mnc);
        values.put("authtype", authtype);
        values.put("type", type);
        values.put("protocol", protocol);
        values.put("roaming_protocol", roaming_protocol);
        values.put("bearer", bearer);
        values.put("mvno_type", mvno_type);

        //Configs showed in Rom settings
        values.put("current", current);
        values.put("carrier_enabled", carrier_enabled);
        values.put("max_conns", max_conns);
        values.put("max_conns_time", max_conns_time);
//        values.put("modem_cognitive", modem_cognitive);
//        values.put("localized_name", localized_name );
        values.put("profile_id", profile_id);
//        values.put("read_only", read_only);
//        values.put("sub_id", sub_id);
//        values.put("ppp_number", ppp_number);
//        values.put("visit_area", visit_area);
        values.put("wait_time", wait_time);

        Cursor cursor = null;
        Uri newRow = resolver.insert(APN_URI, values);
        if (newRow != null) {
            cursor = resolver.query(newRow, null, null, null, null);
            int idIdx = cursor.getColumnIndex("_id");
            cursor.moveToFirst();
            apnId = cursor.getShort(idIdx);
            // Log show all configuration of new APN
            Log.d(TAG, "Manual APN is " + " id: " + cursor.getShort(cursor.getColumnIndex("_id")) + " \napn: " + cursor.getString(cursor.getColumnIndex("apn")) + " \nname: " + cursor.getString(cursor.getColumnIndex("name")) + " \ncurrent: " + cursor.getString(cursor.getColumnIndex("current")) + " \ncarrier_enabled: " + cursor.getString(cursor.getColumnIndex("carrier_enabled")) + " \nmcc: " + cursor.getString(cursor.getColumnIndex("mcc")) + " \nmnc: " + cursor.getString(cursor.getColumnIndex("mnc")) + " \nauthtype: " + cursor.getString(cursor.getColumnIndex("authtype")) + " \nbearer: " + cursor.getString(cursor.getColumnIndex("bearer")) + " \nlocalized_name: " + cursor.getString(cursor.getColumnIndex("localized_name")) + " \nmax_conns: " + cursor.getString(cursor.getColumnIndex("max_conns")) + " \nmax_conns_time: " + cursor.getString(cursor.getColumnIndex("max_conns_time")) + " \nmmsc: " + cursor.getString(cursor.getColumnIndex("mmsc")) + " \nmmsport: " + cursor.getString(cursor.getColumnIndex("mmsport")) + " \nmmsproxy: " + cursor.getString(cursor.getColumnIndex("mmsproxy")) + " \nmodem_cognitive: " + cursor.getString(cursor.getColumnIndex("modem_cognitive")) + " \nmvno_match_data: " + cursor.getString(cursor.getColumnIndex("mvno_match_data")) + " \nmvno_type: " + cursor.getString(cursor.getColumnIndex("mvno_type")) + " \npassword: " + cursor.getString(cursor.getColumnIndex("password")) + " \nport: " + cursor.getString(cursor.getColumnIndex("port")) + " \nppp_number: " + cursor.getString(cursor.getColumnIndex("ppp_number")) + " \nprofile_id: " + cursor.getString(cursor.getColumnIndex("profile_id")) + " \nprotocol: " + cursor.getString(cursor.getColumnIndex("protocol")) + " \nproxy: " + cursor.getString(cursor.getColumnIndex("proxy")) + " \nread_only: " + cursor.getString(cursor.getColumnIndex("read_only")) + " \nroaming_protocol: " + cursor.getString(cursor.getColumnIndex("roaming_protocol")) + " \nserver: " + cursor.getString(cursor.getColumnIndex("server")) + " \nsub_id: " + cursor.getString(cursor.getColumnIndex("sub_id")) + " \ntype: " + cursor.getString(cursor.getColumnIndex("type")) + " \nuser: " + cursor.getString(cursor.getColumnIndex("user")) + " \nvisit_area: " + cursor.getString(cursor.getColumnIndex("visit_area")) + " \nwait_time: " + cursor.getString(cursor.getColumnIndex("wait_time")));
        }


        if (cursor != null) {
            cursor.close();
        }
        return apnId;
    }

    private boolean setDefaultAPN(int apnId) {

//        SharedPreferences sp = VfiServiceApp.getContext().getSharedPreferences(PREF_FILE_APN,
//                Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sp.edit();
//        editor.putLong(COLUMN_APN_ID + apnId, apnId);
//        editor.apply();
//
//
//        return true;
        boolean res = false;
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put("apn_id", apnId);

        // Android 5 设置当前APN
        int rows = resolver.update(PREFERRED_APN_URI, values, null, null);
        Log.d(TAG, " insert apn ,rows = " + rows);

        Cursor c = resolver.query(PREFERRED_APN_URI, new String[]{"_id", "name", "apn"}, "_id=" + apnId, null, null);
        int idIdx = c.getColumnIndex("_id");
        c.moveToFirst();
        int currentApnId = c.getShort(idIdx);
        String apn = c.getString(c.getColumnIndex("apn"));
        String name = c.getString(c.getColumnIndex("name"));

        Log.d(TAG, "Current prefer APN is " + currentApnId + " apn: " + apn + "; name: " + name);
        if (c != null) {
            res = true;
            c.close();
        }
        return res;
    }

    private String getSIMInfo() {
        Context context = VfiServiceApp.getContext();
        TelephonyManager iPhoneManager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        return iPhoneManager.getSimOperator();
    }

    // TODO: 2020/4/30
//    public Bundle getSelectedAPNConfigs(){
//
//        return null;
//    }

    public Bundle getSelectedAPNConfigs() {

        Bundle info = new Bundle();

        ContentResolver resolver = context.getContentResolver();

        String[] fields = {"_id", "name", "apn", "proxy", "port", "user", "password", "server", "mmsc", "mmsproxy", "mmsport", "mcc", "mnc", "authtype", "type", "protocol", "roaming_protocol", "bearer", "mvno_type", "mvno_match_data"};

        Cursor c = resolver.query(PREFERRED_APN_URI, fields, null, null, null);
        int idIdx = c.getColumnIndex("_id");
        c.moveToFirst();
        int currentApnId = c.getShort(idIdx);
        String name = c.getString(c.getColumnIndex("name"));
        String apn = c.getString(c.getColumnIndex("apn"));
        String proxy = c.getString(c.getColumnIndex("proxy"));
        String port = c.getString(c.getColumnIndex("port"));
        String user = c.getString(c.getColumnIndex("user"));
        String password = c.getString(c.getColumnIndex("password"));
        String server = c.getString(c.getColumnIndex("server"));
        String mmsc = c.getString(c.getColumnIndex("mmsc"));
        String mmsproxy = c.getString(c.getColumnIndex("mmsproxy"));
        String mmsport = c.getString(c.getColumnIndex("mmsport"));
        String mcc = c.getString(c.getColumnIndex("mcc"));
        String mnc = c.getString(c.getColumnIndex("mnc"));
        String authtype = c.getString(c.getColumnIndex("authtype"));
        String type = c.getString(c.getColumnIndex("type"));
        String protocol = c.getString(c.getColumnIndex("protocol"));
        String roaming_protocol = c.getString(c.getColumnIndex("roaming_protocol"));
        String bearer = c.getString(c.getColumnIndex("bearer"));
        String mvno_type = c.getString(c.getColumnIndex("mvno_type"));
        String mvno_match_data = c.getString(c.getColumnIndex("mvno_match_data"));

        info.putString("name", name);
        info.putString("apn", apn);
        info.putString("proxy", proxy);
        info.putString("port", port);
        info.putString("user", user);
        info.putString("password", password);
        info.putString("server", server);
        info.putString("mmsc", mmsc);
        info.putString("mmsproxy", mmsproxy);
        info.putString("mcc", mcc);
        info.putString("mnc", mnc);
        info.putString("authtype", authtype);
        info.putString("type", type);
        info.putString("protocol", protocol);
        info.putString("roaming_protocol", roaming_protocol);
        info.putString("bearer", bearer);
        info.putString("mvno_type", mvno_type);
        info.putString("mvno_match_data", mvno_match_data);

        return info;

    }

    public int selectMobileDataOnSlot(int slotIdx) {
        try {
            return setDefaultDataSubId(slotIdx);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int setDefaultDataSubId(int slotIdx) throws Exception {
        try {
            if (slotIdx < 0 || slotIdx > 1) {
                Log.e(TAG, "invalid slot idx got:" + slotIdx);
                return -1;
            }
            @SuppressLint("MissingPermission") int subid = SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(slotIdx).getSubscriptionId();
            Log.i(TAG, "Get subid:" + subid + " from slot:" + slotIdx);
            TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Method setDataEnabled = SubscriptionManager.from(context).getClass().getDeclaredMethod("setDefaultDataSubId", int.class);
            if (null != setDataEnabled) {
                setDataEnabled.invoke(SubscriptionManager.from(context), subid);
                Log.d(TAG, "setDataEnabled on " + subid + " success");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "setDataEnabled exception");
            return -1;
        }
        return 0;
    }
}

