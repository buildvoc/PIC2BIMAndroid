package com.example.ELFA.edas.connection;

import android.util.Log;

import com.example.ELFA.edas.ClientThread;
import com.example.ELFA.edas.sisnet.Ds2dcMessage;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class provides methods for connecting, sending and receiving stream data over a network in asynchronous mode
 */
public class SisnetClient extends TcpClient {
    private PrintWriter mBufferOut;
    private BufferedReader bReader;
    private LinkedBlockingQueue<String> mQueue;
    private SisnetBufferExtractor bExtractor;
    private SisnetQueueReader qReader;
    private int iTimeToReconnect = 0;
    private int iTimeToReconnectCap = 15*60;
    private int iReconnections = 0;
    private Socket mSocket;

    /**
     * Constructor
     * @param clientThread Reference of client thread instance
     */
    public SisnetClient(ClientThread clientThread){
        super(clientThread);
    }

    /**
     * Method to set the message to be sent to the server for authentication and connection
     * @param clientMessage: String to be sent for connection and authentication
     */
    public void setClientMessage(String clientMessage) {
        mClientMessage = clientMessage;
    }

    /**
     * Method to send a message to a destination
     * @param message: Message string
     */
    public void sendMessage(final String message) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mBufferOut != null) {
                    mBufferOut.println(message);
                    mBufferOut.flush();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Method to close the connection and release the members
     */
    public void stopClient() {
        Log.d("Sisnet Client", "Code at stopClient");
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
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * Method to start the connection and the reception of messages
     */
    public void run() {
        if (connectAuthenticate(false)) {
            automaticReception();
        }
    }

    /**
     * Method to connect
     */

    public boolean connect(boolean reconnect){
        mSocket = new Socket();
        try {
            InetSocketAddress socketAddress = new InetSocketAddress(mServerIp, mServerPort);
            Log.d("Sisnet Client", "Connecting to SiSNET at " + mServerIp + ":" + mServerPort);
            mSocket.setSoTimeout(60000);
            mSocket.connect(socketAddress,5000);
            Log.d("SISNET Client", "Connected");
            InputStream inputStream = mSocket.getInputStream();
            BufferedInputStream bufferIn = new BufferedInputStream(inputStream);
            mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream())), true);
            bReader = new BufferedReader( new InputStreamReader(bufferIn));
            return true;
        } catch (Exception e) {
            Log.e("SISNET Client", "Error: Wrong IP/Port or connection problem: " + e);
            try {
                mSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            if (!reconnect) {
                mClientThread.mSisnetClientListener.onClientStateChange("Error: Wrong IP/Port or connection problem: " + e, ClientThread.clientState.DISCONNECTED);
            }
            return false;
        }
    }

    /**
     * Method to authenticate
     */
    public boolean authenticate(){
        try {
            sendMessage(mClientMessage);
            String serverMessage = bReader.readLine();
            if (serverMessage.contains("AUTH")) {
                return true;
            }
            else {
                mSocket.close();
                mClientThread.mSisnetClientListener.onClientStateChange("Authorization error", ClientThread.clientState.DISCONNECTED);
                return false;
            }
        }catch (Exception e){
            try {
                mSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            if (mBufferOut != null) {
                mBufferOut.flush();
                mBufferOut.close();
            }
            mClientThread.mSisnetClientListener.onClientStateChange("Authorization error", ClientThread.clientState.DISCONNECTED);
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
            bExtractor = new SisnetBufferExtractor(mQueue, bReader, this);
            bExtractor.start();
            sendMessage("START");
            Log.d("SISNET Client", "Reconnection successful");
            mClientThread.mSisnetClientListener.onClientStateChange("Reconnection successful", ClientThread.clientState.CONNECTED);
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
    public boolean connectAuthenticate(boolean reconnect){
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
    public void automaticReception(){
        try {
            // Prepare the receiver message queue
            mQueue = new LinkedBlockingQueue<>();
            bExtractor = new SisnetBufferExtractor(mQueue, bReader, this);
            qReader = new SisnetQueueReader(mQueue, mClientThread);
            bExtractor.start();
            qReader.start();
            // Request data transmission start to the server
            sendMessage("START");
            mClientThread.mSisnetClientListener.onClientStateChange("Connection established", ClientThread.clientState.CONNECTED);
        } catch (Exception e) {
            Log.e("SISNET Client", "Error: " + e);
            stopClient();
            mClientThread.mSisnetClientListener.onClientStateChange("Unknown error: " + e, ClientThread.clientState.DISCONNECTED);
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
class SisnetQueueReader extends Thread {
    private final LinkedBlockingQueue<String> mQueue;
    private ClientThread mClientThread;
    private boolean keepRunning;

    /**
     * Constructor
     * @param queue Rx message queue
     * @param clientThread Reference to client thread instance
     */
    SisnetQueueReader(LinkedBlockingQueue<String> queue, ClientThread clientThread){
        mQueue = queue;
        mClientThread = clientThread;
    }

    /**
     * Method to start the connection and the reception of messages
     */
    @Override
    public void run() {
        String line;
        keepRunning = true;
        while (keepRunning) {
            try {

                line = mQueue.poll(60, TimeUnit.SECONDS);

                if (line != null) {
                    // New message extracted from the queue
                    if (line.contains("START")) {
                        Log.d("Ds2dcMessage", "New server message: " + line);
                        continue;
                    } else if (line.contains("STOP")) {
                        Log.d("Ds2dcMessage", "New server message: " + line);
                        stopReader();
                        continue;
                    }
                    Ds2dcMessage message = new Ds2dcMessage(line);
                    // Send a valid msg to ClientThread for processing
                    mClientThread.sendMessage(message.getEgnosMessage());
                }
            }catch (InterruptedException e){
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
class SisnetBufferExtractor extends Thread {
    private final LinkedBlockingQueue<String> mQueue;
    private SisnetClient mSisnetClient;
    private BufferedReader mReader;
    private boolean keepRunning;

    /**
     * Constructor
     * @param queue Rx message queue
     * @param reader Input buffer to read incoming messages from
     * @param sisnetClient Reference to Sisnet client instance
     */
    SisnetBufferExtractor(LinkedBlockingQueue<String> queue, BufferedReader reader, SisnetClient sisnetClient) {
        mQueue = queue;
        mReader = reader;
        mSisnetClient = sisnetClient;
    }

    @Override
    public void run() {
        super.run();
        keepRunning = true;
        try {
            while (keepRunning) {
                String line = mReader.readLine();
                if (!line.equals("")) {
                    mQueue.add(line);
                }
            }
        //Catches SocketTimeoutException and InterruptedIOException
        } catch (IOException e) {
            if (keepRunning) {
                String reason = (e.getMessage() == null) ? "Unknown error" : e.getMessage();
                Log.e("Sisnet queue reader", reason);
                // Try to reconnect
                while ((keepRunning) && (!mSisnetClient.reconnect())){
                    try {
                        for (int t = mSisnetClient.getReconnectTime(); t > 0; t--) {
                            if (!keepRunning) { break; }
                            String sSleepTime = "Data gap. Trying to reconnect in " + String.valueOf(t) + " seconds";
                            mSisnetClient.mClientThread.mSisnetClientListener.onClientStateChange(sSleepTime, ClientThread.clientState.RECONNECTING);
                            Thread.sleep(1000);
                        }
//                        int t = mSisnetClient.getReconnectTime();
//                        String sSleepTime = "Data gap. Trying to reconnect each " + String.valueOf(t) + " seconds";
//                        mSisnetClient.mClientThread.mSisnetClientListener.onClientStateChange(sSleepTime, TcpClient.clientState.RECONNECTING);
//                        Thread.sleep(t * 1000);
                    } catch (InterruptedException ex) {
                        interrupt();
                    }
                    Log.e("SISNET Client", "Failed to reconnect");
                    mSisnetClient.increaseReconnectionsNumber();
                }
                mSisnetClient.resetReconnectionsNumber();

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
