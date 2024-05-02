package com.example.ELFA.edas.ntrip;

/**
 * A class that stores the decoded data from RTCM2_Message.
 */

public class MT1_Message {
    /** Contants */
    private final static int MAX_SATELLITES_MESSAGE = 18;
    private final static int DEFAULT_UDRE_VALUE = -1;
    private final static int DEFAULT_IOD_VALUE = -1;
    private final static double DEFAULT_PRC_VALUE = 10485.76;
    private final static double DEFAULT_RRC_VALUE = 4.096;

    /** Class member variables */
    private MT1_Sat[] arraySats;
    private int numberSats;
    private double mZCount;

    /** Auxiliary classes */
    public static class MT1_Sat {
        private int mUdre;
        private int mSatID;
        private double mPseudoRangeCorr;
        private double mRangeRateCorr;
        private int mIod;
    }

    /**
     * Class constructor
     */
    public MT1_Message() {
        arraySats = new MT1_Sat[MAX_SATELLITES_MESSAGE];
        numberSats = 0;
        mZCount = 0;
    }

    /**
     * Method to get udre value from a satellite. Valid range: 1-3.
     * In case the satellite is not in MT1 list, the method returns -1
     *@param satID: int satellite ID, from 1 to 32.
     *@return int udre: UDRE values of the satellites.
     */
    public int getUdre(int satID) {

        int udre = DEFAULT_UDRE_VALUE;

        for (int i = 0; i < numberSats; i++) {
            if (arraySats[i].mSatID == satID) {
                udre = arraySats[i].mUdre;
                break;
            }
        }
        return udre;
    }

    /**
     * Method to get the pseudorange correction value in meters from a satellite in MT1 list. Valid range -10485.44m - +10485.44m.
     * In case the satellite is not in MT1 list, the method returns 10485.76.
     *@param satID: int satellite ID, from 1 to 32.
     *@return double pseudoRangeCorr: pseudorange correction values of the satellites.
     */
    public double getPseudoRangeCorr(int satID) {

        double pseudoRangeCorr = DEFAULT_PRC_VALUE;

        for (int i = 0; i < numberSats; i++) {
            if (arraySats[i].mSatID == satID) {
                pseudoRangeCorr = arraySats[i].mPseudoRangeCorr;
                break;
            }
        }
        return pseudoRangeCorr;
    }

    /**
     * Method to get the range-rate correction value from a satellite in MT1 list. Valid range -4.064m - +4.064m.
     * In case the satellite is not in MT1 list, the method returns 4.096.
     *@param satID: int satellite ID, from 1 to 32.
     *@return double rangeRateCorr: Range-rate Correction values of the satellites.
     */
    public double getRangeRateCorr(int satID) {

        double rangeRateCorr = DEFAULT_RRC_VALUE;

        for (int i = 0; i < numberSats; i++) {
            if (arraySats[i].mSatID == satID) {
                rangeRateCorr = arraySats[i].mRangeRateCorr;
                break;
            }
        }
        return rangeRateCorr;
    }

    /**
     * Method to get the IOD value from a satellite included in MT1 list. Valid range 0 - 3.
     * In case the satellite is not in MT1 list, the method returns -1.
     *@param satID: int satellite ID, from 1 to 32.
     *@return int IOD: IOD values of the satellites.
     */
    public int getIod(int satID) {

        int iod = DEFAULT_IOD_VALUE;

        for (int i = 0; i < numberSats; i++) {
            if (arraySats[i].mSatID == satID) {
                iod = arraySats[i].mIod;
                break;
            }
        }
        return iod;
    }

    /**
     * Method to check if a particular PRN ID is included in MT1 list.
     * In case PRN is not included in MT1 list return False
     *@param satID: int satellite ID, from 1 to 32.
     *@return boolean: true for PRN ID satellite included in the PRN IDs of the message, false if not.
     */
    public boolean isSatIncluded(int satID) {
        for (int i = 0; i < numberSats; i++) {
            if (arraySats[i].mSatID == satID) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method to get the number of satellites included in the MT1 Message
     * @return int: number of satellites
     */
    public int getNumberSats(){
        return numberSats;
    }

    /**
     * Method to set the time stamp value  in seconds since first GPS epoch.
     * @param ZCountTime: double mZcount.
     */
    void setDecodedZCount(double ZCountTime) {
        mZCount=ZCountTime;
    }

    /**
     * Method to get the time stamp value  in seconds since first GPS epoch.
     * In case the variable has not been set returns 0.
     * @return double mZCount.
     */
    public double getDecodedZCount() {
        return mZCount;
    }

    /**
     * Method to add the decoded data of a particular satellite in a MT1_Sat object.
     *@param udre: Udre value of the satellite.
     *@param satID: Satellite ID of the satellite.
     *@param pseudoRangeCorr: Pseudorange Correction value of the satellite.
     *@param rangeRateCorr: Range-Rate Correction value of the satellite.
     *@param iod: IOD value of the satellite.
     */
    void addSatDecodedData(int udre, int satID, double pseudoRangeCorr,
                           double rangeRateCorr, int iod) {

        MT1_Sat messageSat = new MT1_Sat();
        messageSat.mUdre = udre;
        messageSat.mSatID = satID;
        messageSat.mPseudoRangeCorr = pseudoRangeCorr;
        messageSat.mRangeRateCorr = rangeRateCorr;
        messageSat.mIod = iod;

        if (numberSats < MAX_SATELLITES_MESSAGE) {
            arraySats[numberSats] = messageSat;
            numberSats++;
        }
    }

    /**
     * Method to get the MT1_Sat array with the information of all satellites included in the MT1 Message
     * Each MT1_Sat has the following members:
     *         int mUdre;
     *         int mSatID;
     *         double mPseudoRangeCorr;
     *         double mRangeRateCorr;
     *         int mIod;
     * In case the variable has not been set returns an empty list.
     * 
     * @return arraySats: array with the MT1_Sats information
     */
    public MT1_Sat[] getMT1_SatArray(){
        return arraySats;
    }
}

