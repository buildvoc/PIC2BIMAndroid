package com.example.ELFA.edas.sisnet;

import com.example.ELFA.edas.staticfunctions.BinaryFunctions;

import java.util.Calendar;

public class SbasMessage {
    private int type;
    private Calendar msgCalendar;
    private String msgHex;
    private String datafield;
    private String parity;

    /**
     * Method - constructor of the class
     *@param hex: String with the EGNOS message in hexadecimal
     */
    SbasMessage(String hex, Calendar mCalendar) {
        String binary = BinaryFunctions.hexToBinary(hex);
        type = BinaryFunctions.convertToInteger(binary.substring(8, 14));
        datafield = binary.substring(14,226);
        parity = binary.substring(226,250);
        msgCalendar = mCalendar;
        msgHex = hex;

    }

    /**
     * Method to get the message type. Valid range 0-63.
     *@return int type:  The message type
     */
    public int getType() {
        return type;
    }

    /**
     * Method to get the Gregorian date of the message in UTC.
     *@return Calendar calendar:  The date of the message
     */
    public Calendar getCalendar() {
        return msgCalendar;
    }

    /**
     * Method to get the egnos message
     *@return String msgHex:  The egnos message hexadecimal string
     */
    public String getMesasgeHex() {
        return msgHex;
    }


    /**
     * Method to get the datafield of the message
     *@return String datafield:  The datafield of the message (in binary)
     */
    public String getDatafield() {
        return datafield;
    }

    /**
     * Method to get the parity of the message
     *@return String parity:  The parity of the message (in binary)
     */
    public String getParity() {
        return parity;
    }

}

