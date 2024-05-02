package com.erasmicoin.euspa.gsa.egnss4all.model.ekf;

public interface EkfReceiver {
    // !!! performed in an external thread
    void receive(EkfData ekfData);
}


