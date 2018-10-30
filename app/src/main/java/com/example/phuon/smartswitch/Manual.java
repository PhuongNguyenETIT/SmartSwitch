package com.example.phuon.smartswitch;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class Manual extends AppCompatActivity {

    ImageButton imgOnOff;
    private String[] getID_host;
    boolean OnOff = false;
    private WebSocketClient client;
    private boolean offSchedule = false;
    private boolean usedMQTT = false;
    MqttAndroidClient mqttAndroidClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);
        imgOnOff = (ImageButton)findViewById(R.id.onoff);


        Intent intent = getIntent();
        getID_host = intent.getStringArrayExtra("control");
        if(getID_host[1].equals("AP")){
            ConnectControl("ws://192.168.4.1:3232");
        }
        else if(getID_host[1].equals("STA")) {
            ConnectControl(getID_host[2].toString());
        }
        else if(getID_host[1].equals("MQTT")){
            usedMQTT = true;
            MQTTInit();
        }


        imgOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!offSchedule) {
                    sendCMD("schedule","status:off");
                    offSchedule = true;
                }
                if(OnOff){
                    sendCMD("control","state:off");
                }
                else {
                    sendCMD("control","state:on");
                }

                imgOnOff.setClickable(false);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!usedMQTT){
            client.close();
        }
        else {
            try {
                mqttAndroidClient.unsubscribe("feelback/phuong");
                mqttAndroidClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendCMD(String cmd, String mess){
        String payload = "{id:" + getID_host[0] + ",cmd:" + cmd + "," + mess + "}";
        if(usedMQTT){
            MQTTPublish(payload);
        }
        else {
            if (client.isOpen()) {
                client.send(mess);
            }
        }
    }

    private void ConnectControl(String host){
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
                        if(!getID_host[0].equals(payload.substring(payload.indexOf("SmartBox"), payload.indexOf(",state:")))){
                            Toast.makeText(Manual.this, "Not Connet", Toast.LENGTH_LONG).show();
                            client.close();
                        }
                    }
                    if(payload.indexOf("state:0") > -1){
                        OnOff = true;
                        imgOnOff.setImageResource(R.drawable.on);
                        imgOnOff.setClickable(true);
                    }
                    else if(payload.indexOf("state:1") > -1){
                        OnOff = false;
                        imgOnOff.setImageResource(R.drawable.off);
                        imgOnOff.setClickable(true);
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

        private void MQTTInit(){
        String clientId = MqttClient.generateClientId();
        mqttAndroidClient = new MqttAndroidClient(Manual.this, "tcp://168.63.251.166:1883", clientId);
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final String payload = new String(message.getPayload());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    if(payload.indexOf("state:0") > -1){
                        OnOff = true;
                        imgOnOff.setImageResource(R.drawable.on);
                        imgOnOff.setClickable(true);
                    }
                    else if(payload.indexOf("state:1") > -1){
                        OnOff = false;
                        imgOnOff.setImageResource(R.drawable.off);
                        imgOnOff.setClickable(true);
                    }
                    }
                });
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
        try {
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            connectOptions.setUserName("iotech");
            connectOptions.setPassword("iotech.vn".toCharArray());
            IMqttToken token = mqttAndroidClient.connect(connectOptions);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    MQTTSubscribe(0);
                    sendCMD("control","state:sync");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void MQTTPublish(String message){
        try {
            mqttAndroidClient.publish("command/phuong", message.getBytes(), 0, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void MQTTSubscribe(int qos){
        try {
            mqttAndroidClient.subscribe("feelback/phuong", qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
