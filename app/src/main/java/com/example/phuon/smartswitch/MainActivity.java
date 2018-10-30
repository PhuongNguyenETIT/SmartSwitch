package com.example.phuon.smartswitch;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
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
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView listDevi;
    ArrayList<InformationDevice> arrayList;
    ImageView imageAdd;
    DeviceAdapter adapter;
    public static DatabaseDevice database;

    private static final int VERSION = 1;
    private static final String NAME_DATABASE = "Data.db";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mappedMain();

        arrayList = new ArrayList<>();
        adapter = new DeviceAdapter(this, R.layout.devive_show, arrayList);
        listDevi.setAdapter(adapter);

        database = new DatabaseDevice(this, NAME_DATABASE,null, VERSION);
        //database.QueryDatabase("DROP TABLE IF EXISTS Devices");
        database.QueryDatabase("CREATE TABLE IF NOT EXISTS Devices(ID TEXT PRIMARY KEY , Name TEXT, Mode TEXT, Host TEXT)");


        GetDataDevice();

        imageAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SelectWifiNetwork.class));
            }
        });
    }

    private void GetDataDevice(){
        Cursor getDatabase = database.GetDatabase("SELECT * FROM Devices");
        while (getDatabase.moveToNext()){
            String modeConnection = getDatabase.getString(2);
            String idDevice = getDatabase.getString(0);
            String nameDevice = getDatabase.getString(1);
            String hostLANName = getDatabase.getString(3);

            if(modeConnection.indexOf("AP") > -1) {
                arrayList.add(new InformationDevice(idDevice, nameDevice, modeConnection, hostLANName, R.drawable.apconetion));
            }
            else if(modeConnection.indexOf("STA") > -1 || modeConnection.indexOf("MQTT") > -1){
                arrayList.add(new InformationDevice(idDevice, nameDevice, modeConnection, hostLANName, R.drawable.stateon));
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void mappedMain(){
        imageAdd = (ImageView)findViewById(R.id.addIMG);
        listDevi = (ListView)findViewById(R.id.listDevice);
    }

}
