// ICustomerConfigurationUpdateListener.aidl
package com.vfi.smartpos.system_service.aidl.settings;

interface ICustomerConfigurationUpdateListener {
   //0:succeed, device will reboot
   //
   void onError(int errCode, String msg);
}