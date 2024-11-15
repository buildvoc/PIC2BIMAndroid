package com.example.ELFA.edas;

import static java.lang.String.format;

import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.ELFA.edas.connection.ConnectionTask;
import com.example.ELFA.edas.connection.NtripClient;
import com.example.ELFA.edas.connection.SisnetClient;
import com.example.ELFA.edas.connection.TcpClient;
import com.example.ELFA.edas.ntrip.MT1_Message;
import com.example.ELFA.edas.sisnet.MessageHandler;
import com.example.ELFA.edas.sisnet.SbasCorrections;
import com.example.ELFA.edas.sisnet.SbasMessage;

import java.text.SimpleDateFormat;
import java.util.concurrent.LinkedBlockingQueue;

/*
 * This class implements a queue with lock and event functions
 */
public class ClientThread extends HandlerThread {
    private final static int MAX_PRNS = 32;
    private final static int QUEUE_SIZE = 50;
    private Handler mHandler;
    private SimpleDateFormat df;
    private final LinkedBlockingQueue<MT1_Message> mNtripQueue;
    private SbasCorrections mSbasCorrections;
    private final LinkedBlockingQueue<SbasMessage> mSisnetQueue;
    private MessageHandler mEgnosHandler;
    private TcpClient mTcpClient;
    private static final int USED_COLOR = Color.rgb(0x4a, 0x5f, 0x70);

    public NtripClientListener mNtripClientListener = new NtripClientListener() {};
    public SisnetClientListener mSisnetClientListener = new SisnetClientListener() {};

    /**
     * Constructor
     */
    public ClientThread()
    {
        super("clientThread");
        mNtripQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
        mSisnetQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
        df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        mSbasCorrections = new SbasCorrections();
        mEgnosHandler = new MessageHandler(mSbasCorrections);
    }

    /**
     * Enum with possible client states
     */
    public enum clientState {
        CONNECTED,
        DISCONNECTED,
        RECONNECTING
    }

    /**
     * Interface to pass information from Ntrip Client to UI
     */
    public interface NtripClientListener {
        /**
         * Method called when Ntrip Client state changes
         * @param message: message indicating cause of status change
         * @param state: new client state
         */
        default void onClientStateChange(String message, clientState state) {}

        /**
         * Method called when an Ntrip message is received
         * @param message: message received
         */
        default void onMessageReceived(MT1_Message message) {}
    }

    /**
     * Interface to pass information from Sisnet Client to UI
     */
    public interface SisnetClientListener {
        /**
         * Method called when Sisnet Client state changes
         * @param message: message indicating cause of status change
         * @param state: new client state
         */
        default void onClientStateChange(String message, clientState state) {}

        /**
         * Method called when an Sisnet message is received
         * @param message: message received
         */
        default void onMessageReceived(SbasMessage message) {}
    }

    /**
     * Method to set listener (Interface to pass information from Ntrip client to UI)
     * @param listener: Listener
     */
    public void setNtripClientListener(NtripClientListener listener) {
        this.mNtripClientListener = listener;
    }

    /**
     * Method to set listener (Interface to pass information from Sisnet client to UI)
     * @param listener: Listener
     */
    public void setSisnetClientListener(SisnetClientListener listener) {
        this.mSisnetClientListener = listener;
    }

    /**
     * Event loop of the client thread. New events shall be sent here in an Android message in order to be treated.
     */
    @Override
    protected void onLooperPrepared() {
        mHandler = new Handler(getLooper()) {
            @Override
            public void handleMessage(@NonNull final android.os.Message msg) {

                if (msg.obj instanceof MT1_Message) {
                    // Add MT1 Message to the NTRIP queue
                    addNtripElement((MT1_Message) msg.obj);

                    // Log to console
                    logMessage((MT1_Message) msg.obj);

                    // Notify listener
                    mNtripClientListener.onMessageReceived((MT1_Message) msg.obj);
                } else if(msg.obj instanceof SbasMessage) {
                    // Process message
                    mEgnosHandler.handleMessage((SbasMessage) msg.obj);

                    // Log to console
                    // (replaced by detailed message logging according to type in Message Handler)
                    // logEGNOSMessage((SbasMessage) msg.obj);

                    // Notify listener
                    mSisnetClientListener.onMessageReceived((SbasMessage) msg.obj);
                }
            }
        };
    }

