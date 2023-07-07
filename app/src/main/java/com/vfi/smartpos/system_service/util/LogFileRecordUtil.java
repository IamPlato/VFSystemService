package com.vfi.smartpos.system_service.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class LogFileRecordUtil extends Properties {

    private String recordPath = "";

    private LogFileRecordUtil(String path) {
        recordPath = path;
    }

    public static LogFileRecordUtil getInstance(String path) {
        File file = new File(path);
        File directoryFile = new File(file.getParent());
        if (!directoryFile.exists()){
            directoryFile.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
        LogFileRecordUtil logFileRecordUtil = new LogFileRecordUtil(path);
        try {
            InputStream is = new FileInputStream(file);
            logFileRecordUtil.load(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return logFileRecordUtil;
    }

    @Override
    public Object setProperty(String key, String value) {
        super.setProperty(key, value);
        try {
            this.store(new FileOutputStream(this.recordPath), "utf-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }
}