package com.example.ELFA.edas.connection;

import android.util.Log;

import com.example.ELFA.edas.ClientThread;
import com.example.ELFA.edas.ntrip.RTCM2_MessageBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class provides methods for connecting, sending and receiving stream data over a network in asynchronous mode.
 */
public class NtripClient extends TcpClient {
    private NtripBufferExtractor bExtractor;
    private LinkedBlockingQueue<Byte> mQueue;
    private NtripQueueReader qReader;
    private BufferedInputStream mBufferIn;
    private int iTimeToReconnect = 0;
    private int iTimeToReconnectCap = 15 * 60;
    private int iReconnections = 0;
    private Socket mSocket;

    /**
     * Constructor
     * @param clientThread Reference of client thread instance
     */
    public NtripClient(ClientThread clientThread) {
        super(clientThread);
    }

    /**
     * Method to close the connection and release the members
     */
    public void stopClient() {
        if (bExtractor != null)
            bExtractor.stopExtractor();
        if (qReader != null) {
            qReader.stopReader();
        }
        super.stopClient();
        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to start the connection and the reception of messages
     */
    @Override
    public void run() {
        if (connectAuthenticate(false)) {
            automaticReception();
        }
    }

    /**
     * Method to connect
     */

    public boolean connect(boolean reconnect) {
        mSocket = new Socket();
        try {
            InetAddress serverAddr = InetAddress.getByName(mServerIp);
            Log.d("NTRIP Client", "Connecting to NTRIP at " + mServerIp + ":" + mServerPort);
            mSocket.setSoTimeout(60000);
            mSocket.connect(new InetSocketAddress(serverAddr, mServerPort),5000);
            Log.d("NTRIP Client", "Connected");
            InputStream inputStream = mSocket.getInputStream();
            mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream())), true);
            mBufferIn = new BufferedInputStream(inputStream);
            return true;
        } catch (Exception e) {
            Log.e("NTRIP Client", "Error: Wrong IP/Port or connection problem: " + e);
            try {
                mSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            if (!reconnect) {
                mClientThread.mNtripClientListener.onClientStateChange("Error: Wrong IP/Port or connection problem: " + e, ClientThread.clientState.DISCONNECTED);
            }
            return false;
        }
    }

    /**
     * Method to authenticate
     */
    public boolean authenticate() {
        try {
            sendMessage(mClientMessage);
            byte [] answer = new byte [14];
            int bytesRead = mBufferIn.read(answer, 0, 14);
            if (bytesRead > 0) {
                String serverMessage = new String(answer);
                Log.d("RESPONSE FROM SERVER", "S:Received Message:  '" + serverMessage + "'");

                if (serverMessage.contains("ICY 200 OK")) {
                    return true;
                } else if (serverMessage.contains("SOURCETABLE")) {
                    mSocket.close();
                    mClientThread.mNtripClientListener.onClientStateChange("Wrong mountpoint", ClientThread.clientState.DISCONNECTED);
                    return false;
                } else if (serverMessage.contains("Unauthorized")) {
                    mSocket.close();
                    mClientThread.mNtripClientListener.onClientStateChange("Authorization error", ClientThread.clientState.DISCONNECTED);
                    return false;
                } else {
                    mSocket.close();
                    mClientThread.mNtripClientListener.onClientStateChange("Wrong user/password", ClientThread.clientState.DISCONNECTED);
                    return false;
                }
            }else{
                mSocket.close();
                mClientThread.mNtripClientListener.onClientStateChange("Byte read error", ClientThread.clientState.DISCONNECTED);
                return false;
            }
        }
        catch (Exception e) {
            try {
                mSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            if (mBufferOut != null) {
                mBufferOut.flush();
                mBufferOut.close();
            }
            mClientThread.mNtripClientListener.onClientStateChange("Authorization error", ClientThread.clientState.DISCONNECTED);
            return false;
        }
    }

    /**
     * Method to reconnect
     */
    public boolean reconnect(){
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (connectAuthenticate(true)) {
            bExtractor = new NtripBufferExtractor(mQueue, mBufferIn, this);
            bExtractor.start();
            sendMessage("START");
            Log.d("NTRIP Client", "Reconnection successful");
            mClientThread.mNtripClientListener.onClientStateChange("Reconnection successful", ClientThread.clientState.CONNECTED);
            resetReconnectionsNumber();
            return true;
        }
        else{
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return false;
        }
    }
    /**
     * Method that increases reconnection time
     */
    public int getReconnectTime(){
        iTimeToReconnect = (int) (10 * Math.exp(iReconnections));
        if (iTimeToReconnect >= iTimeToReconnectCap){
            return iTimeToReconnectCap;
        }
        else{
            return iTimeToReconnect;
        }
    }

    /**
     * This method connects and authenticates
     */
    public boolean connectAuthenticate(boolean reconnect) {
        if (connect(reconnect) && authenticate()) {
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * Method to start automatic SBAS messages reception
     */
    public void automaticReception() {
        try {
            // Prepare the receiver message queue
            mQueue = new LinkedBlockingQueue<>();
            bExtractor = new NtripBufferExtractor(mQueue, mBufferIn, this);
            qReader = new NtripQueueReader(mQueue, mClientThread);
            bExtractor.start();
            qReader.start();
            // Request data transmission start to the server
            sendMessage("START");
            mClientThread.mNtripClientListener.onClientStateChange("Connection established", ClientThread.clientState.CONNECTED);
        } catch (Exception e) {
            Log.e("Ntrip Client", "Error: " + e);
            stopClient();
            mClientThread.mNtripClientListener.onClientStateChange("Unknown error: " + e, ClientThread.clientState.DISCONNECTED);
        }
    }

    public void increaseReconnectionsNumber(){
        iReconnections += 1;
    }

    public void resetReconnectionsNumber(){
        iReconnections = 0;
    }
}

/**
 * Thread class to remove from the queue the bytes received from the caster
 */
class NtripQueueReader extends Thread {
    private final LinkedBlockingQueue<Byte> mQueue;
    private boolean keepRunning = false;
    private ClientThread mClientThread;

    /**
     * Constructor
     * @param queue Rx message queue
     * @param clientThread Reference to client thread instance
     */
    NtripQueueReader(LinkedBlockingQueue<Byte> queue, ClientThread clientThread) {
        mQueue = queue;
        mClientThread = clientThread;
    }

    /**
     * Method to start the connection and the reception of messages
     */
    @Override
    public void run (){
        Byte messageByte;
        RTCM2_MessageBuilder messageBuilder = new RTCM2_MessageBuilder(mClientThread);
        keepRunning = true;
        while (keepRunning){
            try {
                messageByte = mQueue.poll(5, TimeUnit.SECONDS);
                if (messageByte != null) {
                    messageBuilder.byteProcessing(messageByte);
                }
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }

    /**
     * Method to stop the the reading loop
     */
    void stopReader(){
        keepRunning = false;
    }
}

/**
 * Thread class to read the input buffer stream and add the bytes into a queue
 */
class NtripBufferExtractor extends Thread {
    private final LinkedBlockingQueue<Byte> mQueue;
    private NtripClient mNtripClient;
    private BufferedInputStream mBufferIn;
    private boolean keepRunning = false;

    /**
     * Constructor
     * @param queue Rx message queue
     * @param bufferIn Input stream buffer to read incoming messages from
     * @param ntripClient Reference to Ntrip client instance
     */
    NtripBufferExtractor(LinkedBlockingQueue<Byte> queue, BufferedInputStream bufferIn, NtripClient ntripClient) {
        mQueue = queue;
        mBufferIn = bufferIn;
        mNtripClient = ntripClient;
    }

    @Override
    public void run() {
        byte [] byteArray = new byte[1];
        super.run();
        keepRunning = true;

        try {
            while (keepRunning) {
                int read = mBufferIn.read(byteArray, 0, 1);
                if (read != -1)
                    mQueue.add(byteArray[0]);
            }
        } catch (IOException e) {
            if (keepRunning) {
                String reason = (e.getMessage() == null) ? "Unknown error" : e.getMessage();
                Log.e("Ntrip queue reader", reason);
                // Try to reconnect
                while ((keepRunning) && (!mNtripClient.reconnect())){
                    try {
                        for (int t = mNtripClient.getReconnectTime(); t > 0; t--) {
                            if (!keepRunning) { break; }
                            String sSleepTime = "Data gap. Trying to reconnect in " + String.valueOf(t) + " seconds";
                            mNtripClient.mClientThread.mNtripClientListener.onClientStateChange(sSleepTime, ClientThread.clientState.RECONNECTING);
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException ex) {
                        interrupt();
                    }
                    Log.e("SISNET Client", "Failed to reconnect");
                    mNtripClient.increaseReconnectionsNumber();
                }
                mNtripClient.resetReconnectionsNumber();
            }
        }
    }

    /**
     * Method to extracting info from the input buffer
     */
    void stopExtractor(){
        keepRunning = false;
    }
}
