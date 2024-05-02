package com.erasmicoin.euspa.gsa.egnss4all;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.erasmicoin.euspa.gsa.egnss4all.model.GNSSLocation.GNSSSettingsStore;
import com.example.ELFA.edas.ClientThread;
import com.example.ELFA.edas.ntrip.MT1_Message;

public class SettingsGNSSLocationActivity extends BaseActivity {

    private Switch sbasSwitch;

    private RadioGroup sbasGroup;
    /*private Switch kalmanSwitch;
    private TextView seekbarLabel;
    private LinearLayout seekbarLayout;
    private EditText seekbarText;
    private SeekBar samplingSk;*/
    private ScrollView settingsscroller;
    private RadioGroup constellationGr;
    private RadioGroup methodGr;
    private CheckBox tropoCheckBox;
    private CheckBox ionoCheckBox;
    private CheckBox shapiroCheckBox;

    private RadioButton dgnss;
    private RadioButton sbas;
    private LinearLayout sbassettings;
    private Button defaultBtn;
    private EditText edasUsername;
    private EditText edasPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_location);
        setToolbar(R.id.toolbar);

        defaultBtn = findViewById(R.id.defaultbtn);
        constellationGr = findViewById(R.id.constSelection);
        methodGr = findViewById(R.id.pvtMethodSelection);

        tropoCheckBox = findViewById(R.id.tropobtn);
        ionoCheckBox = findViewById(R.id.ionobtn);
        shapiroCheckBox = findViewById(R.id.shapirobtn);


        sbasSwitch = findViewById(R.id.switchsbas);
        sbassettings = findViewById(R.id.sbassettings);
        sbasGroup = findViewById(R.id.sbasdgnss);
        edasUsername = findViewById(R.id.editTextEDASUsername);
        edasPassword = findViewById(R.id.editTextEDASPassword);

       /* kalmanSwitch = findViewById(R.id.switchkalman);
        seekbarLabel = findViewById(R.id.seekbarLabel);
        seekbarLayout = findViewById(R.id.seekbarlay);
        seekbarText = findViewById(R.id.seekbarText);
        samplingSk = findViewById(R.id.seekBarCamp);*/

        settingsscroller = findViewById(R.id.settingsscroller);

        dgnss = findViewById(R.id.dgnss);
        sbas = findViewById(R.id.sbas);

        appContext = getApplicationContext();

        init();
        loadSettings();
        prepareDialogs();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void init() {

        /*samplingSk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekbarText.setText(String.valueOf(progress));
                GNSSSettingsStore.savePositionCentroidSamples(getApplicationContext(), progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekbarText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() > 0){
                    if(Integer.parseInt(s.toString()) > GNSSSettingsStore.MAX_SAMPLINGS){
                        seekbarText.setText(GNSSSettingsStore.MAX_SAMPLINGS+"");
                    }else if(Integer.parseInt(s.toString()) < 0){
                        seekbarText.setText(0+"");
                    }
                    GNSSSettingsStore.savePositionCentroidSamples(getApplicationContext(), Integer.parseInt(seekbarText.getText().toString()));
                }


            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });*/

        sbasSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(((Switch)view).isChecked()){
                    sbasGroup.setVisibility(View.VISIBLE);
                    sbassettings.setVisibility(View.VISIBLE);
                    dgnss.setChecked(true);
                    GNSSSettingsStore.savePositionSBASActive(getApplicationContext(), true);
                }else{
                    sbasGroup.setVisibility(View.GONE);
                    sbassettings.setVisibility(View.GONE);
                    GNSSSettingsStore.savePositionSBASActive(getApplicationContext(), false);
                }
            }
        });

        /*kalmanSwitch.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(View view) {
                if(((Switch)view).isChecked()){
                    seekbarLabel.setVisibility(View.VISIBLE);
                    seekbarLayout.setVisibility(View.VISIBLE);
                    seekbarText.setText(String.valueOf(GNSSSettingsStore.readPositionCentroidSamples(getApplicationContext())));
                    samplingSk.setProgress(GNSSSettingsStore.readPositionCentroidSamples(getApplicationContext()));
                    settingsscroller.scrollToDescendant(seekbarLayout);
                    GNSSSettingsStore.savePositionCentroidActive(getApplicationContext(), true);
                }else{
                    seekbarLabel.setVisibility(View.GONE);
                    seekbarLayout.setVisibility(View.GONE);
                    GNSSSettingsStore.savePositionCentroidActive(getApplicationContext(), false);
                }
            }
        });*/

        tropoCheckBox.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                checkCorrections();
            }
        });

        ionoCheckBox.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                checkCorrections();
            }
        });

        shapiroCheckBox.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                checkCorrections();
            }
        });

        methodGr.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i){
                    case R.id.dekfbtn:
                        GNSSSettingsStore.savePositionPositioningMethod(getApplicationContext(), GNSSSettingsStore.DEK_FILTER_METHOD);
                        break;
                    case R.id.sekfbtn:
                        GNSSSettingsStore.savePositionPositioningMethod(getApplicationContext(), GNSSSettingsStore.SEK_FILTER_METHOD);
                        break;
                    case R.id.pekfbtn:
                        GNSSSettingsStore.savePositionPositioningMethod(getApplicationContext(), GNSSSettingsStore.PSEK_FILTER_METHOD);
                        break;
                    case R.id.wlsbtn:
                        GNSSSettingsStore.savePositionPositioningMethod(getApplicationContext(), GNSSSettingsStore.WLS_METHOD);
                        break;
                    default:
                        break;
                }

            }
        });
        constellationGr.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i == R.id.gpsbtn || i == R.id.gpsionobtn || i == R.id.gpsl5btn){
                    sbasSwitch.setChecked(false);
                    sbasSwitch.setClickable(false);
                    sbasSwitch.setTextColor(getColor(R.color.stateDisabled));
                    sbasGroup.setVisibility(View.GONE);
                    sbassettings.setVisibility(View.GONE);
                    GNSSSettingsStore.savePositionSBASActive(getApplicationContext(), false);
                }else{
                    sbasSwitch.setClickable(true);
                    sbasSwitch.setTextColor(getColor(R.color.textNormal));
                    sbasSwitch.setChecked(GNSSSettingsStore.readPositionSBASActive(getApplicationContext()));
                    sbasGroup.setVisibility(GNSSSettingsStore.readPositionSBASActive(getApplicationContext()) ? View.VISIBLE : View.GONE);
                    sbassettings.setVisibility(GNSSSettingsStore.readPositionSBASActive(getApplicationContext()) ? View.VISIBLE : View.GONE);

                }
                switch (i){
                    case R.id.galileoE1btn:
                        GNSSSettingsStore.savePositionConstellation(getApplicationContext(), GNSSSettingsStore.GALILEO_E1_CONSTELLATION);
                        break;
                    case R.id.galileoE1btn2:
                        GNSSSettingsStore.savePositionConstellation(getApplicationContext(), GNSSSettingsStore.GALILEO_E1_OSNMA_CONSTELLATION);
                        break;
                    case R.id.galileoe5btn:
                        GNSSSettingsStore.savePositionConstellation(getApplicationContext(), GNSSSettingsStore.GALILEO_E5_CONSTELLATION);
                        break;
                    case R.id.galileogpsbtn:
                        GNSSSettingsStore.savePositionConstellation(getApplicationContext(), GNSSSettingsStore.GALILEO_GPS_CONSTELLATION);
                        break;
                    case R.id.galileoionobtn:
                        GNSSSettingsStore.savePositionConstellation(getApplicationContext(), GNSSSettingsStore.GALILEO_IONOFREE_CONSTELLATION);
                        break;
                    case R.id.gpsbtn:
                        GNSSSettingsStore.savePositionConstellation(getApplicationContext(), GNSSSettingsStore.GPS_CONSTELLATION);
                        break;
                    case R.id.gpsionobtn:
                        GNSSSettingsStore.savePositionConstellation(getApplicationContext(), GNSSSettingsStore.GPS_IONOFREE_CONSTELLATION);
                        break;
                    case R.id.gpsl5btn:
                        GNSSSettingsStore.savePositionConstellation(getApplicationContext(), GNSSSettingsStore.GPS_L5_CONSTELLATION);
                        break;
                    default:
                        break;
                }
            }
        });


    }

    private void checkCorrections(){

        String[] corrections = new String[3];
        if(tropoCheckBox.isChecked()){
            corrections[0] = ""+GNSSSettingsStore.TROPO_CORRECTION;
        }
        if(ionoCheckBox.isChecked()){
            corrections[1] = ""+GNSSSettingsStore.IONO_CORRECTION;
        }
        if(shapiroCheckBox.isChecked()){
            corrections[2] = ""+GNSSSettingsStore.SHAPIRO_CORRECTION;
        }

        String corrJoined = TextUtils.join(",", corrections);
        GNSSSettingsStore.savePositionCorrections(getApplicationContext(), corrJoined);

    }

    private void loadSettings(){
        int currentConst = GNSSSettingsStore.readPositionConstellation(getApplicationContext());


        sbasSwitch.setTextColor(getColor(R.color.textNormal));
        switch(currentConst){
            case GNSSSettingsStore.GALILEO_E1_CONSTELLATION:
                constellationGr.check(R.id.galileoE1btn);
                sbasSwitch.setClickable(true);
                sbasGroup.setVisibility(View.VISIBLE);
                sbassettings.setVisibility(View.VISIBLE);
                break;
            case GNSSSettingsStore.GALILEO_GPS_CONSTELLATION:
                constellationGr.check(R.id.galileogpsbtn);
                sbasSwitch.setChecked(false);
                sbasSwitch.setClickable(true);
                sbasGroup.setVisibility(View.GONE);
                sbassettings.setVisibility(View.GONE);
                break;
            case GNSSSettingsStore.GALILEO_IONOFREE_CONSTELLATION:
                constellationGr.check(R.id.galileoionobtn);
                sbasSwitch.setClickable(true);
                sbasGroup.setVisibility(View.VISIBLE);
                sbassettings.setVisibility(View.VISIBLE);
                break;
            case GNSSSettingsStore.GPS_CONSTELLATION:
                constellationGr.check(R.id.gpsbtn);
                sbasSwitch.setChecked(false);
                sbasSwitch.setClickable(false);
                sbasSwitch.setTextColor(getColor(R.color.stateDisabled));
                sbasGroup.setVisibility(View.GONE);
                sbassettings.setVisibility(View.GONE);
                break;
            case GNSSSettingsStore.GALILEO_E5_CONSTELLATION:
                constellationGr.check(R.id.galileoe5btn);
                sbasSwitch.setClickable(true);
                sbasGroup.setVisibility(View.VISIBLE);
                sbassettings.setVisibility(View.VISIBLE);
                break;
            case GNSSSettingsStore.GPS_IONOFREE_CONSTELLATION:
                constellationGr.check(R.id.gpsionobtn);
                sbasSwitch.setChecked(false);
                sbasSwitch.setClickable(false);
                sbasGroup.setVisibility(View.GONE);
                sbassettings.setVisibility(View.GONE);
                break;
            case GNSSSettingsStore.GPS_L5_CONSTELLATION:
                constellationGr.check(R.id.gpsl5btn);
                sbasSwitch.setChecked(false);
                sbasSwitch.setClickable(false);
                sbasGroup.setVisibility(View.GONE);
                sbassettings.setVisibility(View.GONE);
                break;
            case GNSSSettingsStore.GALILEO_E1_OSNMA_CONSTELLATION:
                constellationGr.check(R.id.galileoE1btn2);
                sbasSwitch.setClickable(true);
                sbasGroup.setVisibility(View.VISIBLE);
                sbassettings.setVisibility(View.VISIBLE);
            default:
                break;
        }

        int currentMethod = GNSSSettingsStore.readPositionPositioningMethod(getApplicationContext());
        switch(currentMethod){
            case GNSSSettingsStore.DEK_FILTER_METHOD:
                methodGr.check(R.id.dekfbtn);
                break;
            case GNSSSettingsStore.SEK_FILTER_METHOD:
                methodGr.check(R.id.sekfbtn);
                break;
            case GNSSSettingsStore.PSEK_FILTER_METHOD:
                methodGr.check(R.id.pekfbtn);
                break;
            case GNSSSettingsStore.WLS_METHOD:
                methodGr.check(R.id.wlsbtn);
                break;
            default:
                break;
        }

        String[] currentCorrections = GNSSSettingsStore.readPositionCorrections(getApplicationContext()).split(",");
        tropoCheckBox.setChecked(false);
        ionoCheckBox.setChecked(false);
        shapiroCheckBox.setChecked(false);
        for(int i=0; i<currentCorrections.length; i++){
            try{
                int correction = Integer.parseInt(currentCorrections[i]);
                switch(correction){
                    case GNSSSettingsStore.TROPO_CORRECTION:
                        tropoCheckBox.setChecked(true);
                        break;
                    case GNSSSettingsStore.IONO_CORRECTION:
                        ionoCheckBox.setChecked(true);
                        break;
                    case GNSSSettingsStore.SHAPIRO_CORRECTION:
                        shapiroCheckBox.setChecked(true);
                        break;
                    default:
                        break;
                }
            }catch(NumberFormatException nfe){
                Log.d(MainActivity.TAG, "No corrections for A");
            }
        }

        boolean sbasActive = GNSSSettingsStore.readPositionSBASActive(getApplicationContext());
        sbasSwitch.setChecked(sbasActive);


        if(sbasActive){
            sbasGroup.setVisibility(View.VISIBLE);
            sbassettings.setVisibility(View.VISIBLE);
            int sbastype = GNSSSettingsStore.readPositionSBASType(getApplicationContext());
            if(sbastype == GNSSSettingsStore.DGNSS_CORRECTION){
                sbasGroup.check(R.id.dgnss);
            }else{
                sbasGroup.check((R.id.sbas));
            }
            edasUsername.setText(GNSSSettingsStore.readEDASUsername(getApplicationContext()));
            edasPassword.setText(GNSSSettingsStore.readEDASPassword(getApplicationContext()));
        }else{
            sbasGroup.setVisibility(View.GONE);
            sbassettings.setVisibility(View.GONE);
        }

        /*boolean centroidActive = GNSSSettingsStore.readPositionCentroidActive(getApplicationContext());
        kalmanSwitch.setChecked(centroidActive);
        if(centroidActive){
            seekbarLabel.setVisibility(View.VISIBLE);
            seekbarLayout.setVisibility(View.VISIBLE);
            int centroidSamples = GNSSSettingsStore.readPositionCentroidSamples(getApplicationContext());
            seekbarText.setText(centroidSamples+"");
            samplingSk.setProgress(centroidSamples);

        }else{
            seekbarLabel.setVisibility(View.GONE);
            seekbarLayout.setVisibility(View.GONE);
        }*/

    }

    public static Context appContext;

    public static Context getAppContext(){
        return appContext;
    }

    private AlertDialog noCredentialsDlg;
    private AlertDialog wrongCredentialsDlg;
    private AlertDialog goodCredentialsDlg;

    private void prepareDialogs(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.sla_goodedascredentials)
                .setPositiveButton("OK",(dialogInterface, i) -> {
                    dialogInterface.dismiss();
                });
        goodCredentialsDlg = builder.create();

        builder.setTitle(R.string.sla_wrongedascredentials)
                .setPositiveButton("OK",(dialogInterface, i) -> {
                    dialogInterface.dismiss();
                });
        wrongCredentialsDlg = builder.create();

        builder.setTitle(R.string.sla_noedascredentials)
                .setPositiveButton("OK",(dialogInterface, i) -> {
                    dialogInterface.dismiss();
                });
        noCredentialsDlg = builder.create();

    }

    public void testEDASCredentials(View view) {
        String username = ((EditText)findViewById(R.id.editTextEDASUsername)).getEditableText().toString();
        String password = ((EditText)findViewById(R.id.editTextEDASPassword)).getEditableText().toString();



        ClientThread mClient = new ClientThread();
        final boolean[] hasConnected = {false};
        ClientThread.NtripClientListener listener = new ClientThread.NtripClientListener() {
            @Override
            public void onClientStateChange(String message, ClientThread.clientState state) {
                switch (state) {
                    case CONNECTED:
                        runOnUiThread(() -> goodCredentialsDlg.show());
                        GNSSSettingsStore.saveEDASUsername(appContext, username);
                        GNSSSettingsStore.saveEDASPassword(appContext, password);
                        GNSSSettingsStore.savePositionSBASActive(appContext, true);
                        hasConnected[0] = true;
                        mClient.Disconnect();
                        break;
                    case DISCONNECTED:
                        if(!hasConnected[0]){
                            GNSSSettingsStore.saveEDASUsername(appContext, null);
                            GNSSSettingsStore.saveEDASPassword(appContext, null);
                            GNSSSettingsStore.savePositionSBASActive(appContext, false);
                            runOnUiThread(() -> wrongCredentialsDlg.show());
                        }
                        break;
                    case RECONNECTING:
                        Log.d("SSSS","AAAA");
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + state);
                }
            }

            @Override
            public void onMessageReceived(MT1_Message message) {
                System.out.println("Ricevuto messaggio DGNSS");
                //System.out.println(message.toString());
            }
        };
        mClient.setNtripClientListener(listener);

        if(!username.isEmpty() && !password.isEmpty()){
            int sbastype = GNSSSettingsStore.readPositionSBASType(getApplicationContext());
            if(sbastype == GNSSSettingsStore.DGNSS_CORRECTION){
                mClient.ConnectNtrip("egnos-edas.eu",2101, username,password, "ROMA_2401");
            }else{
                mClient.ConnectSisnet("egnos-edas.eu",7777, username, password);
            }

        }else{
            runOnUiThread(() -> noCredentialsDlg.show());
        }


    }

    public void resetDefaults(View view) {
        GNSSSettingsStore.resetDefaultPosition(appContext);
        loadSettings();
    }
}
