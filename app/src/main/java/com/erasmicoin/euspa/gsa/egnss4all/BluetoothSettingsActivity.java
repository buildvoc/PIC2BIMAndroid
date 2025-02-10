package com.erasmicoin.euspa.gsa.egnss4all;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Lifecycle;

import com.erasmicoin.euspa.gsa.egnss4all.model.GNSSLocation.GNSSSettingsStore;
import com.erasmicoin.euspa.gsa.egnss4all.model.extbluetooth.BluetoothManager;
import com.erasmicoin.euspa.gsa.egnss4all.model.extbluetooth.ConnectionResultDialog;
import com.erasmicoin.euspa.gsa.egnss4all.model.extbluetooth.SelectDeviceDialog;

import java.util.ArrayList;
import java.util.HashMap;

import co.uk.pic2bim.R;

public class BluetoothSettingsActivity extends BaseActivity implements BluetoothManager.ScanEndCallback {

    private BluetoothManager bluetoothManager;
    private BluetoothDevice selectedDevice;
    private TextView currentDevice;

    private AlertDialog notSelectedDlg;
    private AlertDialog notFoundDlg;
    private ConnectionResultDialog testResult;

    private Dialog scanDialog;
    private Dialog searchDialog;

    private HashMap<String, BluetoothDevice> deviceList = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_settings);
        setToolbar(R.id.toolbar);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        bluetoothManager.stop();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void init() {

        bluetoothManager = new BluetoothManager(getApplicationContext());
        currentDevice = findViewById(R.id.selectedDevice);

        if (GNSSSettingsStore.readExternalBT(getApplicationContext())) {
            if (checkBluetoothEnabled()) {
                ((Switch) findViewById(R.id.bts_switch_external)).setChecked(true);
                findViewById(R.id.selectedDeviceItem).setVisibility(View.VISIBLE);
                findViewById(R.id.testConnection).setVisibility(View.VISIBLE);
                findViewById(R.id.scanDevices).setVisibility(View.VISIBLE);
                if (!GNSSSettingsStore.readExternalBTName(getApplicationContext()).isEmpty()) {
                    searchDialog = new Dialog(BluetoothSettingsActivity.this);
                    searchDialog.setContentView(R.layout.progress_dialog);
                    Window window = searchDialog.getWindow();
                    if (window != null) {
                        window.setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        TextView dialogMsg = searchDialog.findViewById(R.id.loading_msg);
                        dialogMsg.setText("Searching for device. Please wait...");
                        searchDialog.show();
                    }
                    // searchDialog = ProgressDialog.show(this, "", "Searching for device. Please wait...", true);
                    bluetoothManager.setDeviceFoundCallback(new BluetoothManager.DeviceFoundCallback() {
                        @Override
                        public void onDeviceFound(BluetoothDevice device) {
                            selectedDevice = device;
                            searchDialog.dismiss();
                            currentDevice.setText(GNSSSettingsStore.readExternalBTName(getApplicationContext()));
                        }

                        @Override
                        public void onDeviceNotFound() {
                            searchDialog.dismiss();
                            notFoundDlg.show();
                            currentDevice.setText(R.string.bts_no_device_selected);
                        }
                    });
                    bluetoothManager.getDeviceByName(GNSSSettingsStore.readExternalBTName(getApplicationContext()), this);
                }
            }

        } else {
            ((Switch) findViewById(R.id.bts_switch_external)).setChecked(false);
            findViewById(R.id.selectedDeviceItem).setVisibility(View.GONE);
            findViewById(R.id.testConnection).setVisibility(View.GONE);
            findViewById(R.id.scanDevices).setVisibility(View.GONE);
        }


        createDialogs();
    }

    private void createDialogs() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.bts_noDeviceSelected)
                .setPositiveButton(R.string.bsa_okbtn, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                });
        notSelectedDlg = builder.create();

        builder.setTitle(R.string.bts_notFoundDevice)
                .setPositiveButton(R.string.bsa_okbtn, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                });
        notFoundDlg = builder.create();


    }

    int MY_PERMISSIONS_LOCATION = 13;

    private void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        startActivity(enableBtIntent);
    }

    private boolean checkBluetoothEnabled() {

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.bsa_notenabled)
                    .setMessage(R.string.bsa_mustbeeanbled)
                    .setPositiveButton(R.string.bsa_okbtn, ((dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        enableBluetooth();
                    })).setCancelable(false)
                    .setNegativeButton(R.string.bsa_cancelbt, (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                    });
            builder.create().show();
            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public void scanDevices(View view) {
        checkBluetoothEnabled();
        scanDialog = new Dialog(view.getContext());
        scanDialog.setContentView(R.layout.progress_dialog);
        Window window = scanDialog.getWindow();
        if (window != null) {
            window.setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            TextView dialogMsg = scanDialog.findViewById(R.id.loading_msg);
            dialogMsg.setText("Scanning. Please wait...");
            scanDialog.show();
        }
        // scanDialog = ProgressDialog.show(this, "", "Scanning. Please wait...", true);
        bluetoothManager.setScanEndCallback(this);

        bluetoothManager.startScan(this);
    }

    public void testConnection(View view) {
        if (selectedDevice == null) {
            notSelectedDlg.show();
        } else {
            Dialog prdialog = new Dialog(view.getContext());
            prdialog.setContentView(R.layout.progress_dialog);
            Window window = prdialog.getWindow();
            if (window != null) {
                window.setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                TextView dialogMsg = prdialog.findViewById(R.id.loading_msg);
                dialogMsg.setText("Connecting. Please wait...");
                prdialog.show();
            }
            // ProgressDialog prdialog = ProgressDialog.show(this, "", "Connecting. Please wait...", true);

            bluetoothManager.testConnection(selectedDevice, new BluetoothManager.TestConnectCallback() {
                @Override
                public void onConnectionSuccess() {
                    prdialog.dismiss();
                    testResult = new ConnectionResultDialog(true);
                    testResult.show(getSupportFragmentManager(), "ResultDialogFragment");
                }

                @Override
                public void onConnectionFailure() {
                    prdialog.dismiss();
                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                        testResult = new ConnectionResultDialog(false);
                        testResult.show(getSupportFragmentManager(), "ResultDialogFragment");
                    }
                }

                @Override
                public void onDataReceived(String data) {
                    Toast.makeText(BluetoothSettingsActivity.this, "NMEA: $" + data, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onScanEnd(ArrayList<BluetoothDevice> devices) {
        scanDialog.dismiss();
        String[] deviceNames = new String[devices.size()];
        int i = 0;
        for (BluetoothDevice device : devices) {
            BluetoothClass clas = device.getBluetoothClass();
            //Filtering device type can be made with the class
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress(); // MAC address
            ParcelUuid[] uuids = device.getUuids();
            deviceList.put(deviceName, device);
            deviceNames[i] = deviceName;
            i++;

        }
        SelectDeviceDialog deviceDialog = new SelectDeviceDialog(deviceNames);
        getSupportFragmentManager().setFragmentResultListener("DEVICE_SELECT", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                selectedDevice = deviceList.get(result.getString("DeviceName"));
                currentDevice.setText(result.getString("DeviceName"));
                GNSSSettingsStore.saveExternalBTName(getApplicationContext(), result.getString("DeviceName"));
                //testBluetooth.setVisibility(View.VISIBLE);
                //bluetoothBtn.setVisibility(View.GONE);
            }
        });
        deviceDialog.show(getSupportFragmentManager(), "SelectDialogFragment");
    }

    public void externalDeviceToggle(View view) {
        if (((Switch) view).isChecked()) {
            findViewById(R.id.selectedDeviceItem).setVisibility(View.VISIBLE);
            findViewById(R.id.testConnection).setVisibility(View.VISIBLE);
            findViewById(R.id.scanDevices).setVisibility(View.VISIBLE);
            GNSSSettingsStore.saveExternalBT(getApplicationContext(), true);
        } else {
            findViewById(R.id.selectedDeviceItem).setVisibility(View.GONE);
            findViewById(R.id.testConnection).setVisibility(View.GONE);
            findViewById(R.id.scanDevices).setVisibility(View.GONE);
            GNSSSettingsStore.saveExternalBT(getApplicationContext(), false);
        }
    }
}
