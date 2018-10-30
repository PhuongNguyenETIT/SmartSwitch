package com.example.phuon.smartswitch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Layout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
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
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

import javax.xml.validation.TypeInfoProvider;

public class DeviceAdapter extends BaseAdapter{

    private Context context;
    private int layout;
    private List<InformationDevice> informationDevices;

    public DeviceAdapter(Context context, int layout, List<InformationDevice> informationDevices) {
        this.context = context;
        this.layout = layout;
        this.informationDevices = informationDevices;
    }

    @Override
    public int getCount() {
        return informationDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private class ViewHolder{
        TextView txtNameDevice, txtID;
        ImageView imgConnection, imgSchedule, imgManual, imgMenu;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final ViewHolder viewHolder;
        if(convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(layout, null);

            viewHolder.txtNameDevice = (TextView) convertView.findViewById(R.id.nameDV);
            viewHolder.txtID = (TextView) convertView.findViewById(R.id.textID);
            viewHolder.imgConnection = (ImageView) convertView.findViewById(R.id.imgState);
            viewHolder.imgSchedule = (ImageView)convertView.findViewById(R.id.setupSchedule);
            viewHolder.imgManual = (ImageView) convertView.findViewById(R.id.imgManual);
            viewHolder.imgMenu = (ImageView) convertView.findViewById(R.id.imgMenu);

            viewHolder.imgSchedule.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, ScheduleClass.class);
                    String[] data = {informationDevices.get(position).getClientID(),
                            informationDevices.get(position).getModeConnection(), informationDevices.get(position).getHostLAN()};
                    intent.putExtra("schedule", data);
                    v.getContext().startActivity(intent);
                }
            });

            viewHolder.imgManual.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intentSend = new Intent(context, Manual.class);
                    String[] data = {informationDevices.get(position).getClientID(),
                            informationDevices.get(position).getModeConnection(), informationDevices.get(position).getHostLAN()};
                    intentSend.putExtra("control", data);
                    v.getContext().startActivity(intentSend);
                }
            });

            final View finalConvertView = convertView;
            viewHolder.imgMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(context, finalConvertView, Gravity.RIGHT);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_listview, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.settingMenu:
                                    Toast.makeText(context, "OK", Toast.LENGTH_SHORT).show();
                                    return true;
                                case R.id.editMenu:
                                    DialogShowUpdate(informationDevices.get(position).getNameDevice(),
                                            informationDevices.get(position).getClientID(), position, viewHolder);
                                    return true;
                                case R.id.deleteMenu:
                                    DialogShowDelete(informationDevices.get(position).getClientID());
                                default:
                                return false;
                            }
                        }
                    });
                    popupMenu.show();
                }
            });

            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
            InformationDevice infor = informationDevices.get(position);

            viewHolder.txtNameDevice.setText(infor.getNameDevice());
            viewHolder.txtID.setText(infor.getClientID());
            viewHolder.imgConnection.setImageResource(infor.getImg());

        return convertView;
    }

    private void DialogShowUpdate(String newName, final String id, final int pos, final ViewHolder holder){
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_update_database);
        dialog.setTitle("Edit Name Device");
        dialog.setCanceledOnTouchOutside(false);
        Button btUpdate = (Button) dialog.findViewById(R.id.btUpdate);
        Button btCancel = (Button) dialog.findViewById(R.id.btCancelUpdate);
        final EditText edtNewName = (EditText) dialog.findViewById(R.id.edtNewName);

        btUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newName = edtNewName.getText().toString();
                MainActivity.database.QueryDatabase("UPDATE Devices SET Name = '"+ newName +"'WHERE ID = '"+ id +"'");
                dialog.dismiss();
                holder.txtNameDevice.setText(newName);
            }
        });

        btCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void DialogShowDelete(final String id){
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        builder.setMessage("Do you want to erase the device?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.database.QueryDatabase("DELETE FROM Devices WHERE ID = '"+id+"'");
                context.startActivity(new Intent(builder.getContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.show();
    }
}
