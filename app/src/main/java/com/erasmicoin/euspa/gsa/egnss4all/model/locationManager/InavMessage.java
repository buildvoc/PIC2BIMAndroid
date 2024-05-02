package com.erasmicoin.euspa.gsa.egnss4all.model.locationManager;

public class InavMessage {
    private int svid;
    private long timestamp;
    private String inavMessage;

    private boolean validated;

    public InavMessage(int svid, long timestamp, String inavMessage) {
        this.svid = svid;
        this.timestamp = timestamp;
        this.inavMessage = inavMessage;
    }

    public int getSvid() {
        return svid;
    }

    public void setSvid(int svid) {
        this.svid = svid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getInavMessage() {
        return inavMessage;
    }

    public void setInavMessage(String inavMessage) {
        this.inavMessage = inavMessage;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }
}
