package com.erasmicoin.euspa.gsa.egnss4all.model.pathTrack;

import android.location.Location;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.erasmicoin.euspa.gsa.egnss4all.model.AppDatabase;
import com.erasmicoin.euspa.gsa.egnss4all.model.Requestor;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.collections.PolygonManager;
import com.google.maps.android.collections.PolylineManager;

public interface PTMapActivity {
    AppCompatActivity getAppCompatActivity();

    GoogleMap getGoogleMap();

    AppDatabase getAppDatabase();

    Lifecycle getLifecycle();

    void startingPTException(Exception e);

    void stoppingPTException(Exception e);

    void onStartedPT();

    void onStoppedPT(PTPath ptPath);

    void onPausePT();

    void onContinuePT();

    void onNewUnfilteredLocationPT (Location location);

    void onNoPointsInPath();

    MarkerManager getMarkerManager();

    PolylineManager getPolylineManager();

    PolygonManager getPolygonManager();

    FrameLayout getInfoCurrentPathLayout();

    PTIsPathsUploadingBinder getPtIsPathsUploadingBinder();

    Requestor getRequestor();

    void uploadDrawnPathStarted();

    void uploadDrawnPathSuccess();

    void uploadDrawnPathFailed(String errMsg);

    void uploadDrawnPathComplete();

    void requestAnimatePTOnPoint(CameraUpdate cameraUpdate);

    void requestAnimatePTOnPath(CameraUpdate cameraUpdate);
}


