package com.erasmicoin.euspa.gsa.egnss4all.model.extbluetooth;

public class NMEAValidator {
    /**
     * Validates an NMEA sentence
     * @param sentence The NMEA sentence to validate
     * @return true if the sentence is valid, false otherwise
     */
    public static boolean validateNMEASentence(String sentence) {
        // Check basic format
        if (sentence == null || !sentence.startsWith("$")) {
            return false;
        }

        // Check for checksum
        int starIndex = sentence.indexOf('*');
        if (starIndex == -1 || starIndex + 3 > sentence.length()) {
            return false;
        }

        // Extract the checksum from the sentence
        String checksumString = sentence.substring(starIndex + 1, starIndex + 3);

        // Calculate the checksum
        String dataToCheck = sentence.substring(1, starIndex);
        int calculatedChecksum = 0;
        for (int i = 0; i < dataToCheck.length(); i++) {
            calculatedChecksum ^= dataToCheck.charAt(i);
        }

        // Convert calculated checksum to hex string
        String calculatedChecksumString = Integer.toHexString(calculatedChecksum).toUpperCase();
        if (calculatedChecksumString.length() == 1) {
            calculatedChecksumString = "0" + calculatedChecksumString;
        }

        // Compare calculated checksum with the one in the sentence
        return checksumString.equals(calculatedChecksumString);
    }
}
