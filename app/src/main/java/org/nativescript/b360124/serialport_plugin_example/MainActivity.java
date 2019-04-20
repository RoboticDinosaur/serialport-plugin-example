package org.nativescript.b360124.serialport_plugin_example;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.nativescript.b360124.nsserialport.NSSerialport;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "NSSerialport";

    private NSSerialport nsSerialport;

    private static final String DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    private static final String DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    private static final String DEVICE_PERMISSION = "com.android.example.USB_PERMISSION";

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case DEVICE_ATTACHED:
                    Log.i(LOG_TAG, DEVICE_ATTACHED);
                    break;
                case DEVICE_DETACHED:
                    Log.i(LOG_TAG, DEVICE_DETACHED);
                    break;

                case NSSerialport.DEVICE_CONNECT:
                    Log.i(LOG_TAG, NSSerialport.DEVICE_CONNECT);
                    break;

                case NSSerialport.DEVICE_NOT_FOUND:
                    Log.i(LOG_TAG, NSSerialport.DEVICE_NOT_FOUND);
                    break;

                case NSSerialport.DEVICE_DISCONNECTED:
                    Log.i(LOG_TAG, NSSerialport.DEVICE_DISCONNECTED);
                    break;
                case NSSerialport.DEVICE_NOT_SUPPORTED:
                    Log.i(LOG_TAG, NSSerialport.DEVICE_NOT_SUPPORTED);
                    break;
                case NSSerialport.DEVICE_NOT_OPENED:
                    Log.i(LOG_TAG, NSSerialport.DEVICE_NOT_OPENED);
                    break;

                case NSSerialport.DEVICE_PERMISSION_GRANTED:
                    Log.i(LOG_TAG, NSSerialport.DEVICE_PERMISSION_GRANTED);
                    break;

                case NSSerialport.DEVICE_PERMISSION_NOT_GRANTED:
                    Log.i(LOG_TAG, NSSerialport.DEVICE_PERMISSION_NOT_GRANTED);
                    break;

                case NSSerialport.DEVICE_READ_DATA:
                    byte[] data = intent.getByteArrayExtra("data");
                    String str = new String(data);
                    Log.d("LOG_TAG",  NSSerialport.DEVICE_READ_DATA + " ( " + str + " )");
                    break;
            }
        }
    };

    public void message() {
        Toast.makeText(MainActivity.this, "Ок", Toast.LENGTH_LONG).show();
    }

    private void connect() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(DEVICE_ATTACHED);
        filter.addAction(DEVICE_DETACHED);

        filter.addAction(NSSerialport.DEVICE_NOT_FOUND);
        filter.addAction(NSSerialport.DEVICE_PERMISSION_GRANTED);
        filter.addAction(NSSerialport.DEVICE_CONNECT);
        filter.addAction(NSSerialport.DEVICE_PERMISSION_NOT_GRANTED);
        filter.addAction(NSSerialport.DEVICE_DISCONNECTED);
        filter.addAction(NSSerialport.DEVICE_NOT_SUPPORTED);
        filter.addAction(NSSerialport.DEVICE_NOT_OPENED);
        filter.addAction(NSSerialport.DEVICE_READ_DATA);
        registerReceiver(mUsbReceiver, filter);

        nsSerialport = new NSSerialport(getApplicationContext());
        nsSerialport.setVendorId(0x1A86);
        nsSerialport.setProductId(0x7523);
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