package com.erasmicoin.euspa.gsa.egnss4all.model.GNSSLocation;

import android.content.Context;
import android.content.SharedPreferences;

import com.erasmicoin.euspa.gsa.egnss4all.BaseActivity;

public class GNSSSettingsStore {

    public static final String PREFS_NAME = "OSNMA_APP";

    private static final String CURRENT_SERVER = "CURRENT_SERVER";
    private static final String POS_CONSTELLATION ="POSA_CONST";

    private static final String POS_CORRECTIONS ="POSA_CORRS";

    private static final String POS_SBAS_ACTIVE ="POSA_SBAS_ACT";

    private static final String POS_SBAS_TYPE = "POSA_SBAS_TYPE";

    private static final String POS_CENTROID_ACTIVE ="POSA_CENTROID";

    private static final String POS_CENTROID_SAMPLES ="POSA_CENTROID_SAMPLES";

    private static final String POS_POS_METHOD ="POSA_POS_METHOD";

    private static final String EXTERNAL_BT = "EXTERNAL_BT";

    private static final String ONLY_ACTUAL_SKV = "ACTUAL_SKYVIEW";

    private static final String EXTERNAL_NAME = "EXTERNAL_NAME";

    private static final String ANDROID_LOCATION_TYPE = "ANDLOCTYPE";

    private static final String SKYVIEW_CONSTELLATION = "SKYVIEWCON";

    private static final String EDAS_USERNAME = "EDAS_USERNAME";
    private static final String EDAS_PASSWORD = "EDAS_PASSWORD";

    public final static int GALILEO_E1_CONSTELLATION = 1;
    public final static int GALILEO_GPS_CONSTELLATION = 2;
    public final static int GPS_CONSTELLATION = 3;
    public final static int GALILEO_E5_CONSTELLATION = 4;
    public final static int GALILEO_IONOFREE_CONSTELLATION = 5;
    public final static int GPS_IONOFREE_CONSTELLATION = 6;
    public final static int GPS_L5_CONSTELLATION = 7;
    public final static int GALILEO_E1_OSNMA_CONSTELLATION = 8;

    public final static int IONO_CORRECTION = 10;
    public final static int TROPO_CORRECTION = 11;
    public final static int SHAPIRO_CORRECTION = 12;

    public final static int DEK_FILTER_METHOD = 20;
    public final static int PSEK_FILTER_METHOD = 21;
    public final static int SEK_FILTER_METHOD = 22;
    public final static int WLS_METHOD = 23;

    public final static int DGNSS_CORRECTION = 24;
    public final static int SBAS_CORRECTION = 25;

    public static final int MAX_SAMPLINGS = 20;

    public static final int ANDROID_FUSED = 30;
    public static final int ANDROID_GPS = 31;
    public static final int ANDROID_NETWORK = 32;

    public static final String GALILEO_SKYVIEW = "GALILEO";
    public static final String GPS_SKYVIEW = "GPS";
    public static final String GLONASS_SKYVIEW = "GLONASS";
    public static final String BEIDOU_SKYVIEW = "BEIDOU";


