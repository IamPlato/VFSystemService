// IRestoreObserver.aidl
package com.vfi.smartpos.system_service.aidl;

interface IRestoreObserver {
    /* returnCode: 执行结果，0表示成功，非0表示失败 */
    void onRestoreFinished(int returnCode);
}
