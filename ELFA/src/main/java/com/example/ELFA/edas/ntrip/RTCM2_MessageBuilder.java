package com.example.ELFA.edas.ntrip;

import android.util.Log;

import com.example.ELFA.edas.ClientThread;
import com.example.ELFA.edas.staticfunctions.BinaryFunctions;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.concurrent.TimeUnit;
/**
 * Class to process the bytes received in the device from the NTRIP caster for build RTCM2 Messages.
 */
public class RTCM2_MessageBuilder {
    /** Constants required for message decoding */
    private final static int MSG_SAT_SIZE = 40;
    private final static int SIZE_WORD_1_2 = 48;
    private final static int FIRST_SCL_FACTOR_BIT = SIZE_WORD_1_2+1;
    private final static int FIRST_UDRE_BIT = SIZE_WORD_1_2+2;
    private final static int LAST_UDRE_BIT = SIZE_WORD_1_2+3;
    private final static int FIRST_SAT_ID_BIT = SIZE_WORD_1_2+4;
    private final static int LAST_SAT_ID_BIT = SIZE_WORD_1_2+8;
    private final static int FIRST_PSEUDORANGE_BIT = SIZE_WORD_1_2+9;
    private final static int LAST_PSEUDORANGE_BIT = SIZE_WORD_1_2+24;
    private final static int FIRST_RANGERATE_BIT = SIZE_WORD_1_2+25;
    private final static int LAST_RANGERATE_BIT = SIZE_WORD_1_2+32;
    private final static int FIRST_IOD_BIT = SIZE_WORD_1_2+33;
    private final static int LAST_IOD_BIT = SIZE_WORD_1_2+40;
    private final static double CORRECTION_PRC_TYPE0=0.02;
    private final static double CORRECTION_PRC_TYPE1=0.32;
    private final static double CORRECTION_RRC_TYPE0=0.002;
    private final static double CORRECTION_RRC_TYPE1=0.032;
    private final static int FIRST_ZCOUNT_BIT = 25-1;
    private final static int LAST_ZCOUNT_BIT = 37;
    private final static double Z_COUNT_SCALE_FACTOR=0.6;
    private final static int SECONDS_IN_HOUR=3600;
    private final static int HALF_HOUR_IN_SECONDS=1800;
    // GPS epoch is 1980/01/06
    public static final long GPS_DAYS_SINCE_JAVA_EPOCH = 3657;
    public static final long GPS_UTC_EPOCH_OFFSET_SECONDS =
            TimeUnit.DAYS.toSeconds(GPS_DAYS_SINCE_JAVA_EPOCH);
    public static final long GPS_UTC_EPOCH_OFFSET_NANOS = TimeUnit.SECONDS.toNanos(GPS_UTC_EPOCH_OFFSET_SECONDS);

    /** Constants required for message building */
    private final int LENGHT_FOR_PREAMBLE = 12;

    /** Class member variables */
    private String mCurrentWord;
    private RTCM2_Message mCurrentMessage;
    private ClientThread mClientThread;

    /**
     * Constructor
     * @param clientThread Reference to client thread instance
     */
    public RTCM2_MessageBuilder(ClientThread clientThread){
        mClientThread = clientThread;
        mCurrentWord = "";
        mCurrentMessage = new RTCM2_Message();
    }

    /**
     * Method to check that received bytes belong to a preamble of a valid message.
     * @return boolean isValidPreamble: return true if bytes match with a valid preamble (01100110 or 10011001)
     */
    private boolean checkPreamble() {
        boolean isValidPreamble = false;
        if (mCurrentWord.length() >= LENGHT_FOR_PREAMBLE) {
            if (((mCurrentWord.charAt(0) == '0') &&
                    (mCurrentWord.charAt(1) == '1') &&
                    (mCurrentWord.charAt(2) == '1') &&
                    (mCurrentWord.charAt(3) == '0') &&
                    (mCurrentWord.charAt(4) == '0') &&
                    (mCurrentWord.charAt(5) == '1') &&
                    (mCurrentWord.charAt(6) == '1') &&
                    (mCurrentWord.charAt(7) == '0')) ||
                    ((mCurrentWord.charAt(0) == '1') &&
                            (mCurrentWord.charAt(1) == '0') &&
                            (mCurrentWord.charAt(2) == '0') &&
                            (mCurrentWord.charAt(3) == '1') &&
                            (mCurrentWord.charAt(4) == '1') &&
                            (mCurrentWord.charAt(5) == '0') &&
                            (mCurrentWord.charAt(6) == '0') &&
                            (mCurrentWord.charAt(7) == '1'))) {
                isValidPreamble = true;
            }
        }
        return isValidPreamble;
    }

    /**
     * Method to check if the byte received is in complement 1
     * @return boolean is Complement1: return true if the byte received is in complement 1 looking in the first bit.
     */
    private boolean checkComplement1 () {
        boolean isComplement1;
        if (mCurrentMessage.getWordCounter() == 0) {
            // If it is the first word, we look at the first bit of the preamble in order to check if it is in complement 1
            isComplement1 = mCurrentWord.charAt(0) == '1';
        } else {
            // Otherwise, we must check last bit from previous word to check if it is in complement 1
            isComplement1 = mCurrentMessage.getLastBit30() == '1';
        }
        return isComplement1;
    }


