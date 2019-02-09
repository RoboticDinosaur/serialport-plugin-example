package org.nativescript.b360124.serialport_plugin_example;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.nativescript.b360124.nsserialport.NSSerialport;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "NSSerialport";

    private NSSerialport nsSerialport;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case NSSerialport.ACTION_USB_CONNECT:
                    Log.i(LOG_TAG, "ACTION_USB_CONNECT");
                    break;
                case NSSerialport.ACTION_USB_DISCONNECTED:
                    Log.i(LOG_TAG, "ACTION_USB_DISCONNECTED");
                    break;
                case NSSerialport.ACTION_USB_NOT_SUPPORTED:
                    Log.i(LOG_TAG, "ACTION_USB_NOT_SUPPORTED");
                    break;
                case NSSerialport.ACTION_USB_NOT_OPENED:
                    Log.i(LOG_TAG, "ACTION_USB_NOT_OPENED");
                    break;

                case NSSerialport.ACTION_USB_PERMISSION_GRANTED:
                    Log.i(LOG_TAG, "ACTION_USB_PERMISSION_GRANTED");
                    break;
                case NSSerialport.ACTION_USB_PERMISSION_NOT_GRANTED:
                    Log.i(LOG_TAG, "ACTION_USB_PERMISSION_NOT_GRANTED");
                    break;

                case NSSerialport.ON_READ_DATA_FROM_PORT:
                    byte[] data = intent.getByteArrayExtra("data");
                    String str = new String(data);
                    Log.d("LOG_TAG", "ON_READ_DATA_FROM_PORT  ( " + str + " )");
                    break;
            }
        }
    };


    public void message() {
        Toast.makeText(MainActivity.this, "Ок", Toast.LENGTH_LONG).show();
    }

    private void connect() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(NSSerialport.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(NSSerialport.ACTION_USB_PERMISSION_NOT_GRANTED);
        filter.addAction(NSSerialport.ACTION_USB_CONNECT);
        filter.addAction(NSSerialport.ACTION_USB_DISCONNECTED);
        filter.addAction(NSSerialport.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(NSSerialport.ACTION_USB_NOT_OPENED);
        filter.addAction(NSSerialport.ON_READ_DATA_FROM_PORT);
        registerReceiver(mUsbReceiver, filter);

        nsSerialport = new NSSerialport(getApplicationContext());
        boolean isSetDevice = nsSerialport.setCurrentDevice(0x1A86, 0x7523);
        nsSerialport.setAutoConnect(true);
        nsSerialport.connect();
    }

    private void disconnect() {
        unregisterReceiver(mUsbReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connect();

        findViewById(R.id.btnWrite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nsSerialport.writeString("G28 X0\n");
                nsSerialport.writeString("G1X80F10000\n");
                nsSerialport.writeString("M400\n");
                nsSerialport.writeString("G28 Y0\n");
                nsSerialport.writeString("G1X80F10000\n");
                nsSerialport.writeString("M400\n");
            }
        });
    }
}