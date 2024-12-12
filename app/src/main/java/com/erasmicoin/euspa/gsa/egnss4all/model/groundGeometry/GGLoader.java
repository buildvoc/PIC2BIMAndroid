package com.erasmicoin.euspa.gsa.egnss4all.model.groundGeometry;

import android.content.Context;
import android.util.Log;

import co.uk.pic2bim.R;
import com.erasmicoin.euspa.gsa.egnss4all.model.GNSSLocation.GNSSSettingsStore;
import com.erasmicoin.euspa.gsa.egnss4all.model.Requestor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GGLoader {
    private GGManager ggManager;

    private Requestor requestor;
    private DecimalFormat decimalFormat;

    public GGLoader(GGManager ggManager, Requestor requestor) {
        this.ggManager = ggManager;
        this.requestor = requestor;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        // ! server do not know Czech
        symbols.setDecimalSeparator('.');
        // ! emty char
        symbols.setGroupingSeparator('\u0000');
        this.decimalFormat = new DecimalFormat("#.0###############", symbols);
    }

    void load(GGRegion ggRegion, Context ctx) {
        String currentServer = GNSSSettingsStore.readCurrentServer(ctx);
            requestor.requestAuth(currentServer+"comm_shapes", response -> {
            try {
                JSONObject jsonObject = new JSONObject("{ \"status\": \"ok\", \"error_msg\": null, \"shapes\": [ { \"identificator\": \"SURREY.13399\", \"wgs_geometry\": \"[ [ [ -0.808977288, 51.212073995 ], [ -0.809045188, 51.212116595 ], [ -0.809069888, 51.212132095 ], [ -0.809128088, 51.212168595 ], [ -0.809282288, 51.212071695 ], [ -0.809131588, 51.211977095 ], [ -0.808977288, 51.212073995 ] ] ]\" } ]}");
                String status = jsonObject.getString("status");
                if (!status.equals("ok")) {
                    String errMgs = jsonObject.getString("error_msg");
                    ggManager.exception(ggManager.getContext().getString(R.string.map_unexpectedExceptionGG),
                            ggManager.getContext().getString(R.string.map_exceptionAfterDownloadGG) + "\n\n" + errMgs, null);
                    return;
                }
                JSONArray shapes = jsonObject.getJSONArray("shapes");
                Log.d("GGLoader", shapes.toString());
                List<GGObject> ggObjects = GGObject.createListFromResponse(shapes);
                ggManager.loaderLoadGrounds(ggObjects);
            } catch (JSONException | GGObject.GGParseException e) {
                ggManager.exception(ggManager.getContext().getString(R.string.map_unexpectedExceptionGG),
                        ggManager.getContext().getString(R.string.map_exceptionDuringParsingGG), e);
            }
        }, error -> {
            ggManager.exception(ggManager.getContext().getString(R.string.map_unexpectedExceptionGG),
                    ggManager.getContext().getString(R.string.map_exceptionDuringDownloadGG),
                    error);
        }, new Requestor.Req() {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("max_lat", decimalFormat.format(ggRegion.getMaxLat()));
                params.put("min_lat", decimalFormat.format(ggRegion.getMinLat()));
                params.put("max_lng", decimalFormat.format(ggRegion.getMaxLng()));
                params.put("min_lng", decimalFormat.format(ggRegion.getMinLng()));
                return params;
            }
        });
    }

}