    /**
     * Method to check if the byte received belongs to RTCM 2.3 format.
     * @param messageByte: Byte received in the device from the caster.
     * @return boolean: return true if the byte received belongs to RTCM 2.3 format.
     */
    private boolean isRTCM23Byte (Byte messageByte) {
        String auxString = String.format("%8s", Integer.toBinaryString(messageByte & 0xFF)).replace(' ', '0');
        return ((auxString.charAt(0) == '0') && (auxString.charAt(1) == '1'));
    }

    /**
     * Method to extract the useful bits of a received byte (Useful bits: 3-8;Useless bits:1-2)
     * @param messageByte: Byte received in the device from the caster.
     * @return String byteString: return the six useful bits from the received byte.
     */
    private String extractByteString(Byte messageByte) {
        String auxString = String.format("%8s", Integer.toBinaryString(messageByte & 0xFF)).replace(' ', '0');
        // Construct the string with bits last to third in inverse order;
        return String.valueOf(auxString.charAt(7)) + auxString.charAt(6) +
                auxString.charAt(5) + auxString.charAt(4) +
                auxString.charAt(3) + auxString.charAt(2);
    }

    /**
     * Method to discard the byte received in order to find a valid preamble
     */
    private void discardFirstByte() {
        mCurrentWord = mCurrentWord.substring(6, 12);
        Log.d("DEA: ","Preamble discarded");
    }

    /**
     * Method that contains the logic of the message processor.
     * @param nextByte: Byte received in the device from the caster.
     */
    public void byteProcessing(Byte nextByte) {
        String byteString;
        // If the byte does not match RTCM2.3 format, discard
        if (isRTCM23Byte(nextByte)) {
            // Extract the useful bits
            byteString = extractByteString(nextByte);
            mCurrentWord= mCurrentWord.concat(byteString);

            // Condition to find a valid preamble
            if ((mCurrentMessage.getWordCounter() == 0) && (mCurrentWord.length() == LENGHT_FOR_PREAMBLE)) {
                if (!checkPreamble()) {
                    discardFirstByte();
                }
            }

            int WORD_LENGTH = 30;
            if (mCurrentWord.length() == WORD_LENGTH) {
                // Adding a word to create a valid message
                mCurrentMessage.addWord(mCurrentWord,checkComplement1());
                if (mCurrentMessage.isComplete()) {
                    if (mCurrentMessage.getMsgType() == RTCM2_Message.MSG_TYPE_1) {
                        // Decoding a valid MT1
                        decodeMessage();
                    }
                    // Start with an empty message for the next one
                    mCurrentMessage = new RTCM2_Message();
                }
                mCurrentWord = "";
            }
        }
    }

