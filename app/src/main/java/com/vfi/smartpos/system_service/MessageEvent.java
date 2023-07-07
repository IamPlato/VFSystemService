package com.vfi.smartpos.system_service;

import java.util.HashMap;

public class MessageEvent {
    private HashMap<String, Boolean> installRes;

    public MessageEvent(HashMap<String, Boolean> res) {
        this.installRes = res;
    }

    public HashMap<String, Boolean> getMessage() {
        return installRes;
    }

    public void setMessage(HashMap<String, Boolean> res) {
        this.installRes = res;
    }
}
