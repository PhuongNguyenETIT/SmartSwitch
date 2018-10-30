package com.example.phuon.smartswitch;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ScheduleClass extends AppCompatActivity {

    EditText edtTimerOn, edtTimerOff;
    Switch swEn_DisSchedule;
    TextView txtTimeStart, txtDateStart, txtTimeEnd, txtDateEnd;
    Button btSaveSchedule;
    private boolean usedMQTT = false;
    private String[] getID_host;
    private WebSocketClient client;
    private MqttAndroidClient mqttAndroidClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_class);
        mappedShedule();
        ConnectWebSoketAP("ws://192.168.4.1:3232");

        Intent intent = getIntent();
        getID_host = intent.getStringArrayExtra("schedule");
        if(getID_host[1].equals("AP")){
            ConnectWebSoketAP("ws://192.168.4.1:3232");
        }
        else if(getID_host[1].equals("STA")) {
            ConnectWebSoketAP(getID_host[2].toString());
        }
        else if(getID_host[1].equals("MQTT")){
            usedMQTT = true;
            MQTTInit();
        }

        swEn_DisSchedule.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    sendCMD("schedule","status:on");
                    enableValue(true);
                }
                else {
                    sendCMD("schedule","status:off");
                    enableValue(false);
                }
            }
        });

        txtTimeStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimeStart();
            }
        });

        txtDateStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDateStart();
            }
        });

        txtTimeEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimeEnd();
            }
        });

        txtDateEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDateEnd();
            }
        });

        btSaveSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(txtTimeStart.getText().toString() != "" && txtDateStart.getText().toString() != ""
                        && txtTimeEnd.getText().toString() != "" && txtDateEnd.getText().toString() != ""
                        && edtTimerOn.getText().toString().trim() != "" && edtTimerOff.getText().toString().trim() != "") {
                    String schedule = "ts"+txtTimeStart.getText().toString() +"ds"+ txtDateStart.getText().toString()
                            +"to"+ edtTimerOn.getText().toString().trim() +"tf"+ edtTimerOff.getText().toString().trim()
                            +"te"+ txtTimeEnd.getText().toString() +"de"+ txtDateEnd.getText().toString();
                    sendCMD("schedule", "status:new,data:\"" + schedule+"\"");
                    Toast.makeText(ScheduleClass.this, "Saved", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(ScheduleClass.this, "Please enter all values",Toast.LENGTH_SHORT).show();
                }
            }
        });
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
                sendCMD("schedule", "status:getSche");
            }

            @Override
            public void onMessage(String message) {
                final String payload = message;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    if(payload.indexOf("SmartBox") > -1){
                        if(!getID_host[0].equals(payload.substring(payload.indexOf("SmartBox"), payload.indexOf(",state:")))){
                            Toast.makeText(ScheduleClass.this, "Not Connet", Toast.LENGTH_LONG).show();
                            client.close();
                        }
                    }
                    if(payload.indexOf(",sche:1")>-1){
                        swEn_DisSchedule.setChecked(true);
                        enableValue(true);
                    }
                    else if(payload.indexOf(",sche:255")>-1){
                        swEn_DisSchedule.setChecked(false);
                        enableValue(false);
                    }
                    else if(payload.indexOf("ts")>-1){
                        txtTimeStart.setText(payload.substring(payload.indexOf("ts")+2, payload.indexOf("ds")));
                        txtDateStart.setText(payload.substring(payload.indexOf("ds")+2, payload.indexOf("to")));
                        edtTimerOn.setText(payload.substring(payload.indexOf("to")+2, payload.indexOf("tf")));
                        edtTimerOff.setText(payload.substring(payload.indexOf("tf")+2, payload.indexOf("te")));
                        txtTimeEnd.setText(payload.substring(payload.indexOf("te")+2, payload.indexOf("de")));
                        txtDateEnd.setText(payload.substring(payload.indexOf("de")+2));
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

    private void enableValue(boolean check){
        if(check) {
            txtTimeStart.setEnabled(true);
            txtDateStart.setEnabled(true);
            txtTimeEnd.setEnabled(true);
            txtDateEnd.setEnabled(true);
            edtTimerOn.setEnabled(true);
            edtTimerOff.setEnabled(true);
            swEn_DisSchedule.setEnabled(true);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        else {
            txtTimeStart.setEnabled(false);
            txtDateStart.setEnabled(false);
            txtTimeEnd.setEnabled(false);
            txtDateEnd.setEnabled(false);
            edtTimerOn.setEnabled(false);
            edtTimerOff.setEnabled(false);
            swEn_DisSchedule.setEnabled(true);
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

    private void MQTTInit(){
        String clientId = MqttClient.generateClientId();
        mqttAndroidClient = new MqttAndroidClient(ScheduleClass.this, "tcp://168.63.251.166:1883", clientId);
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
                        if(payload.indexOf(",status:1")>-1){
                            swEn_DisSchedule.setChecked(true);
                            enableValue(true);
                        }
                        else if(payload.indexOf(",status:255")>-1){
                            swEn_DisSchedule.setChecked(false);
                            enableValue(false);
                        }
                        if(payload.indexOf("ts")>-1){
                            txtTimeStart.setText(payload.substring(payload.indexOf("ts")+2, payload.indexOf("ds")));
                            txtDateStart.setText(payload.substring(payload.indexOf("ds")+2, payload.indexOf("to")));
                            edtTimerOn.setText(payload.substring(payload.indexOf("to")+2, payload.indexOf("tf")));
                            edtTimerOff.setText(payload.substring(payload.indexOf("tf")+2, payload.indexOf("te")));
                            txtTimeEnd.setText(payload.substring(payload.indexOf("te")+2, payload.indexOf("de")));
                            txtDateEnd.setText(payload.substring(payload.indexOf("de")+2));
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
                    sendCMD("schedule", "status:getSche");
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

    private void setTimeStart(){
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                    calendar.set(0,0,0, hourOfDay, minute);
                    txtTimeStart.setText(simpleDateFormat.format(calendar.getTime()));
                }
        }, hour, minute, true);
        timePickerDialog.show();
    }

    private void setDateStart(){
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyy");
                        calendar.set(year, month, dayOfMonth);
                        txtDateStart.setText(simpleDateFormat.format(calendar.getTime()));
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    private void setTimeEnd(){
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                        calendar.set(0,0,0, hourOfDay, minute);
                        txtTimeEnd.setText(simpleDateFormat.format(calendar.getTime()));
                    }
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void setDateEnd(){
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyy");
                        calendar.set(year, month, dayOfMonth);
                        txtDateEnd.setText(simpleDateFormat.format(calendar.getTime()));
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    private void mappedShedule(){
        txtTimeStart = (TextView) findViewById(R.id.timeInput);
        txtDateStart = (TextView) findViewById(R.id.dateOn);
        txtTimeEnd = (TextView) findViewById(R.id.timeEnd);
        txtDateEnd = (TextView) findViewById(R.id.dateEnd);
        edtTimerOn = (EditText) findViewById(R.id.timerOn);
        edtTimerOff = (EditText) findViewById(R.id.timerOff);
        btSaveSchedule = (Button)findViewById(R.id.btSaveSchedule);
        swEn_DisSchedule = (Switch)findViewById(R.id.swSchedule);
    }
}