    /**
     * Method to decode a RTCM2 valid message
     */
    private void decodeMessage(){
        int scaleFactor;
        int udre;
        int satID;
        double pseudoRangeCorr;
        int pseudoRangeCorrSign;
        double rangeRateCorr;
        int rangeRateCorrSign;
        int iod;
        double zCount;
        double timeZCountInSec;

        /*
          The RTCM2 MT1 Message contains data for all satellites in view of the reference station (Ns). Since 40
          bits are required for the corrections from each satellite, there won't always be an exact integer
          number of words required.

          Message type 1 content includes 24 bits of word 1, 24 bits of word 2 and 40 bits (formed with various words) of each satellite.

          Word 1 (24 bits): /1/2/3/4/5/6/7/8/ /9/10/11/12/13/14/ /15/16/17/18/19/20/21/21/22/23/24/
                                  Preamble         Msg. Type                  Station ID

          Word 2 (24 bits): /1/2/3/4/5/6/7/8/9/10/11/12/13/ /14/15/16/ /17/18/19/20/21/ /22/23/24/
                                     Modified Z-Count         Seqs nº    Nº of data      Station
                                                                            words         health

          Satellite correction (40 bits): /1/ 	/2/3/ /4/5/6/7/8/ /9/10/11/12/13/14/15/16/17/18/19/20/21/21/22/23/24/ /25/26/27/28/29/30/31/32/ /33/34/35/36/37/38/39/40/
                                         Scale  UDRE   Sat. ID             Pseudorange correction                      Range-range correction             IOD
                                         factor                               (2's complement)                             (2's complement)
         */
        final int numSats = (mCurrentMessage.getMessageContent().length() - SIZE_WORD_1_2) / MSG_SAT_SIZE;
        MT1_Message MT1Message = new MT1_Message();
        for (int j=0; j < numSats; j++) {
            scaleFactor = BinaryFunctions.convertToInteger(mCurrentMessage.getMessageContent().substring((FIRST_SCL_FACTOR_BIT-1) + MSG_SAT_SIZE * j, FIRST_SCL_FACTOR_BIT + MSG_SAT_SIZE * j));
            udre = BinaryFunctions.convertToInteger(mCurrentMessage.getMessageContent().substring((FIRST_UDRE_BIT-1) + MSG_SAT_SIZE * j, LAST_UDRE_BIT + MSG_SAT_SIZE * j));
            satID = BinaryFunctions.convertToInteger(mCurrentMessage.getMessageContent().substring((FIRST_SAT_ID_BIT-1) + MSG_SAT_SIZE * j, LAST_SAT_ID_BIT + MSG_SAT_SIZE * j));
            if (satID == 0) { //PRN32 is indicated with all zeros (00000)
                satID = 32;
            }
            pseudoRangeCorrSign=BinaryFunctions.convertToInteger(mCurrentMessage.getMessageContent().substring((FIRST_PSEUDORANGE_BIT-1) + MSG_SAT_SIZE * j, FIRST_PSEUDORANGE_BIT + MSG_SAT_SIZE * j));

            if (pseudoRangeCorrSign == 0) {
                pseudoRangeCorr = BinaryFunctions.convertToInteger(mCurrentMessage.getMessageContent().substring(FIRST_PSEUDORANGE_BIT + MSG_SAT_SIZE * j, (LAST_PSEUDORANGE_BIT) + MSG_SAT_SIZE * j));
            } else {
                pseudoRangeCorr = BinaryFunctions.convertToInteger(BinaryFunctions.complement2toBinary(mCurrentMessage.getMessageContent().substring((FIRST_PSEUDORANGE_BIT-1) + MSG_SAT_SIZE * j, (LAST_PSEUDORANGE_BIT) + MSG_SAT_SIZE * j))) * (-1);
            }

            rangeRateCorrSign=BinaryFunctions.convertToInteger(mCurrentMessage.getMessageContent().substring((FIRST_RANGERATE_BIT-1) + MSG_SAT_SIZE * j, FIRST_RANGERATE_BIT + MSG_SAT_SIZE * j));

            if (rangeRateCorrSign == 0) {
                rangeRateCorr = BinaryFunctions.convertToInteger(mCurrentMessage.getMessageContent().substring(FIRST_RANGERATE_BIT + MSG_SAT_SIZE * j, LAST_RANGERATE_BIT + MSG_SAT_SIZE * j));
            } else {
                rangeRateCorr = BinaryFunctions.convertToInteger(BinaryFunctions.complement2toBinary(mCurrentMessage.getMessageContent().substring((FIRST_RANGERATE_BIT-1) + MSG_SAT_SIZE * j, LAST_RANGERATE_BIT + MSG_SAT_SIZE * j))) * (-1);
            }

            if (scaleFactor == 0) {
                pseudoRangeCorr *= CORRECTION_PRC_TYPE0;
                rangeRateCorr *= CORRECTION_RRC_TYPE0;
            }else{
                pseudoRangeCorr *= CORRECTION_PRC_TYPE1;
                rangeRateCorr *= CORRECTION_RRC_TYPE1;
            }

            iod = BinaryFunctions.convertToInteger(mCurrentMessage.getMessageContent().substring((FIRST_IOD_BIT-1) + MSG_SAT_SIZE * j, LAST_IOD_BIT + MSG_SAT_SIZE * j));
            MT1Message.addSatDecodedData(udre, satID, pseudoRangeCorr, rangeRateCorr, iod);
        }

        int AuxZCount=BinaryFunctions.convertToInteger(mCurrentMessage.getMessageContent().substring(FIRST_ZCOUNT_BIT,LAST_ZCOUNT_BIT));
        zCount=AuxZCount*Z_COUNT_SCALE_FACTOR;

        DateTime dateTime = DateTime.now(DateTimeZone.UTC);
        Long gpsNanos = TimeUnit.MILLISECONDS.toNanos(dateTime.getMillis()) - GPS_UTC_EPOCH_OFFSET_NANOS;
        Double gpsSecondsNow = gpsNanos*Math.pow(10,-9);
        double gpsHourNowRounded=Math.floor(gpsSecondsNow/SECONDS_IN_HOUR);
        timeZCountInSec=gpsHourNowRounded*SECONDS_IN_HOUR+zCount;

        if((timeZCountInSec - gpsSecondsNow) > HALF_HOUR_IN_SECONDS){
            timeZCountInSec=timeZCountInSec-SECONDS_IN_HOUR;
            MT1Message.setDecodedZCount(timeZCountInSec);
        }else if((timeZCountInSec-gpsSecondsNow) < -HALF_HOUR_IN_SECONDS){
            timeZCountInSec=timeZCountInSec+SECONDS_IN_HOUR;
            MT1Message.setDecodedZCount(timeZCountInSec);
        }else{
            MT1Message.setDecodedZCount(timeZCountInSec);
        }

        // Send decoded RTCM2 message to ClientThread for its usage by the client
        mClientThread.sendMessage(MT1Message);
    }
}

