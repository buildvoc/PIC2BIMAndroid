package com.erasmicoin.euspa.gsa.egnss4all.model.extbluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.erasmicoin.euspa.gsa.egnss4all.MainService;
import com.erasmicoin.euspa.gsa.egnss4all.model.GNSSLocation.GNSSSettingsStore;
import com.erasmicoin.euspa.gsa.egnss4all.model.fusedLocation.FLDelegateActivity;
import com.erasmicoin.euspa.gsa.egnss4all.utils.AppConstant;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.GSASentence;
import net.sf.marineapi.nmea.sentence.RMCSentence;
import net.sf.marineapi.nmea.util.GpsFixQuality;
import net.sf.marineapi.nmea.util.Position;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class BluetoothManager {

    private final static String SERVER_URL = "http://157.230.208.98:8000/galmon";
    private final String TAG = BluetoothManager.class.getSimpleName();
    private static final int MY_BLUETOOTH_PERMISSION = 66;
    //    private BluetoothService service;
    private ArrayList<BleDevice> bleDevices = new ArrayList<BleDevice>();
    //    private BluetoothDevice selectedDevice = null;
    private BleDevice selectedBleDevice = null;
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

    private final BluetoothLocationSource bluetoothLocationSource;
    /**
     * used to move the camera to the nearest future position once
     */
    private boolean isCameraMoveRequested = false;
    private int cameraZoom = 16;
    private int cameraAnimateDurationMils = 700;

    private String bleDataResponse = "";
    private boolean deviceFound = false;
    private double bleLat = 0.0;
    private double bleLong = 0.0;
    private double bleAltitude = 0.0;
    private double bleAccuracy = 0.0;

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

    public void initBluetooth(Context context) {
//        BluetoothConfiguration config = new BluetoothConfiguration();
//        config.context = context;
//        config.bluetoothServiceClass = BluetoothLeService.class;
//        config.bufferSize = 1024;
//        config.characterDelimiter = '$';
//        config.deviceName = "PIC2BIM";
//        config.callListenersInMainThread = true;
//        config.uuidService = UUID.fromString(AppConstant.SERVICE_UUID); // Required
//        config.uuidCharacteristic = UUID.fromString(AppConstant.CHARACTERISTIC_UUID); // Required
//        config.transport = BluetoothDevice.TRANSPORT_LE; // Required for dual-mode devices

//        config.uuid = UUID.fromString(AppConstant.DEVICE_UUID);// required
//        config.uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // Required

//        BluetoothService.init(config);

//        service = BluetoothService.getDefaultInstance();

        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setSplitWriteNum(20)
                .setConnectOverTime(10000)
                .setOperateTimeout(5000);


        UUID[] serviceUuids = new UUID[1];
        serviceUuids[0] = UUID.fromString(AppConstant.SERVICE_UUID);
        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(null)
                .setDeviceName(true, null)
                .setDeviceMac("")
                .setAutoConnect(false)
                .setScanTimeOut(7000)
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

    public interface ScanEndCallback {
        void onScanEnd(ArrayList<BluetoothDevice> devices);
    }

    public interface DeviceFoundCallback {
        void onDeviceFound(BluetoothDevice device);

        void onDeviceNotFound();
    }

    ScanEndCallback scanEndCallback;
    DeviceFoundCallback deviceFoundCallback;

    public void setScanEndCallback(ScanEndCallback scecb) {
        scanEndCallback = scecb;
    }

    public void setDeviceFoundCallback(DeviceFoundCallback dfc) {
        deviceFoundCallback = dfc;
    }

    public void startScan(Activity activity) {

//        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            Log.e(TAG, "Permission");
//            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.BLUETOOTH_SCAN);
//            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION}, MY_BLUETOOTH_PERMISSION);
//        }
        Log.e(TAG, "startScan");
        ArrayList<BluetoothDevice> devices = new ArrayList<>();
        final boolean[] isRunning = new boolean[1];
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                scanIsRunning = true;
                Log.e(TAG, "onScanStarted");
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                bleDevices.add(bleDevice);
                devices.add(bleDevice.getDevice());
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                scanIsRunning = false;
                scanEndCallback.onScanEnd(devices);
            }
        });


