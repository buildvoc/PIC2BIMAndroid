package com.erasmicoin.euspa.gsa.egnss4all.model.extbluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.erasmicoin.euspa.gsa.egnss4all.MainService;
import com.erasmicoin.euspa.gsa.egnss4all.model.GNSSLocation.GNSSSettingsStore;
import com.erasmicoin.euspa.gsa.egnss4all.model.OSNMA.ServerExternalDevicePostPacketTask;
import com.erasmicoin.euspa.gsa.egnss4all.model.OSNMA.ServerPostResponse;
import com.erasmicoin.euspa.gsa.egnss4all.model.fusedLocation.FLDelegateActivity;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothClassicService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.util.GpsFixQuality;
import net.sf.marineapi.nmea.util.Position;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class BluetoothManager {

    private final String TAG = BluetoothManager.class.getSimpleName();
    private static final int MY_BLUETOOTH_PERMISSION = 66;
    private BluetoothService service;
    private ArrayList<BluetoothDevice> devices;
    private BluetoothDevice selectedDevice = null;
    private Context appContext;

    private boolean scanIsRunning = false;

    public static String EXTERNAL_DEVICE_PROVIDER = "BTOSNMAGPSProvider";

    public static HashMap<String, Date> satValidationMap = new HashMap<>();
    private static int satValidationTimeframe = 60; //in seconds
    private final int MIN_OSNMA_VALIDATED_FOR_FIX = 1;
    private final int MIN_SATELLITES_FOR_FIX = 3;



    private MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    private MutableLiveData<LocationResult> lastLocationResult = new MutableLiveData<>(null);
    private Observer<LocationResult> lastLocationResultObserver;
    private Observer<Boolean> isRunningObserver;
    private FLDelegateActivity flDelegateActivity;

    private BluetoothLocationSource bluetoothLocationSource;
    /**
     * used to move the camera to the nearest future position once
     */
    private boolean isCameraMoveRequested = false;
    private int cameraZoom = 16;
    private int cameraAnimateDurationMils = 700;

    public interface BluetoothLocationCallback {

        void onNewLocation(LocationResult locationResult);

    }

    BluetoothLocationCallback bluetoothLocationCallback;

    public void setBluetoothLocationCallback(BluetoothLocationCallback bluetoothLocationCallback) {
        this.bluetoothLocationCallback = bluetoothLocationCallback;
    }

    public BluetoothManager(Context context) {
        appContext = context;
        bluetoothLocationSource = new BluetoothLocationSource(this);
        initBluetooth(context);
    }

    public void initBluetooth(Context context){
        BluetoothConfiguration config = new BluetoothConfiguration();
        config.context = context;
        config.bluetoothServiceClass = BluetoothClassicService.class;
        config.bufferSize = 1024;
        config.characterDelimiter = '\n';
        config.deviceName = "EGNSS4ALL";
        config.callListenersInMainThread = true;
        config.uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // Required

        BluetoothService.init(config);

        service = BluetoothService.getDefaultInstance();
    }

    public interface ScanEndCallback{
        void onScanEnd(ArrayList<BluetoothDevice> devices);
    }

    public interface DeviceFoundCallback{
        void onDeviceFound(BluetoothDevice device);
        void onDeviceNotFound();
    }

    ScanEndCallback scanEndCallback;
    DeviceFoundCallback deviceFoundCallback;

    public void setScanEndCallback(ScanEndCallback scecb){
        scanEndCallback = scecb;
    }

    public void setDeviceFoundCallback(DeviceFoundCallback dfc){
        deviceFoundCallback = dfc;
    }

    public void startScan(Activity activity){

        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.BLUETOOTH_SCAN);
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, MY_BLUETOOTH_PERMISSION);
        }
        ArrayList<BluetoothDevice> devices = new ArrayList<>();
        final boolean[] isRunning = new boolean[1];
        service.setOnScanCallback(new BluetoothService.OnBluetoothScanCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onDeviceDiscovered(BluetoothDevice device, int rssi) {
                if(device.getName() != null && !device.getName().isEmpty())
                    devices.add(device);
            }

            @Override
            public void onStartScan() {
                scanIsRunning = true;
            }

            @Override
            public void onStopScan() {
                scanIsRunning = false;
                scanEndCallback.onScanEnd(devices);
            }
        });

        service.startScan();

    }

    public interface TestConnectCallback{
        void onConnectionSuccess();
        void onConnectionFailure();
    }

    public void testConnection(BluetoothDevice device, TestConnectCallback callback){
        service.setOnEventCallback(new BluetoothService.OnBluetoothEventCallback() {
            @Override
            public void onDataRead(byte[] buffer, int length) {
                String message = new String(buffer);
                //Log.d("GEGEGE",message);
                service.disconnect();
                callback.onConnectionSuccess();
                //connected
            }

            @Override
            public void onStatusChange(BluetoothStatus status) {
                Log.d(TAG, "Status change");
                Log.d(TAG, status.toString());
                if (status == BluetoothStatus.NONE) {
                    service.disconnect();
                    callback.onConnectionFailure();
                    //not connected
                }
            }

            @Override
            public void onDeviceName(String deviceName) {
                Log.d("GEGEGE", deviceName);
            }

            @Override
            public void onToast(String message) {
            }

            @Override
            public void onDataWrite(byte[] buffer) {
                String message = Arrays.toString(buffer);
                Log.d(TAG, message);
            }
        });
        service.connect(device);
    }

    private boolean deviceFound = false;

    public void getDeviceByName(String deviceName, Activity activity){
        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.BLUETOOTH_SCAN);
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, MY_BLUETOOTH_PERMISSION);
        }
        deviceFound = false;
        service.setOnScanCallback(new BluetoothService.OnBluetoothScanCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onDeviceDiscovered(BluetoothDevice device, int rssi) {
                if(device.getName() != null && !device.getName().isEmpty() && device.getName().equals(deviceName)){
                    service.stopScan();
                    deviceFound = true;
                    selectedDevice = device;
                    deviceFoundCallback.onDeviceFound(device);
                }
            }

            @Override
            public void onStartScan() {
            }

            @Override
            public void onStopScan() {
                //service.stopScan();
                if(!deviceFound){
                    deviceFoundCallback.onDeviceNotFound();
                }
            }
        });

        service.startScan();
    }

    private void getDeviceByName(String deviceName, Activity activity, DeviceFoundCallback deviceFoundCallback){

    }

    public boolean isScanRunning(){
        return scanIsRunning;
    }

    private Thread osnmaThread;

    public void requestLocationUpdates(){
        String deviceName = GNSSSettingsStore.readExternalBTName(appContext);
        if(selectedDevice != null){
            service.setOnEventCallback(new BluetoothService.OnBluetoothEventCallback() {
                @Override
                public void onDataRead(byte[] buffer, int length) {
                    String message = new String(buffer);
                    message = message.replace("\r","");
                    //parse GPGGA - GPGSA
                    if(message.contains("GPGGA")){
                        System.out.println("GGA MESSAGE: <"+message+">");
                        try{
                            SentenceFactory sf = SentenceFactory.getInstance();
                            GGASentence gga = (GGASentence) sf.createParser(message);
                            Position pos = gga.getPosition();
                            GpsFixQuality gfq = gga.getFixQuality();
                            int sats = gga.getSatelliteCount();
                            Location myLocation = new Location(MainService.EXTERNAL_PROVIDER);
                            myLocation.setLatitude(pos.getLatitude());
                            myLocation.setLongitude(pos.getLongitude());
                            myLocation.setAltitude(pos.getAltitude());
                            myLocation.setTime(new Date().getTime());
                            ArrayList<Location> tmpList = new ArrayList<Location>();
                            tmpList.add(myLocation);
                            LocationResult lr = LocationResult.create(tmpList);
                            if(bluetoothLocationCallback != null){
                                bluetoothLocationCallback.onNewLocation(lr);
                            }else{
                                newLocationResult(lr);
                            }

                        }catch(Exception e){
                            Log.e(TAG, "Error parsing GGA <"+message+">");
                        }
                    }else{
                        try{
                            JSONObject msg = new JSONObject(message);

                            Location myLocation = new Location(EXTERNAL_DEVICE_PROVIDER);
                            myLocation.setLatitude(Double.parseDouble(msg.getString("latitudine")));
                            myLocation.setLongitude(Double.parseDouble(msg.getString("longitudine")));
                            myLocation.setAltitude(Double.parseDouble(msg.getString("msl")));

                            String svid = msg.getString("svId");
                            int usiSats = msg.getInt("siv");
                            double accuracy = msg.getDouble("accH");

                            Log.d(TAG,"STARTING OSNMA THREAD");
                            osnmaThread = new Thread(() -> {
                                try {
                                    long rt = new Date().getTime();
                                    String rawDate = String.valueOf(rt).substring(0,10);
                                    String svidStr = svid;
                                    Thread.sleep(10000);

                                    AsyncTask<JSONObject, Void, ServerPostResponse> server_response = new ServerExternalDevicePostPacketTask().execute(msg);

                                    try {
                                        ServerPostResponse result = server_response.get();

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
                                                removeSatFromValidated(svidStr);
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

                            Bundle sats = new Bundle();
                            sats.putInt("visiblesats", usiSats);
                            sats.putInt("usedsats", usiSats);
                            sats.putInt("valisats", satValidationMap.size());
                            sats.putDouble("accuracy", accuracy);
                            sats.putBoolean("isOsnma", true);
                            sats.putBoolean("isExternal",true);
                            myLocation.setExtras(sats);

                            myLocation.setTime(new Date().getTime());
                            ArrayList<Location> tmpList = new ArrayList<Location>();
                            tmpList.add(myLocation);
                            LocationResult lr = LocationResult.create(tmpList);
                            if(bluetoothLocationCallback != null){
                                bluetoothLocationCallback.onNewLocation(lr);
                            }else{
                                newLocationResult(lr);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onStatusChange(BluetoothStatus status) {
                    Log.d(TAG, "Status change");
                    Log.d(TAG, status.toString());
                }

                @Override
                public void onDeviceName(String deviceName) {
                    Log.d("GEGEGE", deviceName);
                }

                @Override
                public void onToast(String message) {
                }

                @Override
                public void onDataWrite(byte[] buffer) {
                    String message = Arrays.toString(buffer);
                    Log.d(TAG, message);
                }
            });
            service.connect(selectedDevice);
        }
    }

    private void newLocationResult(LocationResult locationResult) {
        lastLocationResult.postValue(locationResult);
        bluetoothLocationSource.onNewLocation(locationResult.getLastLocation());
    }


    public void start(Activity activity) {
        if (isRunning.getValue()) {
            return;
        }

        requestLocationUpdates();
        isRunning.setValue(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void stop() {
        if (!isRunning.getValue()) {
            return;
        }
        service.disconnect();
        isRunning.setValue(false);
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

    public BluetoothLocationSource getFlLocationSource() {
        return bluetoothLocationSource;
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
        long seconds = (validationDate.getTime() - now.getTime())/1000;
        if(seconds > satValidationTimeframe){
            return true;
        }else{
            return false;
        }
    }


    public int getValidatedSatsNum(){
        return satValidationMap.size();
    }


}
