package com.example.wifishare;

import java.net.Socket;

public class InterfacesClass {
    public static interface getClientSocket{
        public void onClientSocketRecvd(Socket s);
    }
}