//            service.setOnScanCallback(new BluetoothService.OnBluetoothScanCallback() {
//                @SuppressLint("MissingPermission")
//                @Override
//                public void onDeviceDiscovered(BluetoothDevice device, int rssi) {
//                    if (device.getName() != null && !device.getName().isEmpty() && !devices.contains(device))
//                        devices.add(device);
//                }
//
//                @Override
//                public void onStartScan() {
//                    scanIsRunning = true;
//                }
//
//                @Override
//                public void onStopScan() {
//                    scanIsRunning = false;
//                    scanEndCallback.onScanEnd(devices);
//                }
//            });
//
//            service.startScan();


    }

    public interface TestConnectCallback {
        void onConnectionSuccess();

        void onConnectionFailure();

        void onDataReceived(String data);
    }

    @SuppressLint("MissingPermission")
    public void testConnection(BluetoothDevice device, TestConnectCallback callback) {

        for (BleDevice scannedDevice : bleDevices) {
            if (Objects.equals(scannedDevice.getName(), device.getName())) {
                Log.e(TAG, "scannedDevice.getName() : " + scannedDevice.getName() + " and device.getName() : " + device.getName());
                selectedBleDevice = scannedDevice;
                break;
            }
        }

        BleManager.getInstance().connect(selectedBleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                Log.e(TAG, "onStartConnect");
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                Log.e(TAG, "onConnectFail E:" + exception.getDescription());
                callback.onConnectionFailure();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                Log.e(TAG, "onConnectSuccess");
                notifyBleData(callback, null);
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                Log.e(TAG, "onDisConnected");
                callback.onConnectionFailure();
            }
        });

