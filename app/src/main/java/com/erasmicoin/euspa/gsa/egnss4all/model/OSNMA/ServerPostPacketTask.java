package com.erasmicoin.euspa.gsa.egnss4all.model.OSNMA;

import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.erasmicoin.euspa.gsa.egnss4all.AsyncResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class ServerPostPacketTask extends AsyncTask<String, Void, ServerPostResponse> {

    public AsyncResponse delegate = null;
    private final static String SERVER_URL = "http://<your server url>";

    private final static String TAG = "OSNMA-ServerPostPacketTask";
    private OkHttpClient client;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected ServerPostResponse doInBackground(String... strings) {
        JSONObject packet = new JSONObject();

        String timestamp = strings[0];
        String sat_id = strings[1];
        String gnssid = strings[2];
        String inav_string = strings[3];

        String deviceManufacturer = strings[4];
        String deviceModel = strings[5];
        String deviceUUID = strings[6];
        String deviceVer = strings[7];

        long NTPTimeOffset = 0;//Long.parseLong(strings[6]);
        long NTPTime = 0; //Long.parseLong(strings[7]);

        try {
            packet.put("timestamp", timestamp);
            packet.put("wn",0);
            packet.put("tow",0);
            packet.put("sat_id", sat_id);
            packet.put("gnssid", gnssid);
            packet.put("inav_string", inav_string);
            packet.put("ntp_offset", NTPTimeOffset/1000L);
            packet.put("ntp_time", NTPTime);
            packet.put("manufacturer", deviceManufacturer);
            packet.put("model", deviceModel);
            packet.put("uuid", deviceUUID);
            packet.put("version",deviceVer);

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
            return new ServerPostResponse(networkResp.getString("validity_check"), 0, 0);

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return new ServerPostResponse(ServerPostResponse.CALL_EXCEPTION, 0, -1);
        }
    }

    @Override
    protected void onPostExecute(ServerPostResponse result) {
        super.onPostExecute(result);
    }

}