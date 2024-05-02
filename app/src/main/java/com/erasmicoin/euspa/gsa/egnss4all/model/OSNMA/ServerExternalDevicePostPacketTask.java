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

public class ServerExternalDevicePostPacketTask extends AsyncTask<JSONObject, Void, ServerPostResponse> {

    public AsyncResponse delegate = null;

    private final static String SERVER_URL = "http://<your server url>/";

    private final static String TAG = "OSNMA-ServerPostPacketTask";
    private OkHttpClient client;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected ServerPostResponse doInBackground(JSONObject... jsonObjects) {

        JSONObject toSendObj = jsonObjects[0];
        try {
            toSendObj.put("source","client");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        RequestBody body = RequestBody.create(toSendObj.toString(), JSON);
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