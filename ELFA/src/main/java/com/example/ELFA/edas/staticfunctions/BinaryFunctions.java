package com.example.ELFA.edas.staticfunctions;

/**
 * Class with functions for binary operations (represented as string)
 */

public class BinaryFunctions {



    /**
     * Method to convert from binary to decimal
     * @param number: Binary string
     * @return int result: Decimal value
     */
    public static int convertToInteger (String number) {
        int result = 0;

        for (int i = 0; i < number.length(); i++) {
            result = 2 * result;
            if (number.charAt(i) == '1')
                result++;
        }
        return result;
    }


    /**
     * Method to convert from complement2 to Integer
     * @param word: complement2 string
     * @return String result: Integer
     */
    public static int complement2toInteger(String word) {

        if (word.charAt(0) == '0') {
            return convertToInteger(word);
        }
            else
            return -(convertToInteger(invertBits(word)) +1);
        }


    /**
     * Method to invert bits of a binary string
     * @param word: binary string
     * @return String charArray: binary string inverted
     */
    public static String invertBits(String word) {

        char[] charArray = word.toCharArray();
        for (int i = 0; i < word.length(); i++) {
            if (charArray[i] == '0')
                charArray[i] = '1';
            else
                charArray[i] = '0';
        }
        return new String(charArray);
    }


    /**
     * Method to convert from complement2 to binary
     * @param word: complement2 string
     * @return String result: Binary string
     */
    public static String complement2toBinary(String word) {
        String aux="1";
        String negWord= invertBits(word);
        String result;

        // Use as radix 2 because it's binary
        int number0 = Integer.parseInt(negWord, 2);
        int number1 = Integer.parseInt(aux, 2);
        int sum = number0 + number1;
        result=Integer.toBinaryString(sum);

        return result;
    }

    /**
     * Method to convert from hexadecimal to binary
     * @param hex: message in hexadecimal formay
     * @return String result: Message in Binary format
     */
    public static String hexToBinary (String hex) {
        StringBuilder bin = new StringBuilder();
        for(int i=0; i<hex.length(); i++) {
            switch (hex.charAt(i) ) {
                case '0':
                    bin.append("0000");
                    break;
                case '1':
                    bin.append("0001");
                    break;
                case '2':
                    bin.append("0010");
                    break;
                case '3':
                    bin.append("0011");
                    break;
                case '4':
                    bin.append("0100");
                    break;
                case '5':
                    bin.append("0101");
                    break;
                case '6':
                    bin.append("0110");
                    break;
                case '7':
                    bin.append("0111");
                    break;
                case '8':
                    bin.append("1000");
                    break;
                case '9':
                    bin.append("1001");
                    break;
                case 'A':
                    bin.append("1010");
                    break;
                case 'B':
                    bin.append("1011");
                    break;
                case 'C':
                    bin.append("1100");
                    break;
                case 'D':
                    bin.append("1101");
                    break;
                case 'E':
                    bin.append("1110");
                    break;
                case 'F':
                    bin.append("1111");
                    break;
                default:
                    throw new IllegalArgumentException("Not an hexadecimal number");
            }
        }
        return bin.toString();
    }
}

