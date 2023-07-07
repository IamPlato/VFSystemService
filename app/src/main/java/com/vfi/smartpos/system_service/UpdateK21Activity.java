package com.vfi.smartpos.system_service;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.socsi.SoSDKManager;
import com.socsi.k21gpio.K21GpioController;
import com.verifone.exception.SDKException;
import com.verifone.jni.vfc.VfcUpdateCallBack;
import com.verifone.jni.vfc.VfcUpdateInfo;
import com.verifone.smartpos.api.SdkApiHolder;
import com.verifone.smartpos.api.entities.terminal.vfuup.VfuupUpgradeInfo;
import com.verifone.smartpos.api.terminal.ITerminalManager;
import com.verifone.smartpos.api.terminal.OnVfuupUpdateCallback;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static com.verifone.smartpos.api.terminal.FirmwareUpdateResult.ERR_EQUALS_VERSION;
import static com.verifone.smartpos.api.terminal.FirmwareUpdateResult.SUCCESS;

/**
 * for k21 update
 * added by laikey
 * 20170925
 */

public class UpdateK21Activity extends Activity {
    private static String TAG = "UpdateK21Activity";
    private SoSDKManager soSDKManager;
    private ImageView progress_bar;
    private TextView processing;
    private TextView k21_title;
    private boolean isFinished;
    private boolean isNeedSysUpdate;
    private String appBin, sysBin;
    private String uupPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        setContentView(R.layout.activity_update_k21);
        setFinishOnTouchOutside(false);

        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        mInView = (LinearLayout) inflater.inflate(R.layout.activity_update_k21, null, false);
        progress_bar = (ImageView) mInView.findViewById(R.id.progress_bar);
        processing = (TextView) mInView.findViewById(R.id.processing);
        k21_title = (TextView) mInView.findViewById(R.id.k21_title);

        createFloatView();
        showAnimation();

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Log.d(TAG, "wait 10s...");
//                    Thread.sleep(10000);
//                    Log.d(TAG, "wait 10s finish!");
//                    installresult.put("", true);
//                    EventBus.getDefault().post(new MessageEvent(installresult));
//                    finish();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();