//        service.setOnEventCallback(new BluetoothLeService.OnBluetoothEventCallback() {
//            @Override
//            public void onDataRead(byte[] buffer, int length) {
//                String message = new String(buffer);
//                Log.e("onDataRead", message);
////                service.disconnect();
//                callback.onDataReceived(message);
//                //connected
//            }
//
//            @Override
//            public void onStatusChange(BluetoothStatus status) {
//                Log.d(TAG, "Status change");
//                Log.d(TAG, status.toString());
//                if (status == BluetoothStatus.NONE) {
//                    service.disconnect();
//                    callback.onConnectionFailure();
//                    //not connected
//                } else if (status == BluetoothStatus.CONNECTED) {
//                    //connected
//                    callback.onConnectionSuccess();
//                }
//            }
//
//            @Override
//            public void onDeviceName(String deviceName) {
//                Log.e("onDeviceName", deviceName);
//            }
//
//            @Override
//            public void onToast(String message) {
//            }
//
//            @Override
//            public void onDataWrite(byte[] buffer) {
//                String message = Arrays.toString(buffer);
//                Log.e(TAG, message);
//            }
//        });
//        service.connect(device);
    }

    private void notifyBleData(TestConnectCallback callback, BluetoothLocationCallback bluetoothLocationCallback) {
        BleManager.getInstance().notify(
                selectedBleDevice,
                AppConstant.SERVICE_UUID,
                AppConstant.CHARACTERISTIC_UUID,
                new BleNotifyCallback() {
                    @Override
                    public void onNotifySuccess() {
                        if (callback != null) {
                            callback.onConnectionSuccess();
                        }
                    }

                    @Override
                    public void onNotifyFailure(BleException exception) {
                        if (callback != null) {
                            callback.onConnectionFailure();
                        }
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        String message = new String(data, StandardCharsets.UTF_8);
                        Log.e("BLE -> onDataRead", message);
                        if (message.contains("*")) {
                            bleDataResponse = bleDataResponse + message;
                            if (isValidNMEA(bleDataResponse)) {
                                if (callback != null) {
                                    Log.e("Toast BLE -> onDataRead", bleDataResponse);
                                    callback.onDataReceived(bleDataResponse);
                                } else {
                                    SentenceFactory sf = SentenceFactory.getInstance();
                                    Location myLocation = new Location(MainService.EXTERNAL_PROVIDER);

                                    if (bleDataResponse.contains("GGA")) {
                                        Log.e("External DATA", "GGA MESSAGE: <" + bleDataResponse + ">");
                                        try {
                                            GGASentence gga = (GGASentence) sf.createParser(bleDataResponse);
                                            Position pos = gga.getPosition();
                                            bleLat = pos.getLatitude();
                                            bleLong = pos.getLongitude();
                                            bleAltitude = pos.getAltitude();
                                            myLocation.setLatitude(pos.getLatitude());
                                            myLocation.setLongitude(pos.getLongitude());
                                            myLocation.setAltitude(pos.getAltitude());
                                            myLocation.setAccuracy((float) bleAccuracy);
                                            myLocation.setTime(new Date().getTime());
                                            ArrayList<Location> tmpList = new ArrayList<>();
                                            tmpList.add(myLocation);
                                            LocationResult lr = LocationResult.create(tmpList);
                                            if (bluetoothLocationCallback != null) {
                                                bluetoothLocationCallback.onNewLocation(lr);
                                            } else {
                                                newLocationResult(lr);
                                            }

                                        } catch (Exception e) {
                                            Log.e(TAG, "Error parsing GGA <" + bleDataResponse + ">");
                                        }

                                    } else if (bleDataResponse.contains("GSA")) {
                                        Log.e("External DATA", "GSA MESSAGE: <" + bleDataResponse + ">");
                                        try {
                                            GSASentence gsa = (GSASentence) sf.createParser(bleDataResponse);
                                            bleAccuracy = gsa.getHorizontalDOP();
                                            myLocation.setAccuracy((float) gsa.getHorizontalDOP());
                                            myLocation.setLatitude(bleLat);
                                            myLocation.setLongitude(bleLong);
                                            myLocation.setAltitude(bleAltitude);
                                            myLocation.setTime(new Date().getTime());
                                            ArrayList<Location> tmpList = new ArrayList<>();
                                            tmpList.add(myLocation);
                                            LocationResult lr = LocationResult.create(tmpList);
                                            if (bluetoothLocationCallback != null) {
                                                bluetoothLocationCallback.onNewLocation(lr);
                                            } else {
                                                newLocationResult(lr);
                                            }

                                        } catch (Exception e) {
                                            Log.e(TAG, "Error parsing gsa <" + bleDataResponse + ">");
                                        }

                                    } else if (bleDataResponse.contains("RMC")) {
                                        Log.e("External DATA", "RMC MESSAGE: <" + bleDataResponse + ">");
                                        try {
                                            RMCSentence gga = (RMCSentence) sf.createParser(bleDataResponse);
                                            Position pos = gga.getPosition();
                                            bleLat = pos.getLatitude();
                                            bleLong = pos.getLongitude();
                                            myLocation.setLatitude(pos.getLatitude());
                                            myLocation.setLongitude(pos.getLongitude());
                                            myLocation.setAltitude(bleAltitude);
                                            myLocation.setAccuracy((float) bleAccuracy);
                                            myLocation.setTime(new Date().getTime());
                                            ArrayList<Location> tmpList = new ArrayList<>();
                                            tmpList.add(myLocation);
                                            LocationResult lr = LocationResult.create(tmpList);
                                            if (bluetoothLocationCallback != null) {
                                                bluetoothLocationCallback.onNewLocation(lr);
                                            } else {
                                                newLocationResult(lr);
                                            }

                                        } catch (Exception e) {
                                            Log.e(TAG, "Error parsing RMC <" + bleDataResponse + ">");
                                        }
                                    }
                                }
                            }
                            bleDataResponse = "";
                        } else {
                            bleDataResponse = bleDataResponse + message;
                        }
                    }
                });
    }

    // Compile the pattern once for reuse
    private static final Pattern NMEA_PATTERN = Pattern.compile(
            "(\\$(G[ABILNPQ][A-Z]{3}(?:,(-?\\d*(\\.\\d+)?(?:\\.\\d+)?|[a-zA-Z]+|))+\\w*\\*[\\dA-Fa-f]{2})$)",
            Pattern.CASE_INSENSITIVE
    );

    // Method to check if a single string is a valid NMEA sentence
    public static boolean isValidNMEA(String input) {
        return NMEA_PATTERN.matcher(input).matches();
    }

    public void getDeviceByName(String deviceName, Activity activity) {
//        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.BLUETOOTH_SCAN);
//            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION}, MY_BLUETOOTH_PERMISSION);
//        }

        deviceFound = false;
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                scanIsRunning = true;
                Log.e(TAG, "onScanStarted");
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @SuppressLint("MissingPermission")
            @Override
            public void onScanning(BleDevice bleDevice) {
                BluetoothDevice device = bleDevice.getDevice();
                if (device.getName() != null && !device.getName().isEmpty() && device.getName().equals(deviceName)) {
//                        service.stopScan();
                    BleManager.getInstance().cancelScan();
                    deviceFound = true;
//                        selectedDevice = device;
                    selectedBleDevice = bleDevice;
                    deviceFoundCallback.onDeviceFound(device);
                }
//                    devices.add(bleDevice.getDevice());
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                if (!deviceFound) {
                    deviceFoundCallback.onDeviceNotFound();
                }
            }
        });

