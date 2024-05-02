package com.erasmicoin.euspa.gsa.egnss4all.model.OSNMA;

public class ServerPostResponse {

    public static final String CALL_EXCEPTION = "CALL_EXCEPTION";
    public static final String GENERIC_EXCEPTION = "GENERIC_EXCEPTION";
    public static final String OK = "OK";
    public static final String KO = "NOT OK";

    private String status;
    private int listLine;
    private long lastId;

    public ServerPostResponse(String status, int listLine, long lastId) {
        this.status = status;
        this.listLine = listLine;
        this.lastId =  lastId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getListLine() {
        return listLine;
    }

    public void setListLine(int listLine) {
        this.listLine = listLine;
    }

    public long getLastId(){ return lastId;}

    public void setLastId(long lastId){ this.lastId = lastId; }
}
