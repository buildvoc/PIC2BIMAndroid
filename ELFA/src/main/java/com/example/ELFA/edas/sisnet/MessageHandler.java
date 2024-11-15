package com.example.ELFA.edas.sisnet;

import android.util.Log;

import com.example.ELFA.edas.staticfunctions.BinaryFunctions;
import com.example.ELFA.edas.utils.Coordinates;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;


public class MessageHandler {

    private SbasCorrections mSbasCorrections;

    private SimpleDateFormat df;

    public MessageHandler(SbasCorrections sbasCorrections) {
        mSbasCorrections = sbasCorrections;
    }

    /**
     * Method to process EGNOS Message type 0
     *
     * @param datafield: String with the data field from the message
     * @param timeStamp: Date of the message
     */
    private void processMessage0(String datafield, Calendar timeStamp) {

        boolean hasContent = false;

        for (int i = 0; i < SisnetConstants.MESSAGE_SIZE; i++) {

            if (datafield.charAt(i) == '1') {
                hasContent = true;
                break;
            }
            if (hasContent) {
                //Process it as a Message type 2
                processMessage2to5(datafield, 2, timeStamp);
            }
            else {
                // Log message in format:
                // Raw;Time_utc;MsgType;Data
                String logMessage = "Raw" + ";"
                        + df.format(timeStamp.getTime()) + ";"
                        + 0 + ";"
                        + datafield;
                Log.d("SBAS Message", logMessage);
            }
        }
    }


    /**
     * Method to process EGNOS Message type 1
     *
     * @param datafield: String with the data field from the message
     * @param timeStamp: Datetime of the message
     */
    private void processMessage1(String datafield, Calendar timeStamp) {
        mSbasCorrections.updatePrnMask(datafield.substring(0, SisnetConstants.BITS_PRN_MASK));
        mSbasCorrections.updateIodp(BinaryFunctions.convertToInteger(datafield.substring(SisnetConstants.MASK_SIZE_M1,
                SisnetConstants.MASK_SIZE_M1 + SisnetConstants.IODP_SIZE)));

        // Log message in format:
        // Mask;Time_utc;MsgType;[PRN_active_1,PRN_active_2,...,PRN_active_N]
        int[] satMask = mSbasCorrections.getSatMask();
        String logMessage = "Mask" + ";"
                + df.format(timeStamp.getTime()) + ";"
                + 1 + ";"
                + Arrays.toString(satMask);
        Log.d("SBAS Message", logMessage);
    }


    /**
     * Method to process EGNOS Message type 2-5
     *
     * @param datafield:   String with the data field from the message
     * @param messageType: Integer with the message type
     * @param timeStamp:   Datetime of the message
     */
    private void processMessage2to5(String datafield, int messageType, Calendar timeStamp) {

        int fastCoStart = 0;
        int fastCoEnd = 0;
        int udreStart = 0;
        int udreEnd = 0;
        int satIndex = 0;
        int iodf;
        int iodp;
        double prc = 0;
        int udre = 0;

        int iodfStart = 0;
        int iodfEnd = iodfStart + SisnetConstants.IODF_SIZE;
        iodf = BinaryFunctions.convertToInteger(datafield.substring(iodfStart, iodfEnd));
        mSbasCorrections.updateIodf(messageType, iodf);

        int iodpStart = SisnetConstants.IODF_SIZE;
        int iodpEnd = iodpStart + SisnetConstants.IODP_SIZE;
        iodp = BinaryFunctions.convertToInteger(datafield.substring(iodpStart, iodpEnd));
        if (iodp != mSbasCorrections.getIodp()) {
            return;
        }

        for (int i = 0; i < SisnetConstants.MAX_SATS_M_2_5; i++) {

            fastCoStart = SisnetConstants.IODP_SIZE + SisnetConstants.IODF_SIZE + SisnetConstants.FASTCO_SIZE * i;
            fastCoEnd = fastCoStart + SisnetConstants.FASTCO_SIZE;
            udreStart = SisnetConstants.IODP_SIZE + SisnetConstants.IODF_SIZE + SisnetConstants.FASTCO_SIZE * SisnetConstants.MAX_SATS_M_2_5
                    + SisnetConstants.UDRE_SIZE * i;
            udreEnd = udreStart + SisnetConstants.UDRE_SIZE;

            satIndex = mSbasCorrections.getSatIndex(messageType, i);
            if (satIndex >= 0 && satIndex < mSbasCorrections.getSatCounter()) {

                prc = SisnetConstants.PRC_SCALEFACTOR * BinaryFunctions.complement2toInteger(datafield.substring(fastCoStart, fastCoEnd));
                udre = BinaryFunctions.convertToInteger(datafield.substring(udreStart, udreEnd));

                mSbasCorrections.updateSatRangeCorrections(satIndex, prc, timeStamp);
                mSbasCorrections.updateSatUdre(satIndex, udre);

                // Log message in format:
                // FastCorrection;Time_utc;MsgType;PRN;PRC;UdreI
                int[] satMask = mSbasCorrections.getSatMask();
                String logMessage = "FastCorrection" + ";"
                        + df.format(timeStamp.getTime()) + ";"
                        + messageType + ";"
                        + satMask[satIndex] + ";"
                        + prc + ";"
                        + udre;
                Log.d("SBAS Message", logMessage);
            }
        }
    }


