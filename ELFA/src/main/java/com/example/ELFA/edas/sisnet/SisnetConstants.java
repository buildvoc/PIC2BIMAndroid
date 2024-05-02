package com.example.ELFA.edas.sisnet;

import android.util.Pair;

import com.example.ELFA.edas.utils.Coordinates;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class SisnetConstants {
    static final int MESSAGE_SIZE = 212;
    static final int NUM_FC_MESSAGES = 4; // MTs 2-5 correspond to fast correction, MT24 is deliberately not taken into account here
    static final int MAX_SATS_M_2_5 = 13;
    static final int MAX_SATS_M_24 = 6;
    static final int BITS_PRN_MASK = 210;
    static final int PRN_MASK = 51;
    static final int BLOCK_SIZE_M24 = 2;
    static final int SPARE_SIZE_M24 = 4;

    static final int VELOCITY_SIZE_M25 = 1;

    static final int MASK_SIZE_M1 = 210;
    static final int IODP_SIZE = 2;

    static final int FASTCO_SIZE = 12;
    static final int UDRE_SIZE = 4;
    static final int IODF_SIZE = 2;
    static final int IODF_ALERT_CONDITION = 3;

    static final int MAX_SATS_LONG = 2;

    static final int TOTAL_SIZE_LONG = 106;

    static final int MASK_SIZE_LONG = 6;
    static final int IOD_SIZE_LONG = 8;
    static final int SPARE_SIZE_LONG = 1;

    static final int SIGMA_XYZ_SIZE_V0 = 9;
    static final int SIGMA_A_SIZE_V0 = 10;
    static final double PRC_SCALEFACTOR = 0.125;
    static final double SIGMA_XYZ_V0_SCALEFACTOR = 0.125;
    static final double SIGMA_A_V0_SCALEFACTOR = Math.pow(2,-31);

    static final int NULL_CODE = 9999;

    static final int THRESHOLD_TIME_DIFF = 30;
    static final int THRESHOLD_RANGE_RATE = 120;
    static final int THRESHOLD_FC_RRC = 12;
    static final int UDREI_NOT_MONITORED = 14;
    static final int UDREI_NOT_USE = 15;

    static final int IGPS_PER_BLOCK = 15;

    static final int NUM_BAND_SIZE = 4;
    static final int BAND_SIZE = 4;
    static final int IGP_MASK_SIZE = 201;
    static final int BLOCK_SIZE = 4;
    static final int VERT_DELAY_SIZE = 9;
    static final int GIVEI_SIZE = 4;
    static final int IODI_SIZE = 2;
    static final double VERT_DELAY_SCALEFACTOR = 0.125;

    static final Map<Pair<Integer, Integer>, Coordinates> IGP_GRID = readIgpGrid();
    private static Map<Pair<Integer, Integer>, Coordinates> readIgpGrid() {
        Map<Pair<Integer, Integer>, Coordinates> map = new HashMap<>();
        BufferedReader csvReader;
        try {
            String fileName = "res/raw/igp_grid.csv";
            ClassLoader classLoader = SisnetConstants.class.getClassLoader();
            csvReader = new BufferedReader(new InputStreamReader(classLoader.getResource(fileName).openStream()));
            String row;
            while ((row = csvReader.readLine()) != null) {
                String[] data = row.split(",");
                map.put(new Pair<>(Integer.parseInt(data[0]), Integer.parseInt(data[1])),
                        new Coordinates(Integer.parseInt(data[2]), Integer.parseInt(data[3])));
            }
            csvReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

}
