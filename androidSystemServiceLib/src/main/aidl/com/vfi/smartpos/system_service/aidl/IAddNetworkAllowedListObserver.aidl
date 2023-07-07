// IAddNetworkAllowedListObserver.aidl
package com.vfi.smartpos.system_service.aidl;

interface IAddNetworkAllowedListObserver {

    /**
     * Result of adding network allowed list
     * @param result - result
     * @see AddNetworkAllowedListResult
     * @since 1.11.3
     */
    void onResult(int result);
}
