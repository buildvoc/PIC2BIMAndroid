package com.example.ELFA.edas.ntrip;

import com.example.ELFA.edas.staticfunctions.BinaryFunctions;

/**
 * A class representing a valid message created in the RTCM2_MessageBuilder.
 */
public class RTCM2_Message {
    public static final int MSG_TYPE_1 = 1;
    private final int WORD_LENGTH = 30;
    private final int PARITY_LENGTH = 6;
    private final int WORD_SIZE_FOR_LENGTH = 2;
    private int mWordCounter;
    private int mWordSize;
    private String mMessageContent;
    private char mLastBit29;
    private char mLastBit30;

    RTCM2_Message() {
        mWordCounter = 0;
        mWordSize = 2; // It is initialized at 2 because it is the minimum message length
        mMessageContent = "";
    }

    /**
     * Method to set the size of the message (word 1 + word 2 + data words (0-30). Nº of data words located in bits 17-21 in the second word.
     */
    private void setSize() {
        if (mWordCounter >= WORD_SIZE_FOR_LENGTH) {
            int LAST_LENGTH_BIT = 21;
            int FIRST_LENGTH_BIT = 17;
            mWordSize = BinaryFunctions.convertToInteger(
                    ExtractFragment(WORD_SIZE_FOR_LENGTH - 1,
                            FIRST_LENGTH_BIT - 1,
                            LAST_LENGTH_BIT - 1)) + WORD_SIZE_FOR_LENGTH;
        }
    }

    /**
     * Method to get the type of the message. Message type located in bits 9-14 in the first word.
     *@return int Message Type: Type of the message
     */
    public int getMsgType(){
        int FIRST_MSG_TYPE_BIT = 9;
        int LAST_MSG_TYPE_BIT = 14;
        return BinaryFunctions.convertToInteger(
                ExtractFragment(WORD_SIZE_FOR_LENGTH - 2,
                        FIRST_MSG_TYPE_BIT - 1,
                        LAST_MSG_TYPE_BIT - 1));
    }

    /**
     * Method to get the value of the bit 29 in the last word.
     *@return char lastBit29: Value of the bit 29 in the last word.
     */
    char getLastBit29() { return mLastBit29; }

    /**
     * Method to get the value of the bit 30 in the last word.
     *@return char lastBit30: Value of the bit 30 in the last word.
     */
    char getLastBit30() { return mLastBit30; }

    /**
     * Method to get the number of words of the message.
     *@return int mWordCounter: number of words of the message.
     */
    int getWordCounter() { return mWordCounter; }

    /**
     * Method to get the contain of the message.
     *@return String mMessageContent: Binary contain of the message.
     */
    String getMessageContent(){ return mMessageContent; }

    /**
     * Method to get a selected contain of the message.
     *@param word: Word where is the content
     *@param firstBit: First bit of the word where the content is
     *@param lastBit: Last bit of the word where the content is
     *@return String: Selected contain of the message
     */
    private String ExtractFragment(int word, int firstBit, int lastBit) {
        int CLEAN_WORD_LENGTH = WORD_LENGTH - PARITY_LENGTH;
        return mMessageContent.substring(word * CLEAN_WORD_LENGTH + firstBit, word * CLEAN_WORD_LENGTH + lastBit + 1);
    }

    /**
     * Method to check if the word counter is equal to the word size in order to know if the valid message is completed.
     *@return boolean: return true if the word counter is equal to the word size
     */
    boolean isComplete() { return (mWordCounter == mWordSize); }

    /**
     *Method to add a word in order to create a valid message.
     * @param word : word to add in a valid message
     * @param isComplement1 : boolean that indicates if the word is in complement 1
     */
    void addWord(String word, boolean isComplement1) {
        if ((word.length() == WORD_LENGTH) && (mWordCounter < mWordSize)) {
            String cleanWord = word.substring(0, WORD_LENGTH - PARITY_LENGTH);
            if (isComplement1) {
                cleanWord = BinaryFunctions.invertBits(cleanWord);
            }
            mMessageContent = mMessageContent.concat(cleanWord);
            mLastBit29 = word.charAt(28);
            mLastBit30 = word.charAt(29);
            mWordCounter++;

            // If the word with the word counter for the message has been processed, set the size
            if (mWordCounter == WORD_SIZE_FOR_LENGTH) {
                setSize();
            }
        }
    }
}

