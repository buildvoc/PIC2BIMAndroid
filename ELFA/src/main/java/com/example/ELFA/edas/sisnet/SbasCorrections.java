package com.example.ELFA.edas.sisnet;

import android.util.Log;
import android.util.Pair;

import com.example.ELFA.edas.utils.Coordinates;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SbasCorrections {

    private  HashMap<Integer, SbasFastCorrection> fastMap = new HashMap<Integer, SbasFastCorrection>();
    private  HashMap<Integer, SbasLongCorrection> longMap = new HashMap<Integer, SbasLongCorrection>();
    private int currentIodp;
    private Map<Integer, Integer> currentIodfs = new HashMap<>(); // < Message type (2 to 5), Iodf >
    private int satCounter = 0;
    private int[] satMask = new int[SisnetConstants.PRN_MASK];

    private Map<Coordinates, SbasIonoCorrection> ionoMap = new HashMap<>();
    private int currentIodi = -1;
    private Map<Integer, int[]> igpMask = new HashMap<>(); // < Band, mask >

    private final Lock lock = new ReentrantLock();

    /**
     * Constructor
     */
    public SbasCorrections() {

        for (int i = 0; i < SisnetConstants.BITS_PRN_MASK; i++) {
            fastMap.put(i + 1, new SbasFastCorrection());
            longMap.put(i + 1, new SbasLongCorrection());
        }
    }

    /**
     * Method to update the current iodp
     * @param iodp: Integer with the iodp
     */
    void updateIodp(int iodp) {
        this.currentIodp = iodp;
    }


    /**
     * Method to update the current iodf for a type 2-5 message
     * @param messageType: Integer with the message type (2 to 5)
     * @param iodf: Integer with the iodp
     */
    void updateIodf(int messageType, int iodf) {
        this.currentIodfs.put(messageType, iodf);
    }


    /**
     * Method to update the satellite mask
     * @param mask: String with the mask in binary
     */
    void updatePrnMask (String mask){
        satCounter = 0;
        for (int i = 0; i < SisnetConstants.BITS_PRN_MASK; i++) {
            if (mask.charAt(i) == '1') {
                this.satMask[satCounter] = i + 1;
                satCounter++;
            }
        }
    }


    /**
     * Method to update the satellite Range correction
     * @param satIndex: Integer with the index of the satellite in the mask
     * @param prc : The pseudo-range correction
     * @param timeStamp : The timestamp of the correction
     */
    void updateSatRangeCorrections (int satIndex, double prc, Calendar timeStamp) {
        fastMap.get(satMask[satIndex]).updateRangeCorrection(prc,timeStamp);
    };


    /** Method to set the UDRE of a satellite
     * @param satIndex: Integer with the index of the satellite in the mask
     * @param udre: Integer with the UDRE
     * */
    void updateSatUdre (int satIndex, int udre){
        fastMap.get(satMask[satIndex]).updateUdreI(udre);
    }


    /** Method to set the timestamp of an integrity correction (MT6)
     * @param satIndex: Integer with the index of the satellite in the mask
     * @param timeStamp: The timestamp of the integrity message
     * */
    void updateSatIntegrityTimeStamp (int satIndex, Calendar timeStamp){
        fastMap.get(satMask[satIndex]).updateIntegrityTimeStamp(timeStamp);
    }


    /** Method to set the Long Correctiosn of a Satellite from an Velocity code 1 message
     *
     * @param satIndex: Integer with the index of the satellite in the mask
     * @param deltaX : Double with the X position delta
     * @param deltaY : Double with the Y position delta
     * @param deltaZ : Double with the Z position delta
     * @param deltaClock : Double with the clock delta
     * @param timeStamp: Timestamp of the correction
     * */
    void updateSatDeltasV0 (int satIndex, double deltaX, double deltaY, double deltaZ, double deltaClock, Calendar timeStamp){
        longMap.get(satMask[satIndex]).updateDeltasV0(deltaX,deltaY,deltaZ,deltaClock,timeStamp);
    }


    /** Method to set the Long Correctiosn of a Satellite from an Velocity code 1 message
     *
     * @param satIndex: Integer with the index of the satellite in the mask
     * @param deltaX : Double with the X position delta
     * @param deltaY : Double with the Y position delta
     * @param deltaZ : Double with the Z position delta
     * @param deltaClock : Double with the clock delta
     * @param deltaVX : Double with the X  velocity delta
     * @param deltaVY : Double with the Y velocity delta
     * @param deltaVZ : Double with the Z velocity delta
     * @param deltaVClock : Double with the clock velocity delta
     * @param timeStamp: Timestamp of the correction
     * */
    void updateSatDeltasV1 (int satIndex, double deltaX, double deltaY, double deltaZ, double deltaClock,
                            double deltaVX, double deltaVY, double deltaVZ, double deltaVClock, Calendar timeStamp){
        longMap.get(satMask[satIndex]).updateDeltasV1(deltaX,deltaY,deltaZ,deltaClock,deltaVX,deltaVY,deltaVZ,deltaVClock,timeStamp);
    }


    /** Method to set the SBAS IODE corrections of a satellite
     * @param satIndex: Integer with the index of the satellite in the mask
     * @param iode: Integer with the IODE
     * */
    void updateSatIode (int satIndex, int iode){
        longMap.get(satMask[satIndex]).updateIode(iode);
    }


    /**
     * Method to update the current IODI
     * @param iodi: Integer with the IODI
     */
    void updateIodi(int iodi) {
        this.currentIodi = iodi;
    }


    /**
     * Method to update the IGP mask
     * @param band: Integer with the band the mask applies to
     * @param mask: String with the mask in binary
     */
    void updateIgpMask(int band, String mask){
        int igpCounter = 0;
        int[] bandMask = new int[SisnetConstants.IGP_MASK_SIZE];
        for (int i = 0; i < SisnetConstants.IGP_MASK_SIZE; i++) {
            if (mask.charAt(i) == '1') {
                bandMask[igpCounter] = i + 1;  //IGPs in mask numbered starting from 1
                igpCounter++;
            }
        }
        this.igpMask.put(band, bandMask);
    }


    /** Method to set the ionospheric corrections for an IGP (vertical delay,
     * GIVEI and timestamp of the correction)
     * @param igpCoords: Coordinates (longitude and latitude) of the IGP
     * @param vertDelay: Integer with the vertical delay
     * @param giveI: Integer with the GIVEI
     * @param timeStamp: Timestamp of the correction
     * */
    void updateIgpCorrection(Coordinates igpCoords, double vertDelay,
                             int giveI, Calendar timeStamp) {
        SbasIonoCorrection igpCorrection = ionoMap.get(igpCoords);
        if (igpCorrection != null) {
            igpCorrection.updateVertDelay(vertDelay);
            igpCorrection.updateGiveI(giveI);
            igpCorrection.updateTimeStamp(timeStamp);
        }
        else {
            igpCorrection = new SbasIonoCorrection(vertDelay, giveI, timeStamp);
            ionoMap.put(igpCoords, igpCorrection);
        }
    }


    /**
     * Method to obtain the index of the satellite in the SatArray from a correction position in a
     * specific message
     * @param messageType: Integer with the Message Type
     * @param position : Position of the measurement in the message
     * @return int index : Position of the satellite
     *
     */
    int getSatIndex(int messageType, int position) {
        int index = -1;
        if (messageType >= 2 && messageType <= 5) {
            index = (messageType - 2) * SisnetConstants.MAX_SATS_M_2_5 + position;
        }
        return index;
    }

    /** Method to obtain the Current Iodp
     *
     * @return int iodp: The current iodp
     *
     * */
    int getIodp () {
        return currentIodp;
    }

    /**
     * Method to obtain the current iodf for a specified message type (2 to 5)
     *
     * @param messageType : Integer with the message type
     *
     * @return Integer iodf: The current iodf for the specified message type
     */
    Integer getIodf(int messageType) {
        return currentIodfs.get(messageType);
    }

    /**
     * Method to get the number of sats in the mask
     * @return int satCounter: The number of Sats
     *
     */
    int getSatCounter (){
        return satCounter;
    }

    /** Method to obtain the current IODI
     *
     * @return int iodi: The current iodi
     *
     * */
    int getIodi () {
        return currentIodi;
    }

    /**
     * Method to obtain the coordinates of the IGP to which the corrections
     * in a type 26 message apply
     * @param band : Integer with IGP band
     * @param block : Integer with IGP block, according to IGP mask
     * @param position : Integer with position of the correction in the message
     * @return coords : Coordinates (longitude and latitude) of the IGP
     *
     */
    Coordinates getIgpCoords(int band, int block, int position) {
        int maskPosition = block * SisnetConstants.IGPS_PER_BLOCK + position;
        if (maskPosition >= SisnetConstants.IGP_MASK_SIZE) {
            return null;
        }
        int[] bandMask = igpMask.get(band);
        if (bandMask==null){
            return null;
        }else{
            int maskBit = bandMask[maskPosition];
            return getIgpCoordsFromIgpGrid(band, maskBit);
        }


    }


    /**
     * Method to obtain the coordinates of the IGP to which the corrections
     * in a type 26 message apply
     * @param band : Integer with IGP band
     * @param maskBit : Integer with bit of the mask corresponding to the IGP
     * @return coords : Coordinates (longitude and latitude) of the IGP
     *
     */
     Coordinates getIgpCoordsFromIgpGrid(int band, int maskBit) {

        if (maskBit >= 1 & maskBit <= SisnetConstants.IGP_MASK_SIZE) {
            Coordinates coords = SisnetConstants.IGP_GRID.get(new Pair<>(band, maskBit));
            return coords;
        }
        else {
            return null;
        }
    }

    /**
     * Method to obtain the last PRN mask received
     * @return int[] satMask : list with the PRNs included in the mask
     *
     */
    int[] getSatMask(){
        lock.lock();
        try{
            return satMask;
        }
        finally {
            lock.unlock();
        }
    }

    private boolean isSatInMask(int prn) {
        for (final int i : satMask) {
            if (prn == i) {
                return true;
            }
        }
        return false;
    }


    /** Method to obtain the PRC + RRC Ccorrection of a satellite, in meters. 
     * Valid range for PRC -256.0 - +255.875. RRC shall be discarded if time increment > 12 s 
     * In case the satellite is not in MT1 list, or the PRC or RRC has null values, the method returns 9999.
     *
     * @param prn: Integer with the PRN code of the satellite
     * @param currentTime : The timestamp of the measuremnent to which the correction will be applied
     * @return int correction: The calculated correction
     *
     * */
    public double getSatRangeCorrection (int prn, Calendar currentTime) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return fastMap.get(prn).getRangeCorrection(currentTime);
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return SisnetConstants.NULL_CODE;
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return SisnetConstants.NULL_CODE;
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to obtain the UDRE of a satellite. Valid range 1-15 
     * In case the satellite is not in MT1 list the method returns 9999.
     *
     * @param prn: Integer with the PRN code of the satellite
     * @return int udre: The calculated correction
     *
     * */
    public int getSatUdreI (int prn) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return fastMap.get(prn).getUdreI();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return SisnetConstants.NULL_CODE;
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return SisnetConstants.NULL_CODE;
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to obtain the current SBAS IODE of a satellite
     * In case the satellite is not in MT1 list the method returns 9999.
     *
     * @param prn: Integer with the PRN code of the satellite
     * @return int iode: The current IODE
     *
     * */
    public int getSatIode (int prn) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return longMap.get(prn).getSbasIode();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return SisnetConstants.NULL_CODE;
        }
        catch (RuntimeException exception) {
                Log.w("Sbas Corrections", "Exception while accessing correction");
                return SisnetConstants.NULL_CODE;
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to obtain the delta for the X position of a satellite in meters. Valid range +-32m
     * In case the satellite is not in MT1 list the method returns 9999.
     *
     * @param prn: Integer with the PRN code of the satellite
     * @return double deltaX : The delta for the X position of the satellite
     *
     * */
    public double getSatDeltaX (int prn) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return longMap.get(prn).getDeltaX();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return SisnetConstants.NULL_CODE;
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return SisnetConstants.NULL_CODE;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the delta for the Y position of a satellite in meters. Valid range +-32m
     * In case the satellite is not in MT1 list the method returns 9999.
     *
     * @param prn: Integer with the PRN code of the satellite
     * @return double deltaY : The delta for the Y position of the satellite
     *
     * */
    public double getSatDeltaY (int prn) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return longMap.get(prn).getDeltaY();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return SisnetConstants.NULL_CODE;
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return SisnetConstants.NULL_CODE;
        }
        finally {
            lock.unlock();
        }
    }
    /** Method to obtain the delta for the Z position of a satellite in meters. Valid range +-32m for vel_code = 0.
     * In case the satellite is not in MT1 list the method returns 9999.
     *
     * @param prn: Integer with the PRN code of the satellite
     * @return double deltaZ : The delta for the Z position of the satellite
     *
     * */
    public double getSatDeltaZ (int prn) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return longMap.get(prn).getDeltaZ();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return SisnetConstants.NULL_CODE;
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return SisnetConstants.NULL_CODE;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the delta for the clock of a satellite. Valid range +- 2^-22 for vel_code = 0.
     * In case the satellite is not in MT1 list the method returns 9999.
     *
     * @param prn: Integer with the PRN code of the satellite
     * @return double deltaClock : The delta for the clock of the satellite
     *
     * */
    public double getSatDeltaClock (int prn) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return longMap.get(prn).getDeltaClock();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return SisnetConstants.NULL_CODE;
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return SisnetConstants.NULL_CODE;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the delta for the X velocity of a satellite in m. Valid range +-0.0625 m.
     * In case the satellite is not in MT1 list the method returns 9999.
     *
     * @param prn: Integer with the PRN code of the satellite
     * @return double deltaVX : The delta for the X velocity of the satellite
     *
     * */
    public double getSatDeltaVX (int prn) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return longMap.get(prn).getDeltaVX();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return SisnetConstants.NULL_CODE;
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return SisnetConstants.NULL_CODE;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the delta for the Y velocity of a satellite in m. Valid range +-0.0625 m.
     * In case the satellite is not in MT1 list the method returns 9999.
     * 
     * @param prn: Integer with the PRN code of the satellite
     * @return double deltaVY : The delta for the Y velocity of the satellite
     *
     * */
    public double getSatDeltaVY (int prn) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return longMap.get(prn).getDeltaVY();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return SisnetConstants.NULL_CODE;
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return SisnetConstants.NULL_CODE;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the delta for the Z velocity of a satellite in m. Valid range +-0.0625 m.
     * In case the satellite is not in MT1 list the method returns 9999.
     *
     * @param prn: Integer with the PRN code of the satellite
     * @return double deltaVZ : The delta for the Z velocity of the satellite
     *
     * */
    public double getSatDeltaVZ (int prn) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return longMap.get(prn).getDeltaVZ();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return SisnetConstants.NULL_CODE;
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return SisnetConstants.NULL_CODE;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the delta for the Clock velocity of a satellite in s. Valid range +- 2^-32.
     * In case the satellite is not in MT1 list the method returns 9999.
     *
     * @param prn: Integer with the PRN code of the satellite
     * @return double deltaVClock : The delta for the clock velocity of the satellite
     *
     * */
    public double getSatDeltaVClock (int prn) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return longMap.get(prn).getDeltaVClock();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return SisnetConstants.NULL_CODE;
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return SisnetConstants.NULL_CODE;
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the Reference Time in seconds from a satellite long correction with velocity code 1
     * 
     * @param prn: Integer with the PRN code of the satellite
     * @return Calendar refTime : The reference time     *
     * */
    public Calendar getRefTime (int prn) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return longMap.get(prn).getRefTime();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return new GregorianCalendar();
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return new GregorianCalendar();
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the time of the Fast corrections of a satellite in Gregorian Calendar UTC date.
     * If corrections are outdated, it shall be discarded. Refer to MOPS for typical timed out values.
     * In case the time stamp has not been set, the method returns an empty GregorianCalendar object.
     * 
     * @param prn: Integer with the PRN code of the satellite
     * @return Calendar timestamp : Timestamp of the stored correction
     *
     * */
    public Calendar getSatFastTimestamp (int prn) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return fastMap.get(prn).getFastTimeStamp();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return new GregorianCalendar();
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return new GregorianCalendar();
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to obtain the time of the integrity corrections of a satellite in Gregorian Calendar UTC date.
     * If corrections are outdated, it shall be discarded. Refer to MOPS for typical timed out values.
     * In case the time stamp has not been set, the method returns an empty GregorianCalendar object.
     * 
     * @param prn: Integer with the PRN code of the satellite
     * @return Calendar timestamp : Timestamp of the stored correction
     *
     * */
    public Calendar getSatIntegrityTimestamp (int prn) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return fastMap.get(prn).getIntegrityTimeStamp();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return new GregorianCalendar();
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return new GregorianCalendar();
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to obtain the time of the Long corrections of a satellite in Gregorian Calendar UTC date.
     * If corrections are outdated, it shall be discarded. Refer to MOPS for typical timed out values.
     * In case the time stamp has not been set, the method returns an empty GregorianCalendar object.
     * 
     * @param prn: Integer with the PRN code of the satellite
     * @return Calendar timestamp : Timestamp of the stored correction
     *
     * */
    public Calendar getSatLongTimestamp(int prn) {
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return longMap.get(prn).getLongTimeStamp();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return new GregorianCalendar();
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return new GregorianCalendar();
        }
        finally {
            lock.unlock();
        }
    }

    /** Method to obtain the velocity code of the received corrections for a satellite. Valid values 0 / 1.
     * In case the satellite is not in MT1 list the method returns 9999.
     * 
     * @param prn: Integer with the PRN code of the satellite
     * @return int velocityCode : The velocity code
     *
     * */
    public double getSatVelocityCode (int prn){
        lock.lock();
        try {
            if (isSatInMask(prn)) {
                return longMap.get(prn).getVelocityCode();
            }
            Log.w("Sbas Corrections", "PRN out of mask");
            return SisnetConstants.NULL_CODE;
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return SisnetConstants.NULL_CODE;
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to obtain the vertical delay of an IGP. Valid range 0 - 63.875.
     * In case the IGP is not in MT18 list the method returns 9999.
     * 
     * @param longitude: Longitude of the IGP
     * @param latitude: Latitude of the IGP
     * @return int vertDelay : vertical delay of the IGP
     *
     * */
    public double getIgpVertDelay (int longitude, int latitude) {
        lock.lock();
        Coordinates igpCoords = new Coordinates(longitude, latitude);
        try {
            SbasIonoCorrection ionoCorrection = ionoMap.get(igpCoords);
            if (ionoCorrection != null) {
                return ionoCorrection.getVertDelay();
            }
            Log.w("Sbas Corrections", "No corrections found for IGP");
            return SisnetConstants.NULL_CODE;
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return SisnetConstants.NULL_CODE;
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to obtain the GIVE of an IGP. Valid range 0 - 15 
     * In case the IGP is not in MT18 list the method returns 9999.
     * 
     * @param longitude: Longitude of the IGP
     * @param latitude: Latitude of the IGP
     * @return int giveI : GIVEI of the IGP
     *
     * */
    public int getIgpGiveI (int longitude, int latitude) {
        lock.lock();
        Coordinates igpCoords = new Coordinates(longitude, latitude);
        try {
            SbasIonoCorrection ionoCorrection = ionoMap.get(igpCoords);
            if (ionoCorrection != null) {
                return ionoCorrection.getGiveI();
            }
            Log.w("Sbas Corrections", "No corrections found for IGP");
            return SisnetConstants.NULL_CODE;
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return SisnetConstants.NULL_CODE;
        }
        finally {
            lock.unlock();
        }
    }


    /** Method to obtain the time of the ionospheric corrections of an IGP in Gregorian Calendar UTC date.
     * If corrections are outdated, it shall be discarded. Refer to MOPS for typical timed out values.
     * In case the time stamp has not been set, the method returns an empty GregorianCalendar object.
     * 
     * @param longitude: Longitude of the IGP
     * @param latitude: Latitude of the IGP
     * @return Calendar timestamp : Timestamp of the stored correction
     *
     * */
    public Calendar getIgpIonoTimestamp(int longitude, int latitude) {
        lock.lock();
        Coordinates igpCoords = new Coordinates(longitude, latitude);
        try {
            SbasIonoCorrection ionoCorrection = ionoMap.get(igpCoords);
            if (ionoCorrection != null) {
                return ionoCorrection.getIonoTimestamp();
            }
            Log.w("Sbas Corrections", "No corrections found for IGP");
            return new GregorianCalendar();
        }
        catch (RuntimeException exception) {
            Log.w("Sbas Corrections", "Exception while accessing correction");
            return new GregorianCalendar();
        }
        finally {
            lock.unlock();
        }
    }

}
