package com.erasmicoin.euspa.gsa.egnss4all.model.extbluetooth;

import android.location.Location;

import com.google.android.gms.maps.LocationSource;

public class BluetoothLocationSource implements LocationSource {

    private BluetoothManager bluetoothManager;
    private OnLocationChangedListener onLocationChangedListener;
    private Location lastLocation;
    private boolean isActive = false;

    BluetoothLocationSource(BluetoothManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        this.onLocationChangedListener = onLocationChangedListener;
        lastLocation = null;
        isActive = true;
    }

    @Override
    public void deactivate() {
        isActive = false;
    }

    void onNewLocation(Location location) {
        if (
                !isActive
                        || onLocationChangedListener == null
                        || location == null
                        || (lastLocation != null && location.getTime() == lastLocation.getTime())) {
            return;
        }
        onLocationChangedListener.onLocationChanged(location);
        lastLocation = location;
    }
}


