package com.example.phuon.smartswitch;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class ConfigWifiRouter extends AppCompatActivity {

    private EditText editSSID, editPass;
    private Button btSkip, btNext;
    private WebSocketClient client;
    String[] reciverData;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_wifi_router);

        mappedConfig();
        ReconnectWebSoketAP("ws://192.168.4.1:3232");

        Intent intent = getIntent();
        reciverData = intent.getStringArrayExtra("data");

        btNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ssid = editSSID.getText().toString();
                String pass = editPass.getText().toString();
                String putData = "dataS" + ssid + "&&" + pass + "dataE";
                sendData(putData);
                final ProgressDialog progressDialog = new ProgressDialog(ConfigWifiRouter.this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.show();
                progressDialog.setCanceledOnTouchOutside(false);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(10000);
                            progressDialog.dismiss();
                            ReconnectWebSoketAP("ws://192.168.4.1:3232");
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        btSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.close();
                MainActivity.database.InsertData(reciverData[2], reciverData[1], "AP", "");
                startActivity(new Intent(ConfigWifiRouter.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            }
        });

    }


    private void sendData(String mess){
        if(client.isOpen()){
            client.send(mess);
        }
    }

    private void ReconnectWebSoketAP(String host){
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
                        if(payload.indexOf("WiFi:1")>-1){
                            Toast.makeText(ConfigWifiRouter.this, "Connected", Toast.LENGTH_SHORT).show();
                            client.send("feelSTA");
                            client.close();
                            MainActivity.database.InsertData(reciverData[2], reciverData[1], "MQTT", payload.substring(payload.indexOf("host:")+5).toString());
                            startActivity(new Intent(ConfigWifiRouter.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        }
                        else if(payload.indexOf("WiFi:0")>-1){
                            Toast.makeText(ConfigWifiRouter.this, "Error", Toast.LENGTH_SHORT).show();
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

    private void mappedConfig(){
        editSSID = (EditText)findViewById(R.id.inputSSIDRouter);
        editPass = (EditText)findViewById(R.id.inputPassRouter);
        btSkip = (Button)findViewById(R.id.skip);
        btNext = (Button)findViewById(R.id.nextRouter);
    }
}