//            service.setOnScanCallback(new BluetoothService.OnBluetoothScanCallback() {
//                @SuppressLint("MissingPermission")
//                @Override
//                public void onDeviceDiscovered(BluetoothDevice device, int rssi) {
//                    if (device.getName() != null && !device.getName().isEmpty() && device.getName().equals(deviceName)) {
//                        service.stopScan();
//                        deviceFound = true;
//                        selectedDevice = device;
//                        deviceFoundCallback.onDeviceFound(device);
//                    }
//                }
//
//                @Override
//                public void onStartScan() {
//                }
//
//                @Override
//                public void onStopScan() {
//                    //service.stopScan();
//                    if (!deviceFound) {
//                        deviceFoundCallback.onDeviceNotFound();
//                    }
//                }
//            });
//
//            service.startScan();
    }

//    private void getDeviceByName(String deviceName, Activity activity, DeviceFoundCallback deviceFoundCallback) {
//
//    }
//
//    public boolean isScanRunning() {
//        return scanIsRunning;
//    }
//
//    private Thread osnmaThread;

    public void requestLocationUpdates() {
        //String deviceName = GNSSSettingsStore.readExternalBTName(appContext);
//        if (selectedDevice != null) {
        if (selectedBleDevice != null) {

            BleManager.getInstance().connect(selectedBleDevice, new BleGattCallback() {
                @Override
                public void onStartConnect() {
                    Log.e(TAG, "onStartConnect");
                }

                @Override
                public void onConnectFail(BleDevice bleDevice, BleException exception) {
                }

                @Override
                public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                    Log.e(TAG, "onConnectSuccess");
                    notifyBleData(null, bluetoothLocationCallback);
                }

                @Override
                public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                    Log.e(TAG, "onDisConnected");
                }
            });

