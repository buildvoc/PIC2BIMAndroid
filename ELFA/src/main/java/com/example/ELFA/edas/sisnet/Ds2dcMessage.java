package com.example.ELFA.edas.sisnet;

import java.util.Calendar;
import java.util.TimeZone;

public class Ds2dcMessage {
    private Calendar mCalendar;
    private SbasMessage mEgnosMessage;

    /**
     * Method to decompress an hexadecimal message using SINCA algorithm
     *@param Message: String with the compressed message.
     *@return String result: Decompressed message.
     */
    private static String DecompressSinca(String Message){
        StringBuilder result = new StringBuilder();
        for (int i=0; i< Message.length(); i++) {
            if (Message.charAt(i) =='|') {
                String substring = Message.substring(i+1, i+2);
                int repetitions = Integer.parseInt(substring,16);
                for (int j = 1; j < repetitions; j++ ){
                    result.append(Message.charAt(i - 1));
                }
                i++;
            } else if (Message.charAt(i) == '/' ) {
                String substring = Message.substring(i+1, i+3);
                int repetitions = Integer.parseInt(substring,16);
                for (int j = 1; j < repetitions; j++) {
                    result.append(Message.charAt(i - 1));
                }
                i+=2;
            } else {
                result.append(Message.charAt(i));
            }
        }
        return result.toString();
    }

    /**
     * Method to convert a java type date from gpstime
     *@param gpsWeek: integer with the GPS Week.
     *@return result: time in Calendar format.
     */
    private Calendar extractDate (int gpsWeek, int gpsTime) {
        Calendar result = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        result.set(1980,0,6,0,0,0);
        result.add(Calendar.WEEK_OF_MONTH,gpsWeek);
        result.add(Calendar.SECOND,gpsTime);
        return result;
    }

    /**
     * Method - constructor of the class
     *@param message: String with the compressed message
     */
    public Ds2dcMessage(String message) {
        String[] splitMessage = message.split(",");
        if(splitMessage.length >= 3) { // To ensure we are not trying to parse a server info msg, such as STOP*
            String[] splitEgnosMessage = splitMessage[3].split("\\*");
            mCalendar = extractDate((Integer.parseInt(splitMessage[1])), Integer.parseInt(splitMessage[2]));
            mEgnosMessage = new SbasMessage(DecompressSinca(splitEgnosMessage[0]), mCalendar);
        }
    }

    /**
     * Method to get the date of the message
     *@return Calendar calendar:  The date of the message
     */
    public Calendar getCalendar() {
        return mCalendar;
    }

    /**
     * Method to get the egnos message
     *@return EgnosMessage egnosmessage:  The egnos message
     */
    public SbasMessage getEgnosMessage() {
        return mEgnosMessage;
    }
}