    /**
     * Method to process EGNOS Message type 6
     *
     * @param datafield: String with the data field from the message
     * @param timeStamp: Datetime of the message
     */
    private void processMessage6(String datafield, Calendar timeStamp) {

        int iodfStart;
        int iodfEnd;
        int udreBlockStart;
        int iodf;
        for (int i = 0; i < SisnetConstants.NUM_FC_MESSAGES; i++) {
            iodfStart = i * SisnetConstants.IODF_SIZE;
            iodfEnd = iodfStart + SisnetConstants.IODF_SIZE;
            iodf = BinaryFunctions.convertToInteger(datafield.substring(iodfStart, iodfEnd));
            // If IODFj indicates alert condition, UDREI corrections in MT6 apply to all PRNs in MTj, independently of IODF in MTj
            if (iodf != SisnetConstants.IODF_ALERT_CONDITION) {
                // First IODF corresponds to MT2 and so on (i=0-> MT=2, 1-> 3, 2-> 4, 3-> 5)
                if (iodf != mSbasCorrections.getIodf(i + 2)) {
                    return;
                }
            }

            int numSats;
            if (i != (SisnetConstants.NUM_FC_MESSAGES - 1)) {
                numSats = SisnetConstants.MAX_SATS_M_2_5;
            } else {
                numSats = SisnetConstants.MAX_SATS_M_2_5 - 1; // MT5 last correction is always empty due to max number of PRNs on maks
            }

            udreBlockStart = SisnetConstants.NUM_FC_MESSAGES * SisnetConstants.IODF_SIZE + i * SisnetConstants.UDRE_SIZE * SisnetConstants.MAX_SATS_M_2_5;
            for (int j = 0; j < numSats; j++) {
                // First block corresponds to MT2 and so on (i=0-> MT=2, 1-> 3, 2-> 4, 3-> 5)
                int satIndex = mSbasCorrections.getSatIndex(i + 2, j);
                if (satIndex >= 0 && satIndex < mSbasCorrections.getSatCounter()) {
                    int udreStart = udreBlockStart + j * SisnetConstants.UDRE_SIZE;
                    int udreEnd = udreStart + SisnetConstants.UDRE_SIZE;
                    int udre = BinaryFunctions.convertToInteger(datafield.substring(udreStart, udreEnd));
                    mSbasCorrections.updateSatUdre(satIndex, udre);
                    mSbasCorrections.updateSatIntegrityTimeStamp(satIndex, timeStamp);

                    // Log message in format:
                    // FastCorrection;Time_utc;MsgType;PRN;PRC;UdreI
                    int[] satMask = mSbasCorrections.getSatMask();
                    int prn = satMask[satIndex];
                    String logMessage = "FastCorrection" + ";"
                            + df.format(timeStamp.getTime()) + ";"
                            + "6" + ";"
                            + prn + ";"
                            + mSbasCorrections.getSatRangeCorrection(prn, mSbasCorrections.getSatFastTimestamp(prn)) + ";"
                            + udre;
                    Log.d("SBAS Message", logMessage);
                }
            }
        }
    }