//            service.setOnEventCallback(new BluetoothService.OnBluetoothEventCallback() {
//                @Override
//                public void onDataRead(byte[] buffer, int length) {
//                    String message = new String(buffer);
//                    message = message.replace("\r", "");
//                    //parse GPGGA - GPGSA
//                    if (message.contains("GPGGA")) {
//                        System.out.println("GGA MESSAGE: <" + message + ">");
//                        try {
//                            SentenceFactory sf = SentenceFactory.getInstance();
//                            GGASentence gga = (GGASentence) sf.createParser(message);
//                            Position pos = gga.getPosition();
//                            GpsFixQuality gfq = gga.getFixQuality();
//                            int sats = gga.getSatelliteCount();
//                            Location myLocation = new Location(MainService.EXTERNAL_PROVIDER);
//                            myLocation.setLatitude(pos.getLatitude());
//                            myLocation.setLongitude(pos.getLongitude());
//                            myLocation.setAltitude(pos.getAltitude());
//                            myLocation.setTime(new Date().getTime());
//                            ArrayList<Location> tmpList = new ArrayList<Location>();
//                            tmpList.add(myLocation);
//                            LocationResult lr = LocationResult.create(tmpList);
//                            if (bluetoothLocationCallback != null) {
//                                bluetoothLocationCallback.onNewLocation(lr);
//                            } else {
//                                newLocationResult(lr);
//                            }
//
//                        } catch (Exception e) {
//                            Log.e(TAG, "Error parsing GGA <" + message + ">");
//                        }
//                    } else {
//                        try {
//                            JSONObject msg = new JSONObject(message);
//
//                            Location myLocation = new Location(EXTERNAL_DEVICE_PROVIDER);
//                            myLocation.setLatitude(Double.parseDouble(msg.getString("latitudine")));
//                            myLocation.setLongitude(Double.parseDouble(msg.getString("longitudine")));
//                            myLocation.setAltitude(Double.parseDouble(msg.getString("msl")));
//
//                            String svid = msg.getString("svId");
//                            int usiSats = msg.getInt("siv");
//                            double accuracy = msg.getDouble("accH");
//
//                            Log.d(TAG, "STARTING OSNMA THREAD");
//                            osnmaThread = new Thread(() -> {
//                                try {
//                                    long rt = new Date().getTime();
//                                    String rawDate = String.valueOf(rt).substring(0, 10);
//                                    String svidStr = svid;
//                                    Thread.sleep(10000);
//
//                                    try {
//                                        msg.put("source", "client");
//                                    } catch (JSONException e) {
//                                        e.printStackTrace();
//                                    }
//                                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
//                                    OkHttpClient client = new OkHttpClient.Builder()
//                                            .connectTimeout(10, TimeUnit.SECONDS)
//                                            .writeTimeout(10, TimeUnit.SECONDS)
//                                            .readTimeout(30, TimeUnit.SECONDS)
//                                            .build();
//                                    RequestBody body = RequestBody.create(msg.toString(), JSON);
//                                    okhttp3.Request request = new okhttp3.Request.Builder()
//                                            .url(SERVER_URL)
//                                            .post(body)
//                                            .build();
//
//                                    client.newCall(request).enqueue(new Callback() {
//                                        @Override
//                                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                                            call.cancel();
//                                        }
//
//                                        @Override
//                                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                                            try {
//                                                if (response.body() != null) {
//                                                    JSONObject networkResp = new JSONObject(response.body().string());
//                                                    ServerPostResponse result = new ServerPostResponse(networkResp.getString("validity_check"), 0, 0);
//                                                    if (result.getStatus().equalsIgnoreCase(ServerPostResponse.OK)) {
//                                                        if (!isSatValidated(svidStr)) {
//                                                            addSatToValidated(svidStr);
//                                                        } else {
//                                                            if (isValidationExpired(svidStr)) {
//                                                                removeSatFromValidated(svidStr);
//                                                                addSatToValidated(svidStr);
//                                                            }
//                                                        }
//                                                    } else if (result.getStatus().equalsIgnoreCase(ServerPostResponse.KO)) {
//                                                        if (isSatValidated(svidStr) && isValidationExpired(svidStr)) {
//                                                            removeSatFromValidated(svidStr);
//                                                        }
//                                                    }
//
//                                                    Thread.currentThread().interrupt();
//                                                    Log.d(TAG, "Server response: <" + svid + "> - <" + result.getStatus() + ">");
//                                                }
//                                            } catch (JSONException e) {
//                                                Log.e(TAG, "Server error", e);
//                                            }
//                                        }
//                                    });
//                                } catch (InterruptedException e) {
//                                    Log.e(TAG, "OSNMA Validation THREAD interrupted", e);
//                                }
//                            });
//                            osnmaThread.start();
//                            Log.d(TAG, "OSNMA THREAD STATE: <" + osnmaThread.getState().toString() + ">");
//
//                            Bundle sats = new Bundle();
//                            sats.putInt("visiblesats", usiSats);
//                            sats.putInt("usedsats", usiSats);
//                            sats.putInt("valisats", satValidationMap.size());
//                            sats.putDouble("accuracy", accuracy);
//                            sats.putBoolean("isOsnma", true);
//                            sats.putBoolean("isExternal", true);
//                            myLocation.setExtras(sats);
//
//                            myLocation.setTime(new Date().getTime());
//                            ArrayList<Location> tmpList = new ArrayList<Location>();
//                            tmpList.add(myLocation);
//                            LocationResult lr = LocationResult.create(tmpList);
//                            if (bluetoothLocationCallback != null) {
//                                bluetoothLocationCallback.onNewLocation(lr);
//                            } else {
//                                newLocationResult(lr);
//                            }
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//
//                @Override
//                public void onStatusChange(BluetoothStatus status) {
//                    Log.d(TAG, "Status change");
//                    Log.d(TAG, status.toString());
//                }
//
//                @Override
//                public void onDeviceName(String deviceName) {
//                    Log.d("GEGEGE", deviceName);
//                }
//
//                @Override
//                public void onToast(String message) {
//                }
//
//                @Override
//                public void onDataWrite(byte[] buffer) {
//                    String message = Arrays.toString(buffer);
//                    Log.d(TAG, message);
//                }
//            });
//            service.connect(selectedDevice);
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

    public void stop() {
//        service.disconnect();
        if (selectedBleDevice != null) {
            BleManager.getInstance().disconnect(selectedBleDevice);
        }
        if (!isRunning.getValue()) {
            return;
        }
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
                if (aBoolean) {
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


    public static void addSatToValidated(String svid) {
        if (!isSatValidated(svid)) {
            satValidationMap.put(svid, new Date());
        }
    }

    public static void removeSatFromValidated(String svid) {
        satValidationMap.remove(svid);
    }

    public static boolean isSatValidated(String svid) {
        boolean found = false;
        Iterator<Map.Entry<String, Date>> itr = satValidationMap.entrySet().iterator();

        while (itr.hasNext()) {
            Map.Entry<String, Date> entry = itr.next();
            String satId = entry.getKey();
            Date dataValidazione = entry.getValue();
            if (satId.equalsIgnoreCase(svid)) {
                found = true;
            }
        }
        return found;
    }

    public static boolean isValidationExpired(String svid) {
        Date now = new Date();
        Date validationDate = satValidationMap.get(svid);
        long seconds = (validationDate.getTime() - now.getTime()) / 1000;
        if (seconds > satValidationTimeframe) {
            return true;
        } else {
            return false;
        }
    }


    public int getValidatedSatsNum() {
        return satValidationMap.size();
    }


}
