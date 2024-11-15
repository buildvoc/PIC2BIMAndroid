package com.example.ELFA.edas.sisnet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


class SbasIonoCorrection {

    private double vertDelay;
    private int giveI;

    private Calendar timeStamp;

    private final Lock lock = new ReentrantLock();

    public SbasIonoCorrection(){
        vertDelay = SisnetConstants.NULL_CODE;
        giveI =SisnetConstants.NULL_CODE;
        timeStamp = new GregorianCalendar();
    }

    public SbasIonoCorrection(double vertDelay, int giveI, Calendar timeStamp){
        this.vertDelay = vertDelay;
        this.giveI = giveI;
        this.timeStamp = timeStamp;
    }

    /** Method to set the vertical delay corrections of an IGP
     *
     * @param vertDelay: Double with the vertical delay
     * */
    void updateVertDelay(double vertDelay) {
        lock.lock();
        try {
            this.vertDelay = vertDelay;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to set the GIVEI of an IGP
     *
     * @param giveI: Integer with the GIVEI
     * */
    void updateGiveI(int giveI) {
        lock.lock();
        try {
            this.giveI = giveI;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to set the time of the corrections of an IGP
     * @param timeStamp: Timestamp of the correction
     * */
    void updateTimeStamp(Calendar timeStamp) {
        lock.lock();
        try {
            this.timeStamp = timeStamp;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the time of the iono corrections of the IGP
     *
     * @return Calendar timestamp : Timestamp of the stored correction
     *
     * */
    Calendar getIonoTimestamp() {
        lock.lock();
        try {
            return this.timeStamp;
        } finally {
            lock.unlock();
        }
    }

    /** Method to obtain the vertical delay of an IGP
     *
     * @return double give : vertical delay correction of the IGP
     *
     * */
    double getVertDelay() {
        lock.lock();
        try {
            return this.vertDelay;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the GIVEI of an IGP
     *
     * @return int giveI : GIVEI of the IGP
     *
     * */
    int getGiveI() {
        lock.lock();
        try {
            return this.giveI;
        } finally {
            lock.unlock();
        }
    }
}
