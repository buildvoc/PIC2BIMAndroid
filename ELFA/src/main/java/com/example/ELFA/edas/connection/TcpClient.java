package com.example.ELFA.edas.connection;

import com.example.ELFA.edas.ClientThread;

import java.io.BufferedInputStream;
import java.io.PrintWriter;

public class TcpClient {
    boolean mRun = false;
    PrintWriter mBufferOut;
    BufferedInputStream mBufferIn;
    String mServerIp;
    int mServerPort = 0;
    String mClientMessage;
    ClientThread mClientThread;

    /**
     * Method -Constructor of the class
     * @param clientThread: The thread to assign to the TCPClient
     */
    TcpClient(ClientThread clientThread) {
        mClientThread = clientThread;
    }

    /**
     * Method to set the server IP
     * @param ip: String with the IP Address
     */
    void setServerIp(String ip) {
        mServerIp = ip;
    }

    /**
     * Method to set the connection port
     * @param port: Integer with the IP port
     */
    void setServerPort(int port) {
        mServerPort = port;
    }

    /**
     * Method to set the client message
     * @param clientMessage: String with the message
     */
    public void setClientMessage (String clientMessage) {
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
        mRun = false;
        mBufferIn = null;
        mBufferOut = null;
    }

    /**
     * Method to start the connection and the reception of messages
     */
    public void run() { }
}