// IBackupObserver.aidl
package com.vfi.smartpos.system_service.aidl;

interface IBackupObserver {
    /* returnCode: 执行结果，0表示成功，非0表示失败
       fileName: 生成的备份包路径 */
    void onBackupFinished(int returnCode, String fileName);
}
