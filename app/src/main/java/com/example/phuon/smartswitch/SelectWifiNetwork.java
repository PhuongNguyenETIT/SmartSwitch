package com.example.phuon.smartswitch;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class SelectWifiNetwork extends AppCompatActivity {

    Button btGotoWifi, btNext;
    ProgressDialog progressDialog;
    WebSocketClient client;
    CountDownTimer countDownTimer;
    String ssidConneted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_wifi_network);
        btGotoWifi = (Button)findViewById(R.id.btGotoWifi);
        btNext = (Button)findViewById(R.id.btNextWifi);


        btGotoWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });

        btNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                if(networkInfo != null && networkInfo.isConnected()) {
                    ssidConneted = networkInfo.getExtraInfo();
                    if(ssidConneted.indexOf("SmartBox") > -1) {
                        progressCheck();
                    }
                    else {
                        Toast.makeText(SelectWifiNetwork.this, "You have not Wifi connected to the device", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                }
                else {
                    Toast.makeText(SelectWifiNetwork.this, "You have not enabled or not connected to Wifi", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void progressCheck(){
        progressDialog = new ProgressDialog(SelectWifiNetwork.this);
        progressDialog.setTitle("Check Connect");
        progressDialog.setMessage("Checking");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        progressDialog.setMax(10);
//        progressDialog.setProgress(0);
        progressDialog.show();
        progressDialog.setCanceledOnTouchOutside(false);

        countDownTimer = new CountDownTimer(6000, 2000) {
            @Override
            public void onTick(long millisUntilFinished) {
                ConnectWebSoketAP("ws://192.168.4.1:3232");
            }

            @Override
            public void onFinish() {
                progressDialog.dismiss();
                Toast.makeText(SelectWifiNetwork.this, "Error Connect", Toast.LENGTH_SHORT).show();
            }
        };
        countDownTimer.start();
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
            public void onMessage(final String message) {
                final String payload = message;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(message.indexOf("SmartBox") > -1) {
                            Boolean exists = false;
                            String nameID = message.substring(message.indexOf("SmartBox"), message.indexOf(",state"));
                            Cursor getIDDatabase = MainActivity.database.GetDatabase("SELECT ID FROM Devices");
                            while (getIDDatabase.moveToNext()){
                                if(nameID.equals(getIDDatabase.getString(0))){
                                    countDownTimer.cancel();
                                    progressDialog.dismiss();
                                    exists = true;
                                    break;
                                }
                            }
                            if(exists) {
                                Toast.makeText(SelectWifiNetwork.this, "SmartBox already exists", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                countDownTimer.cancel();
                                progressDialog.cancel();
                                client.close();
                                Toast.makeText(SelectWifiNetwork.this, "Connected", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SelectWifiNetwork.this, ControlWebSocketLAN.class);
                                intent.putExtra("ssid", ssidConneted);
                                startActivity(intent);
                            }
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
}