    /**
     * Method to connect the Ntrip Client
     *@param ip: String with the ip address of the server
     *@param port: int with the connection port
     *@param username: String with username
     *@param password: String with the password
     *@param mountpoint: String with the mountpoint
     */
    public void ConnectNtrip(String ip, int port, String username, String password, String mountpoint)
    {
        Disconnect();
        mTcpClient = new NtripClient(this);
        new ConnectionTask(ip, port, username, password, "NTRIP", mountpoint, mTcpClient).start();
    }

    /**
     * Method to connect the Sisnet Client
     *@param ip: String with the ip address of the server
     *@param port: int with the connection port
     *@param username: String with username
     *@param password: String with the password
     */
    public void ConnectSisnet(String ip, int port, String username, String password)
    {
        Disconnect();
        mTcpClient = new SisnetClient(this);
        new ConnectionTask(ip, port, username, password, "SISNET", "", mTcpClient).start();
    }

    /**
     * Method to disconnect the Ntrip or SiSNET Client
     */
    public void Disconnect() {
        if (mTcpClient != null) {
            if (mTcpClient instanceof  NtripClient) {
                mTcpClient.stopClient();
                mNtripClientListener.onClientStateChange("Disconnected", clientState.DISCONNECTED);
            }
            else if (mTcpClient instanceof SisnetClient) {
                mTcpClient.sendMessage("STOP");
                mTcpClient.stopClient();
                mSisnetClientListener.onClientStateChange("Disconnected", clientState.DISCONNECTED);
            }
        }
    }

    /**
     * Method to get the Correction Data.
     * The method returns a SbasCorrections object with default corrections
     *  values if no corrections have been set.
     * 
     *@return correctionData : CorrectionData
     */
    public SbasCorrections getSbasCorrections()
    {
        return mSbasCorrections;
    }

    /**
     * Method to send an Android message with an event to be treated into the client thread.
     *@param msg Android message
     */
    public void sendMessage(Object msg) {
        android.os.Message androidMsg = Message.obtain(mHandler, 0, msg);
        mHandler.sendMessage(androidMsg);
    }

    /**
     * Method to log a received MT1 message.
     *@param mt1Msg MT1 message
     */
    private void logMessage(MT1_Message mt1Msg) {
        Log.d("RTCM2 - MT1: ","Logging new message \n");
        for (int i = 1; i <= MAX_PRNS; i++) {
            if (mt1Msg.isSatIncluded(i)) {
                String decodedMessage = "MESSAGE Type 1 DECODED DATA:\n" +
                        "zcount " + mt1Msg.getDecodedZCount() + "\n" +
                        "UDRE " + mt1Msg.getUdre(i) + "\n" +
                        "SatID " + i + "\n" +
                        "PseudoRangeCorrection " + mt1Msg.getPseudoRangeCorr(i) + "\n" +
                        "RangeRateCorrection " + mt1Msg.getRangeRateCorr(i) + "\n" +
                        "IOD " + mt1Msg.getIod(i) + "\n\n";
                Log.d("MT1 log", decodedMessage);
            }
        }
    }

    /**
     * Method to log a received EGNOSMessage.
     *@param egnosMsg EGNOS message
     */
    private void logEGNOSMessage(SbasMessage egnosMsg) {
        df.setTimeZone(egnosMsg.getCalendar().getTimeZone());
        Log.i("Sisnet", format("Message received at: %s (UTC)", df.format(egnosMsg.getCalendar().getTime())));
        Log.i("Sisnet", format("Message type:  %d", egnosMsg.getType()));
        Log.i("Sisnet", format("Message hexadecimal string:  %s", egnosMsg.getMesasgeHex()));
    }

    /**
     * Method to add a MT1 Message received from NTRIP in NTRIP Queue.
     *@param msg: MT1 Message
     */
    private void addNtripElement(MT1_Message msg) {
        while (mNtripQueue.offer(msg)==false) {
            mNtripQueue.poll();
        }
    }

    /**
     * Method to add a EGNOS message received from SiSNET in SiSNET Queue.
     *@param msg: EgnosMessage
     */
    private void addSisnetElement(SbasMessage msg) {
        while (mSisnetQueue.offer(msg)==false) {
            mSisnetQueue.poll();
        }
    }

    /**
     * Method to get the last element in NTRIP Queue (removing all the messages but the last one from the queue)
     */
    public MT1_Message getLastMessageType1(){
        while (mNtripQueue.size() > 1){
            mNtripQueue.poll();
        }

        // Return last element without removing it
        return mNtripQueue.peek();
    }

}

