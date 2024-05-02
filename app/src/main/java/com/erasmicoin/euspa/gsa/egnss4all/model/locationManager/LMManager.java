package com.erasmicoin.euspa.gsa.egnss4all.model.locationManager;

import android.content.Context;
import android.location.GnssNavigationMessage;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;

import androidx.annotation.RequiresPermission;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LMManager {


    private Context appContext;

    private LocationManager mLocationManager;

    private LocationListener mLocationListener;

    private GnssNavigationMessage.Callback gnssNavigationMessageListener;

    private Looper looper;
    private LMDelegateActivity lmDelegateActivity;
    private LMLocationSource lmLocationSource;
    /**
     * used to move the camera to the nearest future position once
     */
    private boolean isCameraMoveRequested = false;
    private int cameraZoom = 16;
    private int cameraAnimateDurationMils = 700;

    private int interval = 1000;
    private int fastestInterval= 500;
    private int priority = LocationRequest.PRIORITY_HIGH_ACCURACY;

    private MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    private Observer<Boolean> isRunningObserver;

    private MutableLiveData<LocationResult> lastLocationResult = new MutableLiveData<>(null);
    private Observer<LocationResult> lastLocationResultObserver;

    public final static boolean INAV_MESSAGES_GATHERING = true;

    public static final int INAV_MESSAGES_TO_KEEP = 20;

    public static final int MIN_INAV_MESSAGE_COUNT = 5;

    private HashMap<Integer, InavMessage> inavMessages;

    private boolean isOnline = true;

    public LMManager(Context appContext) {
        this.appContext = appContext;

        inavMessages = new HashMap<>();
        mLocationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = location -> {
            List<Location> tmpList = new ArrayList<>();
            tmpList.add(location);
            LocationResult lr = LocationResult.create(tmpList);
            newLocationResult(lr);
        };

        gnssNavigationMessageListener = new GnssNavigationMessage.Callback() {
            @Override
            public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
                if(event.getType() == GnssNavigationMessage.TYPE_GAL_I && INAV_MESSAGES_GATHERING){

                    byte[] rawData = event.getData();

                    String b64Inav = Base64.getEncoder().encodeToString(rawData);

                    long rt = new Date().getTime();
                    String rawDate = String.valueOf(rt).substring(0,10);

                    InavMessage msgTmp = new InavMessage( event.getSvid(), Long.parseLong(rawDate), b64Inav);
                    inavMessages.put(event.getSvid(), msgTmp);
                }
            }

            @Override
            public void onStatusChanged(int status) {
            }
        };

        lmLocationSource = new LMLocationSource(this);
    }

    public void setLMDelegateActivity(LMDelegateActivity lmDelegateActivity) {
        if (isRunning.getValue()) {
            return;
        }

        if (lastLocationResultObserver != null) {
            lastLocationResult.removeObserver(lastLocationResultObserver);
        }
        if (isRunningObserver != null) {
            isRunning.removeObserver(isRunningObserver);
        }

        this.lmDelegateActivity = lmDelegateActivity;
        if (lmDelegateActivity == null) {
            return;
        }

        AtomicBoolean lastLocationIsFirst = new AtomicBoolean(true);
        lastLocationResultObserver = locationResult -> {
            if (lastLocationIsFirst.get()) {
                lastLocationIsFirst.set(false);
                return;
            }
            lmDelegateActivity.onNewLocation(locationResult);
            if (isCameraMoveRequested && locationResult != null && locationResult.getLastLocation() != null) {
                isCameraMoveRequested = false;
                moveCameraToLocation(locationResult.getLastLocation());
            }
        };

        lastLocationResult.observe(lmDelegateActivity.getAppCompatActivity(), lastLocationResultObserver);
        AtomicBoolean isRunningIsFirst = new AtomicBoolean(true);
        isRunningObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (isRunningIsFirst.get()) {
                    isRunningIsFirst.set(false);
                    return;
                }
                if (aBoolean){
                    lmDelegateActivity.onLMStarted();
                } else {
                    lmDelegateActivity.onLMEnded();
                }
            }
        };

        isRunning.observe(lmDelegateActivity.getAppCompatActivity(), isRunningObserver);
    }

    private void moveCameraToLocation(Location location) {
        if (location == null) {
            return;
        }

        GoogleMap map = lmDelegateActivity.getGoogleMap();
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude()))
                .zoom(cameraZoom)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), cameraAnimateDurationMils, null);
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

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, mLocationListener, looper);
        mLocationManager.registerGnssNavigationMessageCallback(gnssNavigationMessageListener);

        isRunning.setValue(true);
    }

    public void stop() {
        if (!isRunning.getValue()) {
            return;
        }
        mLocationManager.unregisterGnssNavigationMessageCallback(gnssNavigationMessageListener);
        mLocationManager.removeUpdates(mLocationListener);
        isRunning.setValue(false);
    }

    public LocationResult getLastLocationResult() {
        return lastLocationResult.getValue();
    }

    public Location getLastLocation() {
        if (lastLocationResult.getValue() == null) {
            return null;
        }
        return lastLocationResult.getValue().getLastLocation();
    }

    private void newLocationResult(LocationResult locationResult) {
        lastLocationResult.setValue(locationResult);
        lmLocationSource.onNewLocation(locationResult.getLastLocation());
    }

    public boolean isRunning() {
        return isRunning.getValue();
    }

    public void requestCameraMoveToNewLocation() {
        isCameraMoveRequested = true;
    }

    // region get, set

    public Looper getLooper() {
        return looper;
    }

    public void setLooper(Looper looper) {
        this.looper = looper;
    }

    public int getInterval() {
        return interval;
    }

    public int getFastestInterval() {
        return fastestInterval;
    }

    public int getPriority() {
        return priority;
    }

    public LMLocationSource getLmLocationSource() {
        return lmLocationSource;
    }

    public void setCameraZoom(int cameraZoom) {
        this.cameraZoom = cameraZoom;
    }

    public void setCameraAnimateDurationMils(int cameraAnimateDurationMils) {
        this.cameraAnimateDurationMils = cameraAnimateDurationMils;
    }

    // endregion

}
