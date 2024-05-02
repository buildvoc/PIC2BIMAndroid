package com.example.ELFA.edas.sisnet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SbasFastCorrection {
    private double prc;
    private Calendar timeStampFast; // For MT 2-5 & MT 24
    private Calendar timeStampIntegrity; // For MT 6
    private double rrc;
    private int udreI;
    private final Lock lock = new ReentrantLock();

    public SbasFastCorrection(){
        prc = SisnetConstants.NULL_CODE;
        timeStampFast = new GregorianCalendar();
        timeStampIntegrity = new GregorianCalendar();
        rrc = SisnetConstants.NULL_CODE;
        udreI = SisnetConstants.NULL_CODE;
    }


    /**
     * Method to update the satellite Range correction
     * @param prc : The pseudo-range correction
     * @paramm timeStamp : The timestamp of the correction
     *
     */
    void updateRangeCorrection (double prc, Calendar timeStamp){
        lock.lock();

        try {
            double deltaTime = 0;
            if (this.udreI != SisnetConstants.UDREI_NOT_MONITORED) {
                deltaTime = timeStamp.getTimeInMillis() - this.timeStampFast.getTimeInMillis();
            }

            if (deltaTime > 0 && deltaTime < SisnetConstants.THRESHOLD_RANGE_RATE * 1000) {
                this.rrc = (prc - this.prc) / deltaTime;
            }
            else {
                this.rrc = 0;
            }

            this.prc = prc;
            this.timeStampFast = timeStamp;
        }

        finally {
            lock.unlock();
        }
    }


    /** Method to set the UDRE of a satellite
     *
     * @param udreI: Integer with the Udre
     *
     * */
    void updateUdreI (int udreI){

        lock.lock();

        try {
            this.udreI = udreI;
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to set the timestamp for integrity message (MT 6)
     *
     * @paramm timeStamp : The timestamp of the integrity message
     *
     * */
    void updateIntegrityTimeStamp (Calendar timeStamp){

        lock.lock();

        try {
            this.timeStampIntegrity = timeStamp;
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to obtain the PRC + RRC Correction of a satellite
     *
     * @param currentTime : The timestamp of the measuremnent to which the correction will be applied
     * @return double correction: The calculated correction.
     *
     * */
    double getRangeCorrection (Calendar currentTime) {
        lock.lock();
        try {
            double deltaTime = currentTime.getTimeInMillis() - timeStampFast.getTimeInMillis();
            double correction;
            // Check that prn and rrc have been updated, else return null_code
            if (prc == SisnetConstants.NULL_CODE || rrc == SisnetConstants.NULL_CODE) {
                correction = SisnetConstants.NULL_CODE;
            }
            else {
                correction = prc + rrc * deltaTime;
            }
            return correction;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the UDRE of a satellite. Valid range 1-15 
     *
     * @return int udre : Position of the satellite
     *
     * */
    int getUdreI () {
        lock.lock();
        try {
            return this.udreI;
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to obtain the time of the Fast corrections of the satellite
     *
     * @return Calendar timestamp : Timestamp of the stored correction
     *
     * */
    Calendar getFastTimeStamp(){
        lock.lock();
        try{
            return this.timeStampFast;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the time of the Fast corrections of the satellite
     *
     * @return Calendar timestamp : Timestamp of the stored correction
     *
     * */
    Calendar getIntegrityTimeStamp(){
        lock.lock();
        try{
            return this.timeStampIntegrity;
        }
        finally {
            lock.unlock();
        }
    }

}
