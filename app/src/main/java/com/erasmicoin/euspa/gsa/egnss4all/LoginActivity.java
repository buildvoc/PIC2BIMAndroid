package com.erasmicoin.euspa.gsa.egnss4all;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.erasmicoin.euspa.gsa.egnss4all.model.GNSSLocation.GNSSSettingsStore;
import com.erasmicoin.euspa.gsa.egnss4all.model.LoggedUser;
import com.erasmicoin.euspa.gsa.egnss4all.model.Requestor;
import com.erasmicoin.euspa.gsa.egnss4all.model.mock.UserMock;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends BaseActivity {

    public final String TAG = LoginActivity.class.getName();

    private boolean isServerLastConnected = false;

    private TextView selectServerBtn;

    private AlertDialog selectServerDialog;
    private Context mCtx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        setContentView(R.layout.activity_login);

        this.mCtx = this;
        selectServerBtn = findViewById(R.id.lg_select_server_btn);
        selectServerBtn.setOnClickListener(v -> openSelectServerDialog());

    }

    private void openSelectServerDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);

        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_change_server, null);
        builder.setView(customLayout);
        String currentServer = GNSSSettingsStore.readCurrentServer(mCtx);
        RadioButton defaultServer = customLayout.findViewById(R.id.cgs_default_server);
        RadioButton customServer = customLayout.findViewById(R.id.cgs_custom_server);
        TextInputLayout customServerUrlLayout = customLayout.findViewById(R.id.cgs_textInputLayout_server);
        TextInputEditText customServerUrl = customLayout.findViewById(R.id.cgs_textInputEditText_server);
        Button saveChanges = customLayout.findViewById(R.id.cgs_btn_confirm_server);
        TextView urlErrorMsg = customLayout.findViewById(R.id.cgs_textView_msg);

        customServerUrl.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
            }
        });

        customServer.setOnCheckedChangeListener((buttonView, isChecked) -> {
                customServerUrl.setEnabled(isChecked);
                customServerUrlLayout.setEnabled(isChecked);
                if(isChecked){
                    customServerUrl.requestFocus();
                }
        });
        if(currentServer.equals(BaseActivity.SERVER_BASE_URL)){
            defaultServer.setChecked(true);
        }else{
            customServer.setChecked(true);
            customServerUrl.setText(currentServer);
        }

        saveChanges.setOnClickListener(v -> {
            if(defaultServer.isChecked()){
                GNSSSettingsStore.saveCurrentServer(mCtx, BaseActivity.SERVER_BASE_URL);
                selectServerDialog.dismiss();
            }else{

                try {
                    String customServerUrlText = customServerUrl.getEditableText().toString();
                    URI testUrl = new URL(customServerUrlText).toURI();
                    urlErrorMsg.setVisibility(View.GONE);
                    GNSSSettingsStore.saveCurrentServer(mCtx, customServerUrlText);
                    selectServerDialog.dismiss();
                } catch (URISyntaxException | MalformedURLException e) {
                    urlErrorMsg.setVisibility(View.VISIBLE);
                }
            }
        });

        selectServerDialog = builder.create();
        selectServerDialog.show();
    }

    @Override
    public void serviceInit() {
        super.serviceInit();
    }

    private void showMessage(String message) {
        TextView messageTextView = findViewById(R.id.lg_textView_msg);
        if (message != null && !message.isEmpty()) {
            messageTextView.setVisibility(View.VISIBLE);
        } else {
            messageTextView.setVisibility(View.GONE);
        }
        messageTextView.setText(message);
    }

    public void tryLogin(View view) {
        beginLogin();
        isServerLastConnected = false;
        final TextView loginTextView = findViewById(R.id.lg_textInputEditText_login);
        final TextView passwordTextView = findViewById(R.id.lg_textInputEditText_password);

        String currentServer = GNSSSettingsStore.readCurrentServer(mCtx);
        MS.getRequestor().requestAuth(currentServer+"/egnss4allservices/comm_login.php", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                isServerLastConnected = true;
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String status = jsonObject.getString("status").trim();
                    String login = loginTextView.getText().toString();
                    String password = passwordTextView.getText().toString();
                    LoggedUser loggedUser;
                    if (login.equals("mockUser") && password.equals("mocking")) {
                        loggedUser = UserMock.createUserMock();
                    } else if (!status.equals("ok")) {
                        String errorMsg = jsonObject.getString("error_msg");
                        endLogin(null);
                        return;
                    } else {
                        loggedUser = LoggedUser.createFromResponse(jsonObject.getJSONObject("user"), loginTextView.getText().toString(), new DateTime());
                    }
                    LoggedUser.login(MS.getAppDatabase(), loggedUser);
                    MS.syncAll();
                    goToMainActivity();
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                    endLogin(null);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                endLogin(error);
            }
        }, new Requestor.Req() {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("login", loginTextView.getText().toString());
                params.put("pswd", passwordTextView.getText().toString());
                return params;
            }
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MS.getMainClass());
        startActivity(intent);
        finish();
    }

    private void beginLogin() {
        showMessage("");
        final ProgressBar progressBar = findViewById(R.id.lg_progressBar);
        progressBar.setVisibility(View.VISIBLE);
    }

    private String createErrMsg() {
        String errMsg;
        if (isServerLastConnected) {
           errMsg = getString(R.string.lg_loginFailed, getString(R.string.lg_loginFailedWrongData));
        } else {
            errMsg = getString(R.string.lg_loginFailed, getString(R.string.lg_loginFailedNoServer));
        }
        return errMsg;
    }

    private void endLogin(VolleyError error) {
        if(error != null && (error.getCause() instanceof UnknownHostException || error instanceof TimeoutError)){
            String errMsg = getString(R.string.lg_loginFailed, getString(R.string.lg_loginFailedWrongHost));
            showMessage(errMsg);
        }else{
            showMessage(createErrMsg());
        }

        final ProgressBar progressBar = findViewById(R.id.lg_progressBar);
        progressBar.setVisibility(View.GONE);
    }
}

