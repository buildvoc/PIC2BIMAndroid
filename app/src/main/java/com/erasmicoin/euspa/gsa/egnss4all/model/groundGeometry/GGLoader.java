package com.erasmicoin.euspa.gsa.egnss4all.model.groundGeometry;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import co.uk.pic2bim.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import com.erasmicoin.euspa.gsa.egnss4all.model.GNSSLocation.GNSSSettingsStore;
import com.erasmicoin.euspa.gsa.egnss4all.model.Requestor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class GGLoader {
    private final static String SERVER_URL = "api.buildingshistory.co.uk";
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
        Log.d("GGLoader", "load");
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host(SERVER_URL)
                .addPathSegment("api")
                .addPathSegment("v1")
                .addPathSegment("ward-south-east")
                .addQueryParameter("max_lat", String.valueOf(ggRegion.getMaxLat()))
                .addQueryParameter("min_lat", String.valueOf(ggRegion.getMinLat()))
                .addQueryParameter("max_lng", String.valueOf(ggRegion.getMaxLng()))
                .addQueryParameter("min_lng", String.valueOf(ggRegion.getMinLng()))
                .build();
        Log.d("GGLoader", url.toString());
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                call.cancel();
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.body() != null) {
                        Activity activity = (Activity) ctx;
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        activity.runOnUiThread(() -> {
                            try {
                                JSONObject dataObject = jsonObject.getJSONObject("data");
                                JSONArray features = dataObject.getJSONArray("features");
                                List<GGObject> ggObjects = GGObject.createListFromResponse(features);
                                Log.d("GGLoader", String.valueOf(ggObjects.size()));
                                ggManager.loaderLoadGrounds(ggObjects);
                            } catch (JSONException | GGObject.GGParseException e) {
                                ggManager.exception(ggManager.getContext().getString(R.string.map_unexpectedExceptionGG),
                                        ggManager.getContext().getString(R.string.map_exceptionDuringParsingGG), e);
                            }
                        });
                    }
                } catch (JSONException e) {
                    ggManager.exception(ggManager.getContext().getString(R.string.map_unexpectedExceptionGG),
                            ggManager.getContext().getString(R.string.map_exceptionDuringParsingGG), e);
                }
            }
        });
    }

    void load2(GGRegion ggRegion, Context ctx) {
        String currentServer = GNSSSettingsStore.readCurrentServer(ctx);
            requestor.requestAuth(currentServer+"comm_shapes", response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                String status = jsonObject.getString("status");
                if (!status.equals("ok")) {
                    String errMgs = jsonObject.getString("error_msg");
                    ggManager.exception(ggManager.getContext().getString(R.string.map_unexpectedExceptionGG),
                            ggManager.getContext().getString(R.string.map_exceptionAfterDownloadGG) + "\n\n" + errMgs, null);
                    return;
                }
                JSONArray shapes = jsonObject.getJSONArray("shapes");
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