    /**
     * Method to process The LONG Corrections for a PRN contained in a message 24/25
     *
     * @param correctionData : String with the data of the correction (in binary)
     * @param timeStamp      : Datetime of the message
     * @param messageType    : Integer with the message type
     */
    private void processLongData(String correctionData, Calendar timeStamp, int messageType) {

        //Check velocity code
        if (correctionData.charAt(0) == '0') {

            for (int i = 0; i < SisnetConstants.MAX_SATS_LONG; i++) {
                int prnMaskStart = SisnetConstants.VELOCITY_SIZE_M25 + i * (SisnetConstants.MASK_SIZE_LONG + SisnetConstants.IOD_SIZE_LONG +
                        SisnetConstants.SIGMA_XYZ_SIZE_V0 * 3 + SisnetConstants.SIGMA_A_SIZE_V0);
                int prnMaskEnd = prnMaskStart + SisnetConstants.MASK_SIZE_LONG;
                int prnMask = BinaryFunctions.convertToInteger(correctionData.substring(prnMaskStart, prnMaskEnd));
                if (prnMask == 0) {
                    continue;
                }
                int satIndex = prnMask - 1;

                if (satIndex < mSbasCorrections.getSatCounter()) {

                    int iodStart = prnMaskEnd;
                    int iodEnd = iodStart + SisnetConstants.IOD_SIZE_LONG;
                    int iod = BinaryFunctions.convertToInteger(correctionData.substring(iodStart, iodEnd));
                    int deltaXStart = iodEnd;

                    int deltaXEnd = deltaXStart + SisnetConstants.SIGMA_XYZ_SIZE_V0;

                    int deltaYStart = deltaXEnd;
                    int deltaYEnd = deltaYStart + SisnetConstants.SIGMA_XYZ_SIZE_V0;
                    int deltaZStart = deltaYEnd;
                    int deltaZEnd = deltaZStart + SisnetConstants.SIGMA_XYZ_SIZE_V0;
                    int deltaClockStart = deltaZEnd;
                    int deltaClockEnd = deltaClockStart + SisnetConstants.SIGMA_A_SIZE_V0;
                    double deltaX = SisnetConstants.SIGMA_XYZ_V0_SCALEFACTOR *
                            BinaryFunctions.complement2toInteger(correctionData.substring(deltaXStart, deltaXEnd));
                    double deltaY = SisnetConstants.SIGMA_XYZ_V0_SCALEFACTOR *
                            BinaryFunctions.complement2toInteger(correctionData.substring(deltaYStart, deltaYEnd));
                    double deltaZ = SisnetConstants.SIGMA_XYZ_V0_SCALEFACTOR *
                            BinaryFunctions.complement2toInteger(correctionData.substring(deltaZStart, deltaZEnd));
                    double deltaClock = SisnetConstants.SIGMA_A_V0_SCALEFACTOR *
                            BinaryFunctions.complement2toInteger(correctionData.substring(deltaClockStart, deltaClockEnd));
                    mSbasCorrections.updateSatDeltasV0(satIndex, deltaX, deltaY, deltaZ, deltaClock, timeStamp);
                    mSbasCorrections.updateSatIode(satIndex, iod);

                    // Log message in format:
                    // LongCorrection;Time_utc;MsgType;PRN;IODE;DeltaX;DeltaY;DeltaZ;DeltaClock
                    int[] satMask = mSbasCorrections.getSatMask();
                    String logMessage = "LongCorrection" + ";"
                            + df.format(timeStamp.getTime()) + ";"
                            + messageType + ";"
                            + satMask[satIndex] + ";"
                            + iod + ";"
                            + deltaX + ";"
                            + deltaY + ";"
                            + deltaZ + ";"
                            + deltaClock;
                    Log.d("SBAS Message", logMessage);
                }
            }
        }
    }


    /**
     * Method to process EGNOS Message type 18
     *
     * @param datafield: String with the data field from the message
     * @param timeStamp: Datetime of the message
     */
    private void processMessage18(String datafield, Calendar timeStamp) {

        int bandStart = SisnetConstants.NUM_BAND_SIZE;
        int bandEnd = bandStart + SisnetConstants.BAND_SIZE;
        int band = BinaryFunctions.convertToInteger(datafield.substring(bandStart, bandEnd));

        int iodiStart = bandEnd;
        int iodiEnd = iodiStart + SisnetConstants.IODI_SIZE;
        mSbasCorrections.updateIodi(BinaryFunctions.convertToInteger(datafield.substring(iodiStart, iodiEnd)));

        int maskStart = iodiEnd;
        int maskEnd = maskStart + SisnetConstants.IGP_MASK_SIZE;
        mSbasCorrections.updateIgpMask(band, datafield.substring(maskStart, maskEnd));

        // Log message in format:
        // IonoMask;Time_utc;MsgType;Band;Mask
        String logMessage = "IonoMask" + ";"
                + df.format(timeStamp.getTime()) + ";"
                + 18 + ";"
                + band + ";"
                + datafield.substring(maskStart, maskEnd);
        Log.d("SBAS Message", logMessage);
    }


