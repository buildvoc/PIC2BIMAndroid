package com.clj.fastble.exception;


public class DiscoveredException extends BleException {

    public DiscoveredException(String description) {
        super(ERROR_CODE_DISCOVERED, description);
    }

}
