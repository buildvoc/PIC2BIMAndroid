package com.erasmicoin.euspa.gsa.egnss4all.model.groundGeometry;

import com.erasmicoin.euspa.gsa.egnss4all.model.Requestor;
import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.collections.PolygonManager;

public interface GGMapActivity {
    GoogleMap getMap();

    void alert(String title, String text);

    Requestor getRequestor();

    PolygonManager getPolygonManager();

    MarkerManager getMarkerManager();
}

