package com.example.ELFA.edas.sisnet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SbasLongCorrection {

    private double deltaX;
    private double deltaY;
    private double deltaZ;
    private double deltaClock;

    private double deltaVX;
    private double deltaVY;
    private double deltaVZ;
    private double deltaVClock;

    private int iode;
    private int velocityCode;

    private Calendar refTime;
    private Calendar timeStamp;
    private final Lock lock = new ReentrantLock();

    public SbasLongCorrection(){
        deltaX = SisnetConstants.NULL_CODE;
        deltaY = SisnetConstants.NULL_CODE;
        deltaZ = SisnetConstants.NULL_CODE;
        deltaClock = SisnetConstants.NULL_CODE;
        deltaVX = SisnetConstants.NULL_CODE;
        deltaVY = SisnetConstants.NULL_CODE;
        deltaVZ = SisnetConstants.NULL_CODE;
        deltaVClock = SisnetConstants.NULL_CODE;
        iode = SisnetConstants.NULL_CODE;
        velocityCode = SisnetConstants.NULL_CODE;
        refTime= new GregorianCalendar();
        timeStamp = new GregorianCalendar();
    }

    /** Method to set the SBAS IODE corrections of a satellite
     *
     * @param iode: Integer with the IODE
     * */
    void updateIode (int iode) {
      lock.lock();
      try {
          this.iode = iode;
      }
      finally {
          lock.unlock();
      }
    }

    /** Method to set the Long Correctiosn of a Satellite from an Velocity code 0 message
     *
     * @param deltaX : Double with the X position delta
     * @param deltaY : Double with the Y position delta
     * @param deltaZ : Double with the Z position delta
     * @param deltaClock : Double with the clock delta
     * @param timeStamp: Timestamp of the correction
     *
     *
     * */
    void updateDeltasV0 (double deltaX, double deltaY, double deltaZ, double deltaClock, Calendar timeStamp){
        lock.lock();
        try{
            this.deltaX= deltaX;
            this.deltaY= deltaY;
            this.deltaZ= deltaZ;
            this.deltaClock= deltaClock;
            this.timeStamp = timeStamp;
            this.velocityCode = 0;
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to set the Long Correctiosn of a Satellite from an Velocity code 1 message
     *
     * @param deltaX : Double with the X position delta
     * @param deltaY : Double with the Y position delta
     * @param deltaZ : Double with the Z position delta
     * @param deltaClock : Double with the clock delta
     * @param deltaVX : Double with the X  velocity delta
     * @param deltaVY : Double with the Y velocity delta
     * @param deltaVZ : Double with the Z velocity delta
     * @param deltaVClock : Double with the clock velocity delta
     * @param timeStamp: Timestamp of the correction
     *
     *
     * */
    void updateDeltasV1 (double deltaX, double deltaY, double deltaZ, double deltaClock,
                         double deltaVX, double deltaVY, double deltaVZ, double deltaVClock, Calendar timeStamp){
        lock.lock();
        try{
            this.deltaX= deltaX;
            this.deltaY= deltaY;
            this.deltaZ= deltaZ;
            this.deltaClock= deltaClock;
            this.deltaVX= deltaVX;
            this.deltaVY= deltaVY;
            this.deltaVZ= deltaVZ;
            this.deltaVClock= deltaVClock;
            this.timeStamp = timeStamp;
            this.velocityCode = 1;
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to obtain the time of the Long corrections of the satellite
     *
     * @return Calendar timestamp : Timestamp of the stored correction
     *
     * */
    Calendar getLongTimeStamp(){
        lock.lock();
        try{
            return this.timeStamp;
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to obtain the time of the reference time for Velocity corrections
     *
     * @return Calendar refTime : Reference time
     *
     * */
    Calendar getRefTime() {
        lock.lock();
        try {
            return this.refTime;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the current SBAS IODE of the sateellite
     *
     * @return int iode : The current IODE
     *
     * */
    int getSbasIode(){
        lock.lock();
        try {
            return this.iode;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the delta for the X position of a satellite
     *
     * @return double deltaX : The delta for the X position of the satellite
     *
     * */
    double getDeltaX(){
        lock.lock();
        try {
            return this.deltaX;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the delta for the Y position of a satellite
     *
     * @return double deltaY : The delta for the Y position of the satellite
     *
     * */
    double getDeltaY(){
        lock.lock();
        try {
            return this.deltaY;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the delta for the Z position of a satellite
     *
     * @return double deltaZ : The delta for the Z position of the satellite
     *
     * */
    double getDeltaZ(){
        lock.lock();
        try {
            return this.deltaZ;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the delta for the clock  of a satellite
     *
     * @return double clock : The delta for the clock of the satellite
     *
     * */
    double getDeltaClock(){
        lock.lock();
        try {
            return this.deltaClock;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the delta for the X velocity of a satellite
     *
     * @return double deltaX : The delta for the X velocity of the satellite
     *
     * */
    double getDeltaVX(){
        lock.lock();
        try {
            return this.deltaVX;
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to obtain the delta for the Y velocity of a satellite
     *
     * @return double deltaY : The delta for the Y velocity of the satellite
     *
     * */
    double getDeltaVY(){
        lock.lock();
        try {
            return this.deltaVY;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the delta for the Z velocity of a satellite
     *
     * @return double deltaZ : The delta for the Z velocity of the satellite
     *
     * */
    double getDeltaVZ(){
        lock.lock();
        try {
            return this.deltaVZ;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the delta for the clock velocity of a satellite
     *
     * @return double deltaClock : The delta for the clock velocity of the satellite
     *
     * */
    double getDeltaVClock(){
        lock.lock();
        try {
            return this.deltaVClock;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the velocity code (0 or 1) of the received corrections for a satelltie
     *
     * @return int velocityCode : The velocity code
     *
     * */
    double getVelocityCode(){
        lock.lock();
        try {
            return this.velocityCode;
        }
        finally {
            lock.unlock();
        }
    }
}
