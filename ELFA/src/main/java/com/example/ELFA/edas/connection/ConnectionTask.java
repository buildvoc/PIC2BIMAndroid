package com.example.ELFA.edas.connection;

import android.util.Base64;
import android.util.Log;

/**
 * Class to run the TcpClient for sending a connection request to the server.
 */
public class ConnectionTask extends Thread{
    private String mIp;
    private int mPort;
    private String mUsername;
    private String mPassword;
    private String mMountpoint;
    private String mMessage;
    private TcpClient mTcpClient;

    /**
     * Method - class constructor
     *@param ip: String with the ip address of the server
     *@param port: int with the connection port
     *@param username: String with username
     *@param password: String with the password
     *@param service: String with the service
     *@param mountpoint: String with the mountpoint
     *@param tcpClient: TcpClient that will handle the connection
     */
    public ConnectionTask(String ip, int port, String username, String password, String service, String mountpoint, TcpClient tcpClient) {
        super();
        mIp = ip;
        mPort = port;
        mUsername = username;
        mPassword = password;
        mMountpoint = mountpoint;
        mTcpClient = tcpClient;

        if (service.equals("NTRIP")) {
            mMessage = composeNtripConnectionMessage();
        } else if (service.equals("SISNET")) {
            mMessage = composeSisnetConnectionMessage();
        }
        else {
            Log.e("ConnectionTask", "Unknown service");
        }
    }

    @Override
    public void run() {
        mTcpClient.setServerIp(mIp);
        mTcpClient.setServerPort(mPort);
        mTcpClient.setClientMessage(mMessage);
        mTcpClient.run();
    }

    /**
     * Method to compose the message to establish the connection with the Ntrip caster
     *@return String result: The complete message
     */
    private String composeNtripConnectionMessage() {
        String textCredentials = mUsername + ":" + mPassword;
        byte [] dataCredentials = textCredentials.getBytes();
        String encodedCredentials = Base64.encodeToString(dataCredentials, Base64.DEFAULT);
        return "GET /" + mMountpoint + " HTTP/1.1" + "\r\n"+
                "User-Agent: NTRIP EDAS/1.0" + "\r\n"+
                "Ntrip-Version: Ntrip/1.0" + "\r\n" +
                "Authorization: Basic " + encodedCredentials + "\r\n"+
                "Connection: close"+ "\r\n\r\n";
    }

    /**
     * Method to compose the message to establish the connection with the Sisnet server
     *@return String result: The complete message
     */
    private String composeSisnetConnectionMessage() {
        return String.format("AUTH,%s,%s", mUsername, mPassword);
    }

}
