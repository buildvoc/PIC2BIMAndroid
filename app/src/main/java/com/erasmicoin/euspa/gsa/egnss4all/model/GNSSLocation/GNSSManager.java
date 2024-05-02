package com.erasmicoin.euspa.gsa.egnss4all.model.GNSSLocation;

import android.content.Context;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.erasmicoin.euspa.gsa.egnss4all.MainService;
import com.erasmicoin.euspa.gsa.egnss4all.model.OSNMA.ServerPostPacketTask;
import com.erasmicoin.euspa.gsa.egnss4all.model.OSNMA.ServerPostResponse;
import com.erasmicoin.euspa.gsa.egnss4all.model.fusedLocation.FLDelegateActivity;
import com.erasmicoin.euspa.gsa.egnss4all.model.locationManager.InavMessage;
import com.example.ELFA.edas.ClientThread;
import com.galfins.gogpsextracts.Coordinates;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.foxcom.gnss_compare_core.Constellations.Constellation;
import eu.foxcom.gnss_compare_core.Constellations.GalileoOSNMAConstellation;
import eu.foxcom.gnss_compare_core.Corrections.Correction;
import eu.foxcom.gnss_compare_core.Corrections.DGNSSCorrection;
import eu.foxcom.gnss_compare_core.Corrections.SBASCorrection;
import eu.foxcom.gnss_compare_core.PvtMethods.PvtMethod;

public class GNSSManager {

    public static HashMap<String, Date> satValidationMap = new HashMap<>();
    private static int satValidationTimeframe = 240; //in seconds
    private final int MIN_OSNMA_VALIDATED_FOR_FIX = 1;
    private final int MIN_SATELLITES_FOR_FIX = 3;

    private String sessionID = "";
    private GnssMeasurementsEvent.Callback gnssMeasurementsEventListener;
    private final String TAG = "GNSSManager";
    private MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    private Looper looper;
    private Context appContext;
    private LocationManager mLocationManager;

    private LocationListener locationListener;

    private Constellation constellation;
    private PvtMethod pvtMethod;
    private ArrayList<Correction> corrections = new ArrayList<>();
    private Coordinates rawPose;
    private Location rawLocation;

    private boolean isInitialized;

    private MutableLiveData<LocationResult> lastLocationResult = new MutableLiveData<>(null);
    private Observer<LocationResult> lastLocationResultObserver;

    private GNSSLocationSource gnssLocationSource;
    private Observer<Boolean> isRunningObserver;

    private FLDelegateActivity flDelegateActivity;

    private ClientThread mClientThread = new ClientThread();

    private DGNSSCorrection dgnsCorrection;
    private SBASCorrection sbasCorrection;

    private double[] campionamentiX;
    private double[] campionamentiY;

    public static ArrayList<String> validatedSatsArray = new ArrayList<>();

    /**
     * used to move the camera to the nearest future position once
     */
    private boolean isCameraMoveRequested = false;
    private int cameraZoom = 16;
    private int cameraAnimateDurationMils = 700;

    private int numCampionamenti = 0;
    private Point centroid;

    public interface GNSSLocationCallback {

        void onNewLocation(LocationResult locationResult);

    }

    GNSSLocationCallback newLocationCallback;

    private HashMap<Integer, InavMessage> inavMessages;

    public HashMap<Integer, InavMessage> getInavMessages(){
        return this.inavMessages;
    }

    public void setNewLocationCallback(GNSSLocationCallback cb){
        newLocationCallback = cb;
    }

