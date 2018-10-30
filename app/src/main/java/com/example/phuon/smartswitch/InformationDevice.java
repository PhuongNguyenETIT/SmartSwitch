package com.example.phuon.smartswitch;

import android.widget.ImageView;

import java.net.URI;

public class InformationDevice {
    private String NameDevice;
    private String ModeConnection;
    private String HostLAN;
    private String ClientID;
    private int img;

    public InformationDevice( String clientID, String nameDevice, String modeConnection, String hostLAN, int img) {
        ClientID = clientID;
        NameDevice = nameDevice;
        ModeConnection = modeConnection;
        HostLAN = hostLAN;
        this.img = img;
    }

    public String getNameDevice() {
        return NameDevice;
    }

    public void setNameDevice(String nameDevice) {
        NameDevice = nameDevice;
    }

    public String getModeConnection() {
        return ModeConnection;
    }

    public void setModeConnection(String modeConnection) {
        ModeConnection = modeConnection;
    }

    public String getHostLAN() {
        return HostLAN;
    }

    public void setHostLAN(String hostLAN) {
        HostLAN = hostLAN;
    }

    public String getClientID() {
        return ClientID;
    }

    public void setClientID(String clientID) {
        ClientID = clientID;
    }

    public int getImg() {
        return img;
    }

    public void setImg(int img) {
        this.img = img;
    }
}
