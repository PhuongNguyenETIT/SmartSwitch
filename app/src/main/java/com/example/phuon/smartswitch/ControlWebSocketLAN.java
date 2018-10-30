package com.example.phuon.smartswitch;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class ControlWebSocketLAN extends AppCompatActivity {

    private EditText textNameDV;
    private Button btBack, btNext;
    private WebSocketClient client;
    private String nameWifiConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_web_socket_lan);

        mapped();


        btBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(ControlWebSocketLAN.this, MainActivity.class);
                startActivity(new Intent(ControlWebSocketLAN.this, MainActivity.class));
            }
        });

        btNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectWebSoketAP("ws://192.168.4.1:3232");
            }
        });

        Intent intent = getIntent();
        nameWifiConnected = intent.getStringExtra("ssid");

    }

    private void ConnectWebSoketAP(String host){
        URI uri;
        try {
            uri = new URI(host);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        client = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {

            }

            @Override
            public void onMessage(String message) {
                final String payload = message;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(payload.indexOf("SmartBox") > -1){
                            String idDV = payload.substring(payload.indexOf("SmartBox"), payload.indexOf(",state:"));
                            client.close();
                            Intent intent = new Intent(ControlWebSocketLAN.this, ConfigWifiRouter.class);
                            String[] data = {nameWifiConnected, textNameDV.getText().toString(), idDV};
                            intent.putExtra("data", data);
                            startActivity(intent);
                        }
                    }
                });
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {

            }

            @Override
            public void onError(Exception ex) {

            }
        };
        client.connect();
    }

    private void mapped(){
        textNameDV = (EditText)findViewById(R.id.setNameDV);
        btBack = (Button)findViewById(R.id.back);
        btNext = (Button)findViewById(R.id.next);
    }
}