    public GNSSManager(Context appContext){
        this.appContext = appContext;

        sessionID = UUID.randomUUID().toString();
        gnssLocationSource = new GNSSLocationSource(this);

        inavMessages = new HashMap<>();
        mLocationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                Log.d(TAG, "Location changed");
                if(!isInitialized){
                    rawPose = Coordinates.globalGeodInstance(
                            location.getLatitude(),
                            location.getLongitude(),
                            location.getAltitude());
                    rawLocation = location;
                    isInitialized = true;
                }
            }
        };
        gnssMeasurementsEventListener = new GnssMeasurementsEvent.Callback() {
            @Override
            public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
                super.onGnssMeasurementsReceived(eventArgs);
                onMeasurementReceived(eventArgs);
            }
        };
        isInitialized = false;
        generateClasses();
    }

    @RequiresPermission(
            allOf = {"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"}
    )

    public void start() {
        if (isRunning.getValue()) {
            return;
        }
        if (looper == null) {
            looper = appContext.getMainLooper();
        }
        numCampionamenti = 0;

        //fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, looper);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener, looper);
        mLocationManager.registerGnssMeasurementsCallback(gnssMeasurementsEventListener);
        if(constellation instanceof GalileoOSNMAConstellation){
            mLocationManager.registerGnssNavigationMessageCallback(gnssNavigationMessageListener);
            satValidationMap = new HashMap<>();
        }
        isRunning.setValue(true);
    }

    public void stop() {
        if (!isRunning.getValue()) {
            return;
        }
        mLocationManager.removeUpdates(locationListener);
        mLocationManager.unregisterGnssMeasurementsCallback(gnssMeasurementsEventListener);
        if(constellation instanceof GalileoOSNMAConstellation){
            mLocationManager.unregisterGnssNavigationMessageCallback(gnssNavigationMessageListener);
            if(osnmaThread != null && osnmaThread.isAlive()){
                osnmaThread.interrupt();
            }
        }
        if(mClientThread.isAlive()){
            mClientThread.Disconnect();
        }
        isRunning.setValue(false);
    }

    private void onMeasurementReceived(GnssMeasurementsEvent measurement){
        constellation.updateMeasurements(measurement);
        if(isInitialized){
            constellation.calculateSatPosition(rawLocation, rawPose);
            int constSize = constellation.getUsedConstellationSize();
            List<Location> tmpList = new ArrayList<>();


            int minsizeforfix;
            int minSize = constSize;

            /*if(constellation instanceof GalileoOSNMAConstellation){
                minSize = satValidationMap.size();
                minsizeforfix = MIN_OSNMA_VALIDATED_FOR_FIX;
            }else{
                minsizeforfix = MIN_SATELLITES_FOR_FIX;
            }*/

            if ((constellation instanceof GalileoOSNMAConstellation && satValidationMap.size() >= MIN_OSNMA_VALIDATED_FOR_FIX && constSize >= MIN_SATELLITES_FOR_FIX) ||
                    (!(constellation instanceof GalileoOSNMAConstellation) && constSize >= MIN_SATELLITES_FOR_FIX)){
                rawPose = pvtMethod.calculatePose(constellation);
                if(!GNSSSettingsStore.readPositionCentroidActive(appContext)){
                    Location myLocation = new Location(MainService.GNSS_PROVIDER);
                    myLocation.setLatitude(rawPose.getGeodeticLatitude());
                    myLocation.setLongitude(rawPose.getGeodeticLongitude());
                    myLocation.setAltitude(rawPose.getGeodeticHeight());
                    myLocation.setTime(new Date().getTime());
                    Bundle sats = new Bundle();
                    sats.putInt("visiblesats",constellation.getVisibleConstellationSize());
                    sats.putInt("usedsats",constellation.getUsedConstellationSize());
                    if(constellation instanceof GalileoOSNMAConstellation){
                        sats.putInt("valisats", satValidationMap.size());
                        sats.putBoolean("isOsnma",true);
                    }else{
                        sats.putBoolean("isOsnma",false);
                    }
                    myLocation.setExtras(sats);
                    tmpList.add(myLocation);
                    LocationResult lr = LocationResult.create(tmpList);
                    if(flDelegateActivity != null){
                        newLocationResult(lr);
                    }
                    if(newLocationCallback != null){
                        newLocationCallback.onNewLocation(lr);
                    }
                }else{
                    campionamentiX[numCampionamenti] = rawPose.getGeodeticLongitude();
                    campionamentiY[numCampionamenti] = rawPose.getGeodeticLatitude();
                    numCampionamenti++;
                    if(numCampionamenti == GNSSSettingsStore.readPositionCentroidSamples(appContext)){
                        numCampionamenti = 0;
                        double[] copyOfX = Arrays.copyOfRange(campionamentiX, 0, campionamentiX.length);
                        double[] copyOfY = Arrays.copyOfRange(campionamentiY, 0, campionamentiY.length);
                        List<Point> convex = GrahamScan.getConvexHull(copyOfX, copyOfY);
                        centroid = GrahamScan.getHullCentroid(convex);

                        Location myLocation = new Location(MainService.GNSS_PROVIDER);
                        myLocation.setLatitude(centroid.x);
                        myLocation.setLongitude(centroid.y);
                        myLocation.setAltitude(rawPose.getGeodeticHeight());
                        myLocation.setTime(new Date().getTime());

                        Bundle sats = new Bundle();
                        sats.putInt("visiblesats",constellation.getVisibleConstellationSize());
                        sats.putInt("usedsats",constellation.getUsedConstellationSize());
                        myLocation.setExtras(sats);
                        tmpList.add(myLocation);
                        LocationResult lr = LocationResult.create(tmpList);

                        if(flDelegateActivity != null){
                            newLocationResult(lr);
                        }
                        if(newLocationCallback != null){
                            newLocationCallback.onNewLocation(lr);
                        }
                    }
                }
            }else{
                /*List<Location> tmpList2 = new ArrayList<>();
                LocationResult lr = LocationResult.create(tmpList2);

                if(flDelegateActivity != null){
                    newLocationResult(lr);
                }
                if(newLocationCallback != null){
                    newLocationCallback.onNewLocation(lr);
                }*/
            }
        }
    }

    private void generateClasses(){

        constellation = SettingsConverter.getConstellation(GNSSSettingsStore.readPositionConstellation(appContext));
        corrections = SettingsConverter.getCorrectionList(GNSSSettingsStore.readPositionCorrections(appContext));
        pvtMethod = SettingsConverter.getPositioningMethod(GNSSSettingsStore.readPositionPositioningMethod(appContext));

        if(GNSSSettingsStore.readPositionSBASActive(appContext)){

            switch (GNSSSettingsStore.readPositionSBASType(appContext)){
                case GNSSSettingsStore.DGNSS_CORRECTION:
                    if(!mClientThread.isAlive())
                        mClientThread.start();
                    dgnsCorrection = new DGNSSCorrection(mClientThread,appContext);
                    corrections.add(dgnsCorrection);
                    dgnsCorrection.connectNTRIP(GNSSSettingsStore.readEDASUsername(appContext), GNSSSettingsStore.readEDASPassword(appContext));
                    break;
                case GNSSSettingsStore.SBAS_CORRECTION:
                    if(!mClientThread.isAlive())
                        mClientThread.start();
                    sbasCorrection = new SBASCorrection(mClientThread);
                    sbasCorrection.connectSISNET(GNSSSettingsStore.readEDASUsername(appContext), GNSSSettingsStore.readEDASPassword(appContext));
                    corrections.add(sbasCorrection);
                    break;
                default:
                    break;
            }
        }

        constellation.addCorrections(corrections);


        if(GNSSSettingsStore.readPositionCentroidActive(appContext)){
            campionamentiX = new double[GNSSSettingsStore.readPositionCentroidSamples(appContext)];
            campionamentiY = new double[GNSSSettingsStore.readPositionCentroidSamples(appContext)];
        }

    }


    private final GnssNavigationMessage.Callback gnssNavigationMessageListener =
            new GnssNavigationMessage.Callback() {
                @Override
                public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
                    if(event.getType() == GnssNavigationMessage.TYPE_GAL_I){

                        byte[] rawData = event.getData();

                        String b64Inav = Base64.getEncoder().encodeToString(rawData);
                        long rt = new Date().getTime();
                        String rawDate = String.valueOf(rt).substring(0,10);

                        InavMessage msgTmp = new InavMessage( event.getSvid(), Long.parseLong(rawDate), b64Inav);
                        inavMessages.put(event.getSvid(), msgTmp);

                        verify_osnma_packet(b64Inav, event.getSvid(), 0, 0);

                    }

                }

                @Override
                public void onStatusChanged(int status) {
                    Log.d(TAG,"NEW NAVIGATION STATUS "+status);
                }
            };


    private Thread osnmaThread;

    private void verify_osnma_packet(String b64Inav, int svid, int wn, int tow) {
        Log.d(TAG,"STARTING OSNMA THREAD");
        osnmaThread = new Thread(() -> {
            try {
                long rt = new Date().getTime();
                String rawDate = String.valueOf(rt).substring(0,10);

                Thread.sleep(10000);

                String deviceName = android.os.Build.MODEL;
                String deviceMan = android.os.Build.MANUFACTURER;
                String deviceVer = Build.VERSION.RELEASE;

                AsyncTask<String, Void, ServerPostResponse> server_response = new ServerPostPacketTask().execute(
                        rawDate,
                        String.valueOf(svid),
                        "2",
                        b64Inav,
                        deviceMan,
                        deviceName,
                        sessionID,
                        deviceVer);

                try {
                    ServerPostResponse result = server_response.get();
                    String svidStr = String.valueOf(svid);
                    if(result.getStatus().equalsIgnoreCase(ServerPostResponse.OK)) {
                        if (!isSatValidated(svidStr)) {
                            addSatToValidated(svidStr);
                        } else {
                            if(isValidationExpired(svidStr)){
                                removeSatFromValidated(svidStr);
                                addSatToValidated(svidStr);
                            }
                        }
                    }else if(result.getStatus().equalsIgnoreCase(ServerPostResponse.KO)) {
                        if (isSatValidated(svidStr) && isValidationExpired(svidStr)) {
                            removeSatFromValidated(String.valueOf(svid));
                        }
                    }

                    Thread.currentThread().interrupt();
                    Log.d(TAG,"Server response: <"+ svid +"> - <"+result.getStatus()+">");
                } catch (ExecutionException e) {
                    Log.e(TAG,"Server error",e);
                } catch (InterruptedException e) {
                    Log.e(TAG,"Server error",e);
                }
            } catch (InterruptedException e) {
                Log.e(TAG,"OSNMA Validation THREAD interrupted",e);
            }
        });
        osnmaThread.start();
        Log.d(TAG,"OSNMA THREAD STATE: <"+osnmaThread.getState().toString()+">");
    }

    public boolean isRunning() {
        return isRunning.getValue();
    }

    public void setFlDelegateActivity(FLDelegateActivity flDelegateActivity) {
        if (isRunning.getValue()) {
            return;
        }

        if (lastLocationResultObserver != null) {
            lastLocationResult.removeObserver(lastLocationResultObserver);
        }
        if (isRunningObserver != null) {
            isRunning.removeObserver(isRunningObserver);
        }

        this.flDelegateActivity = flDelegateActivity;
        if (flDelegateActivity == null) {
            return;
        }

        AtomicBoolean lastLocationIsFirst = new AtomicBoolean(true);
        lastLocationResultObserver = locationResult -> {
            if (lastLocationIsFirst.get()) {
                lastLocationIsFirst.set(false);
                return;
            }
            flDelegateActivity.onNewFusedLocations(locationResult);
            if (isCameraMoveRequested && locationResult != null && locationResult.getLastLocation() != null) {
                isCameraMoveRequested = false;
                moveCameraToLocation(locationResult.getLastLocation());
            }
        };

        lastLocationResult.observe(flDelegateActivity.getAppCompatActivity(), lastLocationResultObserver);
        AtomicBoolean isRunningIsFirst = new AtomicBoolean(true);
        isRunningObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (isRunningIsFirst.get()) {
                    isRunningIsFirst.set(false);
                    return;
                }
                if (aBoolean){
                    flDelegateActivity.onFLStarted();
                } else {
                    flDelegateActivity.onFLEnded();
                }
            }
        };

        isRunning.observe(flDelegateActivity.getAppCompatActivity(), isRunningObserver);
    }

    private void moveCameraToLocation(Location location) {
        if (location == null) {
            return;
        }

        GoogleMap map = flDelegateActivity.getGoogleMap();
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude()))
                .zoom(cameraZoom)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), cameraAnimateDurationMils, null);
    }

    private void newLocationResult(LocationResult locationResult) {
        //lastLocationResult.setValue(locationResult);
        lastLocationResult.postValue(locationResult);
        gnssLocationSource.onNewLocation(locationResult.getLastLocation());
    }

    public GNSSLocationSource getFlLocationSource() {
        return gnssLocationSource;
    }

    public void requestCameraMoveToNewLocation() {
        isCameraMoveRequested = true;
    }

    public void setCameraZoom(int cameraZoom) {
        this.cameraZoom = cameraZoom;
    }

    public void setCameraAnimateDurationMils(int cameraAnimateDurationMils) {
        this.cameraAnimateDurationMils = cameraAnimateDurationMils;
    }

    public void setLooper(Looper looper) {
        this.looper = looper;
    }

    public int getVisibleSatellites(){
        return constellation.getVisibleConstellationSize();
    }

    public int getUsedSatellites(){
        return constellation.getUsedConstellationSize();
    }

    public static void addSatToValidated(String svid){
        if(!isSatValidated(svid)){
            satValidationMap.put(svid, new Date());
        }
    }

    public static void removeSatFromValidated(String svid){
        satValidationMap.remove(svid);
    }

    public static boolean isSatValidated(String svid){
        boolean found = false;
        Iterator<Map.Entry<String, Date>> itr = satValidationMap.entrySet().iterator();

        while(itr.hasNext())
        {
            Map.Entry<String, Date> entry = itr.next();
            String satId = entry.getKey();
            Date dataValidazione = entry.getValue();
            if(satId.equalsIgnoreCase(svid)){
                found = true;
            }
        }
        return found;
    }

    public static boolean isValidationExpired(String svid){
        Date now = new Date();
        Date validationDate = satValidationMap.get(svid);
        long seconds = (now.getTime() - validationDate.getTime())/1000;
        if(seconds > satValidationTimeframe){
            return true;
        }else{
            return false;
        }
    }


    public int getValidatedSatsNum(){
        return satValidationMap.size();
    }

    public boolean isConstellationOSNMA(){
        if(constellation instanceof GalileoOSNMAConstellation){
            return true;
        }else{
            return false;
        }
    }


}