        soSDKManager = (SoSDKManager) this.getSystemService("SoSDKService");
//        progressBar = (ImageView) findViewById(R.id.progress_bar);
        try {
            Bundle bdl = getIntent().getExtras();
            if (bdl != null) {
                final boolean isUpdateApp = bdl.getBoolean("isUpdateApp", false);
                final boolean isUpdateSys = bdl.getBoolean("isUpdateSys", false);
                boolean isUpdatePackage = false;

                appBin = bdl.getString("appBin", "");
                sysBin = bdl.getString("sysBin", "");
                uupPath = bdl.getString("updatePackagePath", "");
                if (!TextUtils.isEmpty(uupPath))
                    isUpdatePackage = true;

                Log.i(TAG, "isUpdateApp=" + isUpdateApp + ", isUpdateSys=" + isUpdateSys);
                Log.i(TAG, "isUpdateApp path=" + appBin + ", isUpdateSys path=" + sysBin);
                Log.i(TAG, "updatePackage path=" + uupPath);

                isNeedSysUpdate = isUpdateSys;
                int msgid = 0;
                if (isUpdateApp) {
                    msgid = 11;
                } else if (isUpdateSys) {
                    msgid = 12;
                } else if (isUpdatePackage) {
                    msgid = 13;
                }
                if (msgid != 0) {
                    Message msg = handlerUpdateK21.obtainMessage(msgid);
                    msg.sendToTarget();
                } else {
                    finish();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "UpdateK21Activity, exception=" + e.getMessage());
            Toast.makeText(UpdateK21Activity.this, "update k21 paramater invalid", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        Transfer.getInstance().setNormalBaudrateModel();
        homeKeyOnoff(false);
        isFinished = true;
        K21GpioController power = new K21GpioController();
        power.K21PowerOff();
        SystemClock.sleep(1000);
        power.K21PowerOn();
        SystemClock.sleep(2000);
        closeFloatView();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_HOME) {
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void homeKeyOnoff(boolean onoff) {
        try {
            soSDKManager.setKeyHomeState(onoff);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    private String procName;
    private HashMap<String, Boolean> installresult = new HashMap<>();

    private void updateK21_new(int updateType) {
        String filePath = "";

        if (updateType == 0) {
            filePath = appBin;
            procName = "update k21 app ";
        } else if (updateType == 1) {
            filePath = sysBin;
            procName = "update k21 system ";
        } else if (updateType == 2) {
            filePath = uupPath;
            procName = "update total package ";
        } else {

        }

        writeK21UpdateStatus("start:" + procName);

//        resetK21();

        Log.i(TAG, "start to " + procName);

        try {
            File f = new File(filePath);
            if (!f.exists()) {
                Log.e(TAG, "k21 file can't find:" + filePath);
                return;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    k21_title.setText(procName);
                    processing.setText("Updating... Pls wait");
                }
            });

            final String finalFilePath = filePath;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final ITerminalManager terminalManager = SdkApiHolder.getInstance().getTerminalManager();
                    try {
                        Log.d(TAG, "begin updateBin...");
                        terminalManager.updateBin(finalFilePath, new OnVfuupUpdateCallback() {
                            @Override
                            public void onProgress(String fileName, final double percentage) {
                                Log.i(TAG, procName + "onProgress: " + Math.round(percentage) + "%\n");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        k21_title.setText(procName);
                                        processing.setText("completed: " + Math.round(percentage) + "%");
                                    }
                                });
                            }

                            @Override
                            public void onUpdateResult(int code, VfuupUpgradeInfo vfuupUpgradeInfo) {
                                Log.i(TAG, procName + "onUpdateResult: " + code);
                                if (code == SUCCESS || code == ERR_EQUALS_VERSION) {
                                    Log.i(TAG, procName + " updating successfully\n");
                                    writeK21UpdateStatus("ok");
                                    installresult.put(procName, true);
                                } else {
                                    Log.i(TAG, procName + "result : failed" + "\n");
                                    writeK21UpdateStatus("fail: unknown");
                                    installresult.put(procName, false);
                                }

                                SystemClock.sleep(1000);
                                if (procName.contains("app") && isNeedSysUpdate) {
                                    Message msg = handlerUpdateK21.obtainMessage(12);
                                    msg.sendToTarget();
                                } else {
                                    EventBus.getDefault().post(new MessageEvent(installresult));
                                    finish();
                                }
                            }
                        });
                    } catch (SDKException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Handler handlerUpdateK21 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    progress_bar.setImageResource(R.mipmap.bg_loading_01);
                    break;
                case 1:
                    progress_bar.setImageResource(R.mipmap.bg_loading_02);
                    break;
                case 2:
                    progress_bar.setImageResource(R.mipmap.bg_loading_03);
                    break;
                case 3:
                    progress_bar.setImageResource(R.mipmap.bg_loading_04);
                    break;
                case 4:
                    progress_bar.setImageResource(R.mipmap.bg_loading_05);
                    break;
                case 5:
                    progress_bar.setImageResource(R.mipmap.bg_loading_06);
                    break;
                case 6:
                    progress_bar.setImageResource(R.mipmap.bg_loading_07);
                    break;
                case 7:
                    progress_bar.setImageResource(R.mipmap.bg_loading_08);
                    break;
                case 11:
                    updateK21_new(0); // App Type
                    break;
                case 12:
                    updateK21_new(1); //sys Type
                    break;
                case 13:
                    updateK21_new(2); //total Type
                    break;
                default:
//                    Toast.makeText(UpdateK21Activity.this, msg.getData().getString("msg").toString(), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    private void writeK21UpdateStatus(String status) {
        try {
            FileWriter fw = new FileWriter("/sdcard/k21updatestatus.txt");
            if (fw != null) {
                fw.write(status);
                fw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 创建悬浮窗
     */
    private static WindowManager wm;
    private static WindowManager.LayoutParams params;
    private LinearLayout mInView;

    private void createFloatView() {
        wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();  //WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 设置window type
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        params.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明
        params.flags = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;  //window gets focus
        params.windowAnimations = android.R.style.Animation_Translucent;

        // 设置悬浮窗的长得宽
        params.width = 500;
        params.height = 350;

        wm.addView(mInView, params);
    }

    private void closeFloatView() {
        wm.removeView(mInView);
    }

    /**
     * 显示动画效果
     */
    private void showAnimation() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                int i = 0;
                isFinished = false;
                while (!isFinished) {
                    try {
                        sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Message msg = handlerUpdateK21.obtainMessage(i % 8);
                    msg.sendToTarget();
                    i++;
                    if (i >= 7) {
                        i = 0;
                    }
                }
            }
        }.start();
    }
}