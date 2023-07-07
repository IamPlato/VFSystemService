// IAppInstallObserver.aidl
package com.vfi.smartpos.system_service.aidl;

interface IAppInstallObserver {
    /* Install Callback：returnCode：0-sucess，other value-fail */
	/**
	 * Install Callback：returnCode：0-sucess，other value-fail
	 * @param packageName - package name.
	 * @param returnCode - package name.<BR>
     * <BR>
     * <b>The following is the returnCode for V4</b><BR>
     * public static final int STATUS_FAILURE = 1;<BR>
     * public static final int STATUS_FAILURE_BLOCKED = 2;<BR>
     * public static final int STATUS_FAILURE_ABORTED = 3;<BR>
     * public static final int STATUS_FAILURE_INVALID = 4;<BR>
     * public static final int STATUS_FAILURE_CONFLICT = 5;<BR>
     * public static final int STATUS_FAILURE_STORAGE = 6;<BR>
     * public static final int STATUS_FAILURE_INCOMPATIBLE = 7;<BR>
     * public static final int STATUS_PENDING_USER_ACTION = -1;<BR>
     * <b>The following is the returnCode for V2</b><BR>
     * public static final int INSTALL_FAILED_ALREADY_EXISTS = -1;<BR>
     * public static final int INSTALL_FAILED_INVALID_APK = -2;<BR>
     * public static final int INSTALL_FAILED_INVALID_URI = -3;<BR>
     * public static final int INSTALL_FAILED_INSUFFICIENT_STORAGE = -4;<BR>
     * public static final int INSTALL_FAILED_DUPLICATE_PACKAGE = -5;<BR>
     * public static final int INSTALL_FAILED_NO_SHARED_USER = -6;<BR>
     * public static final int INSTALL_FAILED_UPDATE_INCOMPATIBLE = -7;<BR>
     * public static final int INSTALL_FAILED_SHARED_USER_INCOMPATIBLE = -8;<BR>
     * public static final int INSTALL_FAILED_MISSING_SHARED_LIBRARY = -9;<BR>
     * public static final int INSTALL_FAILED_REPLACE_COULDNT_DELETE = -10;<BR>
     * public static final int INSTALL_FAILED_DEXOPT = -11;<BR>
     * public static final int INSTALL_FAILED_OLDER_SDK = -12;<BR>
     * public static final int INSTALL_FAILED_CONFLICTING_PROVIDER = -13;<BR>
     * public static final int INSTALL_FAILED_NEWER_SDK = -14;<BR>
     * public static final int INSTALL_FAILED_TEST_ONLY = -15;<BR>
     * public static final int INSTALL_FAILED_CPU_ABI_INCOMPATIBLE = -16;<BR>
     * public static final int INSTALL_FAILED_MISSING_FEATURE = -17;<BR>
     * public static final int INSTALL_FAILED_CONTAINER_ERROR = -18;<BR>
     * public static final int INSTALL_FAILED_INVALID_INSTALL_LOCATION = -19;<BR>
     * public static final int INSTALL_FAILED_MEDIA_UNAVAILABLE = -20;<BR>
     * public static final int INSTALL_FAILED_VERIFICATION_TIMEOUT = -21;<BR>
     * public static final int INSTALL_FAILED_VERIFICATION_FAILURE = -22;<BR>
     * public static final int INSTALL_FAILED_PACKAGE_CHANGED = -23;<BR>
     * public static final int INSTALL_FAILED_UID_CHANGED = -24;<BR>
     * public static final int INSTALL_FAILED_VERSION_DOWNGRADE = -25;<BR>
     * public static final int INSTALL_FAILED_PERMISSION_MODEL_DOWNGRADE = -26;<BR>
     * public static final int INSTALL_PARSE_FAILED_NOT_APK = -100;<BR>
     * public static final int INSTALL_PARSE_FAILED_BAD_MANIFEST = -101;<BR>
     * public static final int INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION = -102;<BR>
     * public static final int INSTALL_PARSE_FAILED_NO_CERTIFICATES = -103;<BR>
     * public static final int INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES = -104;<BR>
     * public static final int INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING = -105;<BR>
     * public static final int INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME = -106;<BR>
     * public static final int INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID = -107;<BR>
     * public static final int INSTALL_PARSE_FAILED_MANIFEST_MALFORMED = -108;<BR>
     * public static final int INSTALL_PARSE_FAILED_MANIFEST_EMPTY = -109;<BR>
     * public static final int INSTALL_FAILED_INTERNAL_ERROR = -110;<BR>
     * public static final int INSTALL_FAILED_USER_RESTRICTED = -111;<BR>
     * public static final int INSTALL_FAILED_DUPLICATE_PERMISSION = -112;<BR>
     * public static final int INSTALL_FAILED_NO_MATCHING_ABIS = -113;<BR>
     * public static final int NO_NATIVE_LIBRARIES = -114;<BR>
     * public static final int INSTALL_FAILED_ABORTED = -115;<BR>
	 */
    void onInstallFinished(String packageName,int returnCode);
}