    /**
     * Method to process EGNOS Message type 24
     *
     * @param datafield: String with the data field from the message
     * @param timeStamp: Datetime of the message
     */
    private void processMessage24(String datafield, Calendar timeStamp) {

        //Long Part
        int longStart = SisnetConstants.FASTCO_SIZE * SisnetConstants.MAX_SATS_M_24 +
                SisnetConstants.UDRE_SIZE * SisnetConstants.MAX_SATS_M_24 + SisnetConstants.IODP_SIZE + SisnetConstants.BLOCK_SIZE_M24 + SisnetConstants.IODF_SIZE + SisnetConstants.SPARE_SIZE_M24;
        int longEnd = longStart + SisnetConstants.TOTAL_SIZE_LONG;
        processLongData(datafield.substring(longStart, longEnd), timeStamp, 24);

        // FastCo Half
        int fastCoStart = 0;
        int fastCoEnd = 0;
        int udreStart = 0;
        int udreEnd = 0;
        int satIndex = 0;
        int iodpStart = SisnetConstants.FASTCO_SIZE * SisnetConstants.MAX_SATS_M_24 + SisnetConstants.UDRE_SIZE * SisnetConstants.MAX_SATS_M_24;
        int iodpEnd = iodpStart + SisnetConstants.IODP_SIZE;
        int blockIdStart = iodpEnd;
        int blockIdEnd = blockIdStart + SisnetConstants.BLOCK_SIZE_M24;
        int iodfStart = blockIdEnd;
        int iodfEnd = iodfStart + SisnetConstants.IODF_SIZE;

        int iodp;
        int iodf;

        int blockId;
        double prc = 0;
        int udre = 0;

        iodp = BinaryFunctions.convertToInteger(datafield.substring(iodpStart, iodpEnd));
        if (iodp != mSbasCorrections.getIodp()) {
            return;
        }

        blockId = BinaryFunctions.convertToInteger(datafield.substring(blockIdStart, blockIdEnd));

        iodf = BinaryFunctions.convertToInteger(datafield.substring(iodfStart, iodfEnd));
        // We use the BLOCK ID to determine which message is being replaced (0->2, 1-> 3, 2-> 4, 3->5)
        mSbasCorrections.updateIodf(blockId + 2, iodf);

        for (int i = 0; i < SisnetConstants.MAX_SATS_M_24; i++) {

            // We use the BLOCK ID to determine which message is being replaced (0->2, 1-> 3, 2-> 4, 3->5)
            satIndex = mSbasCorrections.getSatIndex(blockId + 2, i);
            if (satIndex >= 0 && satIndex < mSbasCorrections.getSatCounter()) {
                fastCoStart = i * SisnetConstants.FASTCO_SIZE;
                fastCoEnd = fastCoStart + SisnetConstants.FASTCO_SIZE;
                udreStart = SisnetConstants.FASTCO_SIZE * SisnetConstants.MAX_SATS_M_24 + i * SisnetConstants.UDRE_SIZE;
                udreEnd = udreStart + SisnetConstants.UDRE_SIZE;
                prc = SisnetConstants.PRC_SCALEFACTOR * BinaryFunctions.complement2toInteger(datafield.substring(fastCoStart, fastCoEnd));
                udre = BinaryFunctions.convertToInteger(datafield.substring(udreStart, udreEnd));
                mSbasCorrections.updateSatRangeCorrections(satIndex, prc, timeStamp);
                mSbasCorrections.updateSatUdre(satIndex, udre);

                // Log message in format:
                // FastCorrection;Time_utc;MsgType;PRN;PRC;UdreI
                int[] satMask = mSbasCorrections.getSatMask();
                String logMessage = "FastCorrection" + ";"
                        + df.format(timeStamp.getTime()) + ";"
                        + "24" + ";"
                        + satMask[satIndex] + ";"
                        + prc + ";"
                        + udre;
                Log.d("SBAS Message", logMessage);
            }
        }
    }


    /**
     * Method to process EGNOS Message type 25
     *
     * @param datafield: String with the data field from the message
     * @param timeStamp: Datetime of the message
     */
    private void processMessage25(String datafield, Calendar timeStamp) {
        //First half
        int longStart = 0;
        int longEnd = longStart + SisnetConstants.TOTAL_SIZE_LONG;
        processLongData(datafield.substring(longStart, longEnd), timeStamp, 25);

        //Second half
        longStart = longEnd;
        longEnd = longStart + SisnetConstants.TOTAL_SIZE_LONG;
        processLongData(datafield.substring(longStart, longEnd), timeStamp, 25);
    }