    public static void resetDefaultPosition(Context ctx){
        savePositionConstellation(ctx, GALILEO_GPS_CONSTELLATION);
        savePositionCorrections(ctx, "11,12");
        savePositionSBASActive(ctx, false);
        savePositionSBASType(ctx, DGNSS_CORRECTION);
        savePositionPositioningMethod(ctx, DEK_FILTER_METHOD);
        savePositionCentroidActive(ctx, false);
        savePositionCentroidSamples(ctx, 10);
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences.Editor getEditor(Context context) {
        return getSharedPreferences(context).edit(); //SettingsGNSSLocationActivity.getAppContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
    }

    public static void saveCurrentServer(Context ctx, String currentServer){
        SharedPreferences.Editor editor = getEditor(ctx);
        editor.putString(CURRENT_SERVER, currentServer);
        editor.apply();
    }
     public static void savePositionConstellation(Context ctx, int constellation){
        SharedPreferences.Editor editor = getEditor(ctx);
        editor.putInt(POS_CONSTELLATION, constellation);
        editor.apply();
    }

    public static void savePositionCorrections(Context ctx, String corrections){
        SharedPreferences.Editor editor = getEditor(ctx);
        editor.putString(POS_CORRECTIONS, corrections);
        editor.apply();
    }

    public static void savePositionSBASActive(Context ctx, boolean sbasActive){
        SharedPreferences.Editor editor = getEditor(ctx);
        editor.putBoolean(POS_SBAS_ACTIVE, sbasActive);
        editor.apply();
    }

    public static void savePositionSBASType(Context ctx, int sbasActive){
        SharedPreferences.Editor editor = getEditor(ctx);
        editor.putInt(POS_SBAS_TYPE, sbasActive);
        editor.apply();
    }

    public static void savePositionCentroidActive(Context ctx, boolean centroidActive){
        SharedPreferences.Editor editor = getEditor(ctx);
        editor.putBoolean(POS_CENTROID_ACTIVE, centroidActive);
        editor.apply();
    }

    public static void savePositionCentroidSamples(Context ctx, int centroidSamples){
        SharedPreferences.Editor editor = getEditor(ctx);
        editor.putInt(POS_CENTROID_SAMPLES, centroidSamples);
        editor.apply();
    }

    public static void savePositionPositioningMethod(Context ctx, int positioningMethod){
        SharedPreferences.Editor editor = getEditor(ctx);
        editor.putInt(POS_POS_METHOD, positioningMethod);
        editor.apply();
    }
    public static String readCurrentServer(Context ctx){
        SharedPreferences settings = getSharedPreferences(ctx);
        String server = settings.getString(CURRENT_SERVER, BaseActivity.SERVER_BASE_URL);
        if(!server.endsWith("/")){
            server = server + "/";
        }
        return server;
    }

    public static int readPositionConstellation(Context ctx){
        SharedPreferences settings = getSharedPreferences(ctx);
        return settings.getInt(POS_CONSTELLATION, GALILEO_GPS_CONSTELLATION);
    }

    public static String readPositionCorrections(Context ctx){
        SharedPreferences settings = getSharedPreferences(ctx);
        return settings.getString(POS_CORRECTIONS, "11,12");
    }

    public static boolean readPositionSBASActive(Context ctx){
        SharedPreferences settings = getSharedPreferences(ctx);
        return settings.getBoolean(POS_SBAS_ACTIVE, false);
    }

    public static int readPositionSBASType(Context ctx){
        SharedPreferences settings = getSharedPreferences(ctx);
        return settings.getInt(POS_SBAS_TYPE, 24);
    }

    public static boolean readPositionCentroidActive(Context ctx){
        SharedPreferences settings = getSharedPreferences(ctx);
        return settings.getBoolean(POS_CENTROID_ACTIVE, false);
    }

    public static int readPositionCentroidSamples(Context ctx){
        SharedPreferences settings = getSharedPreferences(ctx);
        return settings.getInt(POS_CENTROID_SAMPLES, 10);
    }

    public static int readPositionPositioningMethod(Context ctx){
        SharedPreferences settings = getSharedPreferences(ctx);
        return settings.getInt(POS_POS_METHOD, 20);
    }

    public static void saveExternalBT(Context ctx, boolean btStatus){
        SharedPreferences.Editor editor = getEditor(ctx);
        editor.putBoolean(EXTERNAL_BT, btStatus);
        editor.apply();
    }

    public static boolean readExternalBT(Context ctx){
        SharedPreferences settings = getSharedPreferences(ctx);
        return settings.getBoolean(EXTERNAL_BT, false);
    }

    public static void saveExternalBTName(Context ctx, String btName){
        SharedPreferences.Editor editor = getEditor(ctx);
        editor.putString(EXTERNAL_NAME, btName);
        editor.apply();
    }

    public static String readExternalBTName(Context ctx){
        SharedPreferences settings = getSharedPreferences(ctx);
        return settings.getString(EXTERNAL_NAME, "");
    }

    public static void saveEDASUsername(Context ctx, String username){
        SharedPreferences.Editor editor = getEditor(ctx);
        editor.putString(EDAS_USERNAME, username);
        editor.apply();
    }

    public static String readEDASUsername(Context ctx){
        SharedPreferences settings = getSharedPreferences(ctx);
        return settings.getString(EDAS_USERNAME, null);
    }

    public static void saveEDASPassword(Context ctx, String password){
        SharedPreferences.Editor editor = getEditor(ctx);
        editor.putString(EDAS_PASSWORD, password);
        editor.apply();
    }

    public static String readEDASPassword(Context ctx){
        SharedPreferences settings = getSharedPreferences(ctx);
        return settings.getString(EDAS_PASSWORD, null);
    }


    public static void saveSkyviewConstellation(Context ctx, String skyType){
        SharedPreferences.Editor editor = getEditor(ctx);
        editor.putString(SKYVIEW_CONSTELLATION, skyType);
        editor.apply();
    }

    public static String readSkyviewConstellation(Context ctx){
        SharedPreferences settings = getSharedPreferences(ctx);
        return settings.getString(SKYVIEW_CONSTELLATION, GALILEO_SKYVIEW);
    }

    public static void saveOnlyActualSkyview(Context ctx, boolean actualSwitch){
        SharedPreferences.Editor editor = getEditor(ctx);
        editor.putBoolean(ONLY_ACTUAL_SKV, actualSwitch);
        editor.apply();
    }

    public static boolean readOnlyActualSkyview(Context ctx){
        SharedPreferences settings = getSharedPreferences(ctx);
        return settings.getBoolean(ONLY_ACTUAL_SKV, false);
    }

}

