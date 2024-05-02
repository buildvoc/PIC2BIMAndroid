package eu.foxcom.gnss_compare_core.Corrections;

import android.content.Context;
import android.location.Location;

import com.example.ELFA.edas.ClientThread;
import com.example.ELFA.edas.ntrip.MT1_Message;
import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gogpsextracts.NavigationProducer;
import com.galfins.gogpsextracts.SatellitePosition;
import com.galfins.gogpsextracts.Time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DGNSSCorrection extends Correction{

    private ClientThread mClient;
    public boolean clientConnected = false;

    private MT1_Message latestMessage;

    private ClientThread.NtripClientListener listener;

    private double currentCorr;

    private Context mAppContext;

    public DGNSSCorrection(ClientThread client, Context ctx) {
        mClient = client;
        mAppContext = ctx;

        listener = new ClientThread.NtripClientListener() {
            @Override
            public void onClientStateChange(String message, ClientThread.clientState state) {
                switch (state) {
                    case CONNECTED:
                        clientConnected = true;
                        break;
                    case DISCONNECTED:
                        clientConnected = false;
                        break;
                        //throw new IllegalStateException("Client disconnected");
                    case RECONNECTING:
                        //setAsStopReconnect();
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + state);
                }
            }

            @Override
            public void onMessageReceived(MT1_Message message) {
                latestMessage = message;
                System.out.println("Ricevuto messaggio DGNSS");
                //System.out.println(message.toString());
            }
        };
        mClient.setNtripClientListener(listener);


    }

    @Override
    public void calculateCorrection(Time currentTime, Coordinates approximatedPose, SatellitePosition satelliteCoordinates, NavigationProducer navigationProducer, Location initialLocation) {
        //currentCorr = latestMessage.getPseudoRangeCorr(satelliteCoordinates.getSatID());
        MT1_Message msg = mClient.getLastMessageType1();
        if(!msg.isSatIncluded(satelliteCoordinates.getSatID())){
            currentCorr = 0.0;
        }else{
            Double decZCount = msg.getDecodedZCount();
            long tolongzcount = decZCount.longValue();
            double gpsTime = currentTime.getRoundedGpsTime();

            SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss", Locale.ITALY);
            Date d1 = null;
            String dateInString = "6-Gen-1980 00:00:00";
            try {
                d1 = formatter.parse(dateInString);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Date d2 = new Date();
            long seconds = (d2.getTime()-d1.getTime())/1000;

            long timebtwtss = seconds - tolongzcount;

            double lastCorr = msg.getPseudoRangeCorr(satelliteCoordinates.getSatID());

            int iod = msg.getIod(satelliteCoordinates.getSatID());

            if(lastCorr != 10485.76 && iod != -1 && Math.abs(timebtwtss) < 30 ){
                currentCorr = lastCorr;
                System.out.println("Ultima correzione DGNSS: "+lastCorr);
            }else{
                return;
            }
        }
    }

    @Override
    public double getCorrection() {
        return currentCorr;
    }

    @Override
    public String getName() {
        return "DGNSSCorrection with NTRIP";
    }

    public void connectNTRIP(String username, String password){
        //mClient.ConnectNtrip("egnos-edas.eu",2101, "avserasmicoin", "nsV5yHLD", "ROMA_2401");
        mClient.ConnectNtrip("egnos-edas.eu",2101, username, password, "ROMA_2401");
    }

    public void disconnectNTRIP(){
        mClient.Disconnect();
    }
}

