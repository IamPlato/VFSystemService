package com.vfi.smartpos.system_service.androidp;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

public final class IOUtils {
    public static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
                ignored.printStackTrace();
            }
        }
    }

    public static void closeQuietly(Socket c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
                ignored.printStackTrace();
            }
        }
    }
}
