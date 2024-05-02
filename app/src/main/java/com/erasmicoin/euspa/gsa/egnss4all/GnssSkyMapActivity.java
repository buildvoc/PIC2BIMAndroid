package com.erasmicoin.euspa.gsa.egnss4all;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.erasmicoin.euspa.gsa.egnss4all.model.GNSSLocation.GNSSSettingsStore;
import com.erasmicoin.euspa.gsa.egnss4all.model.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import eu.foxcom.gnss_scan.GnssStatusScanner;

public class GnssSkyMapActivity extends BaseActivity {

    public static final String TAG = GnssSkyMapActivity.class.getSimpleName();
    public static final int REQUEST_LOCATION_UPDATE_TIMEOUT = 5000;
    public static final int UPDATE_SATS_NUMBER_INTERVAL = 4000;

    private WebView skyview;

    private JSONObject remoteSatellites = null;

    private GnssStatusScanner gnssStatusUnit;

    private String constellationType = GNSSSettingsStore.GALILEO_SKYVIEW;

    private String lastConstellationType;

    private Spinner constellationSpinner;

    private LocationManager mLocationManager;

    private ProgressDialog locationWait;

    private long startTime;

    private int SECONDS_WITHOUT_SIGNAL = 10;

    private TimerTask timerTask;
    private Timer timer;

    private boolean reachableInternet = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gnss_skyview);
        setToolbar(R.id.toolbar);

        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        startTime = new Date().getTime();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                long now = new Date().getTime();
                long diff = now - startTime;
                if(diff > SECONDS_WITHOUT_SIGNAL*1000L && locationWait.isShowing()){
                    runOnUiThread(() -> {
                        locationWait.dismiss();
                        showLocationErrorDialog();
                        timerTask.cancel();
                    });
                }
            }
        };

        timer = new Timer();

        timer.schedule(timerTask, 0, 5000);

        skyview = findViewById(R.id.skymap);
        skyview.getSettings().setJavaScriptEnabled(true);
        skyview.loadUrl("file:///android_asset/orbit.html");

        constellationSpinner = findViewById(R.id.constellation_spinner);

        locationWait = ProgressDialog.show(this, "",
                getString(R.string.gnsm_wait_message), true);

        locationWait.show();
        setSpinnerAction();

        reachableInternet = Util.isInternetAvailable();
        if(!reachableInternet)
            showNoInternetErrorDialog();
    }

    private void showNoInternetErrorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.gnsm_nointernet);
        // Add the buttons
        builder.setPositiveButton(R.string.dl_OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    private void showLocationErrorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.gnsm_location_error);
        // Add the buttons
        builder.setPositiveButton(R.string.dl_OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                finish();
            }
        });

        builder.create().show();
    }

    private void showNoProviderErrorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.gnsm_location_disabled);
        // Add the buttons
        builder.setPositiveButton(R.string.dl_OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                finish();
            }
        });

        builder.create().show();
    }
    @Override
    public void serviceInit() {
        super.serviceInit();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            goToStartActivity();
            return;
        }

        mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        mLocationManager.registerGnssStatusCallback(gnssStatusListener);

    }

    private final LocationListener locationListener = new LocationListener(){

        @Override
        public void onLocationChanged(@NonNull Location location) {
            locationWait.dismiss();
            if((remoteSatellites == null || (!Objects.equals(constellationType, lastConstellationType))) && reachableInternet) {
                downloadSatellitesLocations(location.getLatitude(), location.getLongitude(), constellationType);
                lastConstellationType = constellationType;
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
            timerTask.cancel();
            showNoProviderErrorDialog();
        }
    };

    private final GnssStatus.Callback gnssStatusListener =
            new GnssStatus.Callback() {
                @Override
                public void onStarted() {
                    super.onStarted();
                    Log.d(TAG,"STARTED STATUS LISTENER");
                }

                @Override
                public void onStopped() {
                    super.onStopped();
                    Log.d(TAG,"STOPPED STATUS LISTENER");
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    mLocationManager.registerGnssStatusCallback(gnssStatusListener);
                }

                @Override
                public void onFirstFix(int ttffMillis) {
                    super.onFirstFix(ttffMillis);
                    Log.d(TAG,"FIRST FIX");
                    Log.d(TAG, String.valueOf(ttffMillis));
                }

                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onSatelliteStatusChanged(GnssStatus status) {
                    super.onSatelliteStatusChanged(status);
                    int satelliteCount = status.getSatelliteCount();
                    JSONArray satsArray = new JSONArray();

                    try{

                        for (int i = 0; i < satelliteCount; i++) {
                            JSONObject satStat = new JSONObject();
                            satStat.put("svid", String.valueOf(status.getSvid(i)));
                            satStat.put("azimuth", String.valueOf(status.getAzimuthDegrees(i)));
                            satStat.put("elevation", String.valueOf(status.getElevationDegrees(i)));
                            satStat.put("constellation", status.getConstellationType(i));
                            satStat.put("snr", status.getCn0DbHz(i));
                            satsArray.put(satStat);
                        }
                    }catch(JSONException e){
                        Log.e(TAG, "Errore json",e);
                    }

                    String satsData = Base64.getEncoder().encodeToString(satsArray.toString().getBytes());
                    String remoteSatsData = "";
                    if(remoteSatellites != null)
                        remoteSatsData = Base64.getEncoder().encodeToString(remoteSatellites.toString().getBytes());

                    String posUpdate = "updateGraph('"+satsData+"','"+remoteSatsData+"','"+constellationType+"','"+!reachableInternet+"')";
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            skyview.evaluateJavascript(posUpdate,null);
                        }
                    });

                }
            };


    private void downloadSatellitesLocations(double latitude, double longitude, String constellation){
        AsyncTask<String, Void, JSONObject> satellites_response = new ServerGetSatelllitesTask().execute(
                String.valueOf(latitude), String.valueOf(longitude), constellation);
        try {
            remoteSatellites = satellites_response.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationManager.unregisterGnssStatusCallback(gnssStatusListener);
        mLocationManager.removeUpdates(locationListener);
        //gnssStatusUnit.stopScan();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

           // updateSatsHandler.removeCallbacks(updateSatsRunnable);
        }
        //MS.stopFusedLocationMonitoring(null, null, null);
    }

    private void setSpinnerAction(){
        constellationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    constellationType = GNSSSettingsStore.GALILEO_SKYVIEW;
                } else if (i == 1) {
                    constellationType = GNSSSettingsStore.GPS_SKYVIEW;
                } else if (i == 2) {
                    constellationType = GNSSSettingsStore.GLONASS_SKYVIEW;
                } else if(i == 3){
                    constellationType = GNSSSettingsStore.BEIDOU_SKYVIEW;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }
}
