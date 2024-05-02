package eu.foxcom.gnss_compare_core.Corrections;

import android.location.Location;

import com.example.ELFA.edas.ClientThread;
import com.example.ELFA.edas.sisnet.SbasCorrections;
import com.example.ELFA.edas.sisnet.SbasMessage;
import com.galfins.gogpsextracts.Coordinates;
import com.galfins.gogpsextracts.NavigationProducer;
import com.galfins.gogpsextracts.SatellitePosition;
import com.galfins.gogpsextracts.Time;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class SBASCorrection extends Correction{

    private ClientThread.SisnetClientListener listener;

    private ClientThread mClient;

    private boolean connected = false;

    private SbasMessage lastMessage;

    private double currentCorrection = 0;

    public SBASCorrection(ClientThread client) {
        mClient = client;
        listener =  new ClientThread.SisnetClientListener() {
            @Override
            public void onClientStateChange(String message, ClientThread.clientState state) {
                switch (state) {
                    case CONNECTED:
                        connected = true;
                        break;
                    case DISCONNECTED:
                        connected = false;
                        break;
                    case RECONNECTING:
                        System.out.println("Sisnet reconnecting...");
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + state);
                }
            }

            @Override
            public void onMessageReceived(SbasMessage message) {
                lastMessage = message;
            }
        };
    }

    @Override
    public void calculateCorrection(Time currentTime, Coordinates approximatedPose, SatellitePosition satelliteCoordinates, NavigationProducer navigationProducer, Location initialLocation) {
        SbasCorrections corrs = mClient.getSbasCorrections();
        Calendar satCal = corrs.getSatLongTimestamp(satelliteCoordinates.getSatID());
        long satTs = satCal.getTimeInMillis();
        int udreI = corrs.getSatUdreI(satelliteCoordinates.getSatID());
        Calendar currTime = GregorianCalendar.getInstance();
        currTime.setTimeInMillis(currentTime.getMsec());
        System.out.println("-----------------> SAT ID "+satelliteCoordinates.getSatID());
        double tempCorrection = corrs.getSatRangeCorrection(satelliteCoordinates.getSatID(), currTime);
        long currTs = currTime.getTimeInMillis();
        long timeDiff = Math.abs(currTime.getTimeInMillis() - satTs);
        if(udreI != 9999 && tempCorrection != 9999 && timeDiff < 30000){
            currentCorrection = tempCorrection;
        }else{
            currentCorrection = 0.0;
            return;
        }

    }

    @Override
    public double getCorrection() {
        return currentCorrection;
    }

    @Override
    public String getName() {
        return "SBAS Correction";
    }

    public void connectSISNET(String username, String password){
        //mClient.ConnectSisnet("egnos-edas.eu",7777, "avserasmicoin", "nsV5yHLD");
        mClient.ConnectSisnet("egnos-edas.eu",7777, username, password);
    }

    public void disconnectSISNET(){
        mClient.Disconnect();
    }
}
