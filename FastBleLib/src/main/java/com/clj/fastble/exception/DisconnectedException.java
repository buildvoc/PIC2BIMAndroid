package com.clj.fastble.exception;


public class DisconnectedException extends BleException {

    public DisconnectedException() {
        super(ERROR_CODE_DISCONNECTED, "This device is disconnected!");
    }

}
