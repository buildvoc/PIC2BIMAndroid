package com.erasmicoin.euspa.gsa.egnss4all;

import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class ServerGetSatelllitesTask extends AsyncTask<String, Void, JSONObject> {

    public AsyncResponse delegate = null;

    private final static String SERVER_URL = "https://<your tle server url>/getSatellites";


    private final static String TAG = "ServerGetSatellitesTask";
    private OkHttpClient client;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected JSONObject doInBackground(String... strings) {
        JSONObject packet = new JSONObject();

        String latitude = strings[0];
        String longitude = strings[1];
        String constType = strings[2];
        try {
            packet.put("lat", Double.valueOf(latitude));
            packet.put("lon", Double.valueOf(longitude));
            packet.put("type", constType);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        RequestBody body = RequestBody.create(packet.toString(), JSON);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(SERVER_URL)
                .post(body)
                .build();

        okhttp3.Response response = null;
        try {
            response = client.newCall(request).execute();
            JSONObject networkResp = new JSONObject(response.body().string());
            return networkResp;

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(JSONObject result) {
        super.onPostExecute(result);
    }

}