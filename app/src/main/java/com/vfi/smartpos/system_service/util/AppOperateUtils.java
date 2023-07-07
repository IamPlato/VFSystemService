package com.vfi.smartpos.system_service.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.vfi.smartpos.system_service.SystemManager;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AppOperateUtils {

    private static final String TAG = "AppOperateUtils";
    private static int retryCount = 0;

    public static boolean silentInstallAPK(final Context context, final String apkPath, final String mInstallerPkgName) {
        // 注册安装应用广播接收器
        if (mInstallReceiver == null) {
            mInstallReceiver = new InstallReceiver();
        }

        IntentFilter filterInstall = new IntentFilter();
        filterInstall.addAction(Intent.ACTION_PACKAGE_ADDED);
        filterInstall.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filterInstall.addDataScheme("package");
        context.registerReceiver(mInstallReceiver, filterInstall);
        Log.e(TAG, "registerReceiver InstallReceiver success");

        File apkFile = new File(apkPath);

        Log.i(TAG, "Get in silentInstallAPK(), apkPath = " + apkPath);
        if (!apkFile.exists()) {
            Log.e(TAG, apkPath + "does not exist!");
            return false;
        }

        try {
            Uri packageUri = Uri.fromFile(apkFile);

            int flags = 0x00000002 | 0x00000080;

            PackageManager packMngr = context.getPackageManager();

            Method installPackageMethod = Class.forName("android.content.pm.PackageManager").getMethod("installPackage",
                    Uri.class, Class.forName("android.content.pm.IPackageInstallObserver"), int.class, String.class);
            installPackageMethod.setAccessible(true);

            try {
                installPackageMethod.invoke(packMngr, packageUri, /*null*/new IPackageInstallObserver.Stub() {
                    @Override
                    public void packageInstalled(final String packageName, final int returnCode) throws RemoteException {
                        Log.i(TAG, "packageInstalled, packageName = " + packageName + ", returnCode=" + returnCode + ",return mean=" + getReturnMeans(returnCode));
                        if (returnCode == -22 && retryCount == 0) {
                            //验证失败，重试一次
                            Log.e(TAG, "verification failed, retry!!");
                            retryCount++;
                            silentInstallAPK(context, apkPath, mInstallerPkgName);
                        } else {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (returnCode == 1) {
//                                    Toast.makeText(MainActivity.this, "app" + packageName + "install sucess", Toast.LENGTH_SHORT).show();
//                                } else {
//                                    Toast.makeText(MainActivity.this, "app" + packageName + "install failed" + getReturnMeans(returnCode), Toast.LENGTH_SHORT).show();
//                                }
//                            }
//                        });
                            retryCount = 0;
                            SystemManager.getInstance().cbInstallObserver(packageName, returnCode);
                            if (returnCode != 1) {
                                unregisterReceiver(context);
                            }
                        }
                    }
                }, flags, mInstallerPkgName);
                return true;
            } catch (InvocationTargetException | IllegalAccessException ite) {
                ite.printStackTrace();
                return false;
            }
        } catch (ClassNotFoundException | NoSuchMethodException cnfe) {
            cnfe.printStackTrace();
            return false;
        }
    }


    /**
     * 静默卸载
     */
    public static boolean silentDeletePKG(final Context context, String packageName) {
        // 注册删除应用广播接收器
        if (mDeleteReceiver == null) {
            mDeleteReceiver = new DeleteReceiver();
        }

        IntentFilter filterDelete = new IntentFilter();
        filterDelete.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filterDelete.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        filterDelete.addDataScheme("package");
        context.registerReceiver(mDeleteReceiver, filterDelete);
        Log.e(TAG, "registerReceiver DeleteReceiver success");

        Log.i(TAG, "Get in silentDeletePKG(), packageName = " + packageName);

        if (!checkPackage(context, packageName)) {
            Log.e(TAG, packageName + "does not exist!");
            return false;
        }
        /* 非静默方式
        Uri packageURI = Uri.parse("package:" + packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE,packageURI);
        uninstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        VfiServiceApp.getContext().startActivity(uninstallIntent);
        return true;*/
    /*PrintWriter PrintWriter = null;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            PrintWriter = new PrintWriter(process.getOutputStream());
            PrintWriter.println("LD_LIBRARY_PATH=/vendor/lib:/system/lib ");
            PrintWriter.println("pm uninstall "+packageName);
            PrintWriter.flush();
            PrintWriter.close();
            int value = process.waitFor();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            if(process!=null){
                process.destroy();
            }
        }
        return false;*/


        try {
            // 方法参数1
            PackageManager packMngr = context.getPackageManager();

            // 方法参数2
            // Class clsObserver =
            // Class.forName("android.content.pm.IPackageDeleteObserver");

            // 获取方法
            Method deletePackageMethod = Class.forName("android.content.pm.PackageManager").getMethod("deletePackage",
                    String.class, Class.forName("android.content.pm.IPackageDeleteObserver"), int.class);

            deletePackageMethod.setAccessible(true);

            // 执行方法
            try {
                deletePackageMethod.invoke(packMngr, packageName, /*null*/new IPackageDeleteObserver.Stub() {
                    @Override
                    public void packageDeleted(String packageName, int returnCode) throws RemoteException {
                        Log.i(TAG, "packageDeleted, packageName = " + packageName + ", returnCode=" + returnCode);
                        if (returnCode != 1) { // 返回删除App遇到错误返回错误信息，成功信息在广播中返回
                            SystemManager.getInstance().cbDeleteObserver(packageName, returnCode);
                        }
                    }
                }, 0);
                return true;
            } catch (InvocationTargetException | IllegalAccessException ite) {
                ite.printStackTrace();
                return false;
            }
        } catch (ClassNotFoundException | NoSuchMethodException cnfe) {
            cnfe.printStackTrace();
            return false;
        }
    }

    /**
     * 检测该包名所对应的应用是否存在
     *
     * @param packageName
     * @return
     */
    private static boolean checkPackage(Context context, String packageName) {
        if (packageName == null || "".equals(packageName))
            return false;
        try {
            context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static String getReturnMeans(int returnCode) {
        String msg = "";
        switch (returnCode) {
            case -1:
                msg = "INSTALL_FAILED_ALREADY_EXISTS";
                break;
            case -2:
                msg = "INSTALL_FAILED_INVALID_APK";
                break;
            case -3:
                msg = "INSTALL_FAILED_INVALID_URL";
                break;
            case -4:
                msg = "INSTALL_FAILED_INSUFFICIENT_STORAGE";
                break;
            case -5:
                msg = "INSTALL_FAILED_DUPLICATE_PACKAGE";
                break;
            case -6:
                msg = "INSTALL_FAILED_NO_SHARED_USER";
                break;
            case -7:
                msg = "INSTALL_FAILED_UPDATE_INCOMPATIBLE";
                break;
            case -8:
                msg = "INSTALL_FAILED_SHARED_USER_INCOMPATIBLE";
                break;
            case -9:
                msg = "INSTALL_FAILED_MISSING_SHARED_LIBRARY";
                break;
            case -10:
                msg = "INSTALL_FAILED_REPLACE_COULDNT_DELETE";
                break;
            case -11:
                msg = "INSTALL_FAILED_DEXOPT";
                break;
            case -12:
                msg = "INSTALL_FAILED_OLDER_SDK";
                break;
            case -13:
                msg = "INSTALL_FAILED_CONFLICTING_PROVIDER";
                break;
            case -14:
                msg = "INSTALL_FAILED_NEWER_SDK";
                break;
            case -15:
                msg = "INSTALL_FAILED_TEST_ONLY";
                break;
            case -16:
                msg = "INSTALL_FAILED_CPU_ABI_INCOMPATIBLE";
                break;
            case -17:
                msg = "INSTALL_FAILED_MISSING_FEATURE";
                break;
            case -18:
                msg = "INSTALL_FAILED_CONTAINER_ERROR";
                break;
            case -19:
                msg = "INSTALL_FAILED_INVALID_INSTALL_LOCATION";
                break;
            case -20:
                msg = "INSTALL_FAILED_MEDIA_UNAVAILABLE";
                break;
            case -100:
                msg = "INSTALL_PARSE_FAILED_NOT_APK";
                break;
            case -101:
                msg = "INSTALL_PARSE_FAILED_BAD_MANIFEST";
                break;
            case -102:
                msg = "INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION";
                break;
            case -103:
                msg = "INSTALL_PARSE_FAILED_NO_CERTIFICATES";
                break;
            case -104:
                msg = "INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES";
                break;
            case -105:
                msg = "INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING";
                break;
            case -106:
                msg = "INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME";
                break;
            case -107:
                msg = "INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID";
                break;
            case -108:
                msg = "INSTALL_PARSE_FAILED_MANIFEST_MALFORMED";
                break;
            case -109:
                msg = "INSTALL_PARSE_FAILED_MANIFEST_EMPTY";
                break;
            case -110:
                msg = "INSTALL_FAILED_INTERNAL_ERROR";
                break;
            default:
                msg = "" + returnCode;
                break;
        }

        return msg;
    }

    private static InstallReceiver mInstallReceiver;
    private static DeleteReceiver mDeleteReceiver;

    private static class InstallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Receive Intent.action=" + action);

            //Intent.ACTION_PACKAGE_NEEDS_VERIFICATION
            if (action.equals(Intent.ACTION_PACKAGE_ADDED)
                    || action.equalsIgnoreCase(Intent.ACTION_PACKAGE_REPLACED)) {
                final String packageName = intent.getData().getSchemeSpecificPart();
                Log.i(TAG, "Install Success, PackageName=" + packageName);

                unregisterReceiver(context);
                try {
                    SystemManager.getInstance().cbInstallObserver(packageName, 0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void unregisterReceiver(Context context) {
        // 注销广播接收器
        if (mInstallReceiver != null) {
            Log.e(TAG, "unregisterReceiver InstallReceiver");
            context.unregisterReceiver(mInstallReceiver);
            mInstallReceiver = null;
        }

        if (mDeleteReceiver != null) {
            Log.e(TAG, "unregisterReceiver DeleteReceiver");
            context.unregisterReceiver(mDeleteReceiver);
            mDeleteReceiver = null;
        }
    }

    private static class DeleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Receive Intent.action=" + action);

            if (action.equals(Intent.ACTION_PACKAGE_REMOVED)
                    || action.equals(Intent.ACTION_PACKAGE_FULLY_REMOVED)) {
                final String packageName = intent.getData().getSchemeSpecificPart();
                Log.i(TAG, "Uninstall Success, PackageName=" + packageName);

                unregisterReceiver(context);
                try {
                    SystemManager.getInstance().cbDeleteObserver(packageName, 0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            return;
        }
    }
}
