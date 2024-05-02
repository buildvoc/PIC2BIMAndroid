package com.erasmicoin.euspa.gsa.egnss4all.model.GNSSLocation;

import android.util.Log;

import java.util.ArrayList;

import eu.foxcom.gnss_compare_core.Constellations.Constellation;
import eu.foxcom.gnss_compare_core.Constellations.GalileoConstellation;
import eu.foxcom.gnss_compare_core.Constellations.GalileoE5aConstellation;
import eu.foxcom.gnss_compare_core.Constellations.GalileoGpsConstellation;
import eu.foxcom.gnss_compare_core.Constellations.GalileoIonoFreeConstellation;
import eu.foxcom.gnss_compare_core.Constellations.GalileoOSNMAConstellation;
import eu.foxcom.gnss_compare_core.Constellations.GpsConstellation;
import eu.foxcom.gnss_compare_core.Constellations.GpsIonoFreeConstellation;
import eu.foxcom.gnss_compare_core.Constellations.GpsL5Constellation;
import eu.foxcom.gnss_compare_core.Corrections.Correction;
import eu.foxcom.gnss_compare_core.Corrections.IonoCorrection;
import eu.foxcom.gnss_compare_core.Corrections.ShapiroCorrection;
import eu.foxcom.gnss_compare_core.Corrections.TropoCorrection;
import eu.foxcom.gnss_compare_core.PvtMethods.DynamicExtendedKalmanFilter;
import eu.foxcom.gnss_compare_core.PvtMethods.PedestrianStaticExtendedKalmanFilter;
import eu.foxcom.gnss_compare_core.PvtMethods.PvtMethod;
import eu.foxcom.gnss_compare_core.PvtMethods.StaticExtendedKalmanFilter;
import eu.foxcom.gnss_compare_core.PvtMethods.WeightedLeastSquares;

public class SettingsConverter {

    private static String TAG = "GNSSSettingsConverter";

    public static Constellation getConstellation(int constSetting){

        switch (constSetting) {
            case GNSSSettingsStore.GALILEO_E1_CONSTELLATION:
                return new GalileoConstellation();
            case GNSSSettingsStore.GALILEO_E1_OSNMA_CONSTELLATION:
                return new GalileoOSNMAConstellation();
            case GNSSSettingsStore.GALILEO_GPS_CONSTELLATION:
                return new GalileoGpsConstellation();
            case GNSSSettingsStore.GALILEO_IONOFREE_CONSTELLATION:
                return new GalileoIonoFreeConstellation();
            case GNSSSettingsStore.GPS_CONSTELLATION:
                return new GpsConstellation();
            case GNSSSettingsStore.GALILEO_E5_CONSTELLATION:
                return new GalileoE5aConstellation();
            case GNSSSettingsStore.GPS_IONOFREE_CONSTELLATION:
                return new GpsIonoFreeConstellation();
            case GNSSSettingsStore.GPS_L5_CONSTELLATION:
                return new GpsL5Constellation();
            default:
                return null;
        }
    }

    public static String getConstellationName(int constSetting){
        switch (constSetting) {
            case GNSSSettingsStore.GALILEO_E1_CONSTELLATION:
                return "Galileo E1";
            case GNSSSettingsStore.GALILEO_E1_OSNMA_CONSTELLATION:
                return "Galileo E1 OSNMA Verified";
            case GNSSSettingsStore.GALILEO_GPS_CONSTELLATION:
                return "Galileo E1 + GPS";
            case GNSSSettingsStore.GALILEO_IONOFREE_CONSTELLATION:
                return "Galileo Iono Free";
            case GNSSSettingsStore.GPS_CONSTELLATION:
                return "GPS";
            case GNSSSettingsStore.GALILEO_E5_CONSTELLATION:
                return "Galileo E5";
            case GNSSSettingsStore.GPS_IONOFREE_CONSTELLATION:
                return "GPS Iono Free";
            case GNSSSettingsStore.GPS_L5_CONSTELLATION:
                return "GPS L5";
            default:
                return null;
        }
    }

    public static ArrayList<Correction> getCorrectionList(String corrections){
        ArrayList<Correction> corrList = new ArrayList<>();
        String[] corrArr = corrections.split(",");
        for(int i = 0; i < corrArr.length; i++){
            try{
                int corr = Integer.parseInt(corrArr[i]);
                switch (corr) {
                    case GNSSSettingsStore.TROPO_CORRECTION:
                        corrList.add(new TropoCorrection());
                        break;
                    case GNSSSettingsStore.IONO_CORRECTION:
                        corrList.add(new IonoCorrection());
                        break;
                    case GNSSSettingsStore.SHAPIRO_CORRECTION:
                        corrList.add(new ShapiroCorrection());
                        break;
                    default:
                        break;
                }
            }catch(NumberFormatException nfe){
                Log.d(TAG,"No correction");
            }


        }

        return corrList;

    }

    public static ArrayList<String> getCorrectionNames(String corrections){
        ArrayList<String> corrList = new ArrayList<>();
        String[] corrArr = corrections.split(",");
        for(int i = 0; i < corrArr.length; i++){
            try{
                int corr = Integer.parseInt(corrArr[i]);
                switch (corr) {
                    case GNSSSettingsStore.TROPO_CORRECTION:
                        corrList.add("Troposphere Correction");
                        break;
                    case GNSSSettingsStore.IONO_CORRECTION:
                        corrList.add("Ionosphere Correction");
                        break;
                    case GNSSSettingsStore.SHAPIRO_CORRECTION:
                        corrList.add("Shapiro Correction");
                        break;
                    default:
                        break;
                }
            }catch(NumberFormatException nfe){
                Log.d(TAG,"No correction");
            }


        }

        return corrList;
    }

    public static PvtMethod getPositioningMethod(int method){
        switch (method) {
            case GNSSSettingsStore.DEK_FILTER_METHOD:
                return new DynamicExtendedKalmanFilter();
            case GNSSSettingsStore.SEK_FILTER_METHOD:
                return new StaticExtendedKalmanFilter();
            case GNSSSettingsStore.PSEK_FILTER_METHOD:
                return new PedestrianStaticExtendedKalmanFilter();
            case GNSSSettingsStore.WLS_METHOD:
                return new WeightedLeastSquares();
            default:
                return null;
        }
    }

    public static String getPositioningMethodName(int method){
        switch (method) {
            case GNSSSettingsStore.DEK_FILTER_METHOD:
                return "Dynamic Extended Kalman Filter";
            case GNSSSettingsStore.SEK_FILTER_METHOD:
                return "Static Extended Kalman Filter";
            case GNSSSettingsStore.PSEK_FILTER_METHOD:
                return "Pedestrian Static Extended Kalman Filter";
            case GNSSSettingsStore.WLS_METHOD:
                return "Weighted Least Squares";
            default:
                return null;
        }
    }

}