    /**
     * Method to process EGNOS Message type 26
     *
     * @param datafield: String with the data field from the message
     * @param timeStamp: Datetime of the message
     */
    private void processMessage26(String datafield, Calendar timeStamp) {

        int iodiStart = SisnetConstants.BAND_SIZE + SisnetConstants.BLOCK_SIZE
                + SisnetConstants.IGPS_PER_BLOCK * (SisnetConstants.VERT_DELAY_SIZE + SisnetConstants.GIVEI_SIZE);
        int iodiEnd = iodiStart + SisnetConstants.IODI_SIZE;
        int iodi = BinaryFunctions.convertToInteger(datafield.substring(iodiStart, iodiEnd));
        if (iodi != mSbasCorrections.getIodi()) {
            return;
        }

        int bandStart = 0;
        int bandEnd = bandStart + SisnetConstants.BAND_SIZE;
        int band = BinaryFunctions.convertToInteger(datafield.substring(bandStart, bandEnd));

        int blockStart = bandEnd;
        int blockEnd = blockStart + SisnetConstants.BLOCK_SIZE;
        int block = BinaryFunctions.convertToInteger(datafield.substring(blockStart, blockEnd));

        int vertDelayStart = 0;
        int vertDelayEnd = 0;
        int giveiStart = 0;
        int giveiEnd = 0;
        double vertDelay = 0;
        int givei = 0;
        Coordinates igpCoords;
        for (int i = 0; i < SisnetConstants.IGPS_PER_BLOCK; i++) {
            vertDelayStart = blockEnd + (SisnetConstants.VERT_DELAY_SIZE + SisnetConstants.GIVEI_SIZE) * i;
            vertDelayEnd = vertDelayStart + SisnetConstants.VERT_DELAY_SIZE;
            giveiStart = vertDelayEnd;
            giveiEnd = giveiStart + SisnetConstants.GIVEI_SIZE;

            vertDelay = SisnetConstants.VERT_DELAY_SCALEFACTOR * BinaryFunctions.convertToInteger(datafield.substring(vertDelayStart, vertDelayEnd));
            givei = BinaryFunctions.convertToInteger(datafield.substring(giveiStart, giveiEnd));

            igpCoords = mSbasCorrections.getIgpCoords(band, block, i);
            if (igpCoords == null) {
                continue;
            }
            mSbasCorrections.updateIgpCorrection(igpCoords, vertDelay, givei, timeStamp);

            // Log message in format:
            // IonoCorrection;Time_utc;MsgType;Lon_IGP;Lat_IGP;IGP_VertDelay;GiveI
            int[] satMask = mSbasCorrections.getSatMask();
            String logMessage = "IonoCorrection" + ";"
                    + df.format(timeStamp.getTime()) + ";"
                    + "26" + ";"
                    + igpCoords.getLongitude() + ";"
                    + igpCoords.getLatitude() + ";"
                    + vertDelay + ";"
                    + givei;
            Log.d("SBAS Message", logMessage);
        }
    }

    /**
     * Method to process EGNOS Messages for which a particular processing hasn't been defined
     * @param datafield   : String with the data field from the message
     * @param messageType : Integer with the message type
     * @param timeStamp   : Datetime of the message
     */
    private void processMessageDefault(String datafield, int messageType, Calendar timeStamp) {
        // Log message in format:
        // Raw;Time_utc;MsgType;Data
        String logMessage = "Raw" + ";"
                + df.format(timeStamp.getTime()) + ";"
                + messageType + ";"
                + datafield;
        Log.d("SBAS Message", logMessage);
    }

    /**
     * Method to handle the SBAS Message depending of its type
     *
     * @param message : The SBAS message to process
     */
    public void handleMessage(SbasMessage message) {

        if (df == null) {
            df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            df.setTimeZone(message.getCalendar().getTimeZone());
        }

        switch (message.getType()) {
            case 0:
                processMessage0(message.getDatafield(), message.getCalendar());
                break;
            case 1:
                processMessage1(message.getDatafield(), message.getCalendar());
                break;
            case 2:
            case 3:
            case 4:
            case 5:
                processMessage2to5(message.getDatafield(), message.getType(), message.getCalendar());
                break;
            case 6:
                processMessage6(message.getDatafield(), message.getCalendar());
                break;
            case 18:
                processMessage18(message.getDatafield(), message.getCalendar());
                break;
            case 24:
                processMessage24(message.getDatafield(), message.getCalendar());
                break;
            case 25:
                processMessage25(message.getDatafield(), message.getCalendar());
                break;
            case 26:
                processMessage26(message.getDatafield(), message.getCalendar());
                break;
            default:
                processMessageDefault(message.getDatafield(), message.getType(), message.getCalendar());
                break;
        }
        return;
    }
}

