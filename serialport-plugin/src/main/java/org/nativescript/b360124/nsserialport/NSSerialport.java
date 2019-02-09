package org.nativescript.b360124.nsserialport;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.SystemClock;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NSSerialport {
    private static final String LOG_TAG = "NSSerialport";

    private static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    private static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    public static final String ACTION_USB_PERMISSION_GRANTED = "USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_NOT_SUPPORTED = "USB_NOT_SUPPORTED";
    public static final String ACTION_USB_DISCONNECTED = "USB_DISCONNECTED";
    public static final String ACTION_USB_CONNECT = "USB_CONNECT";
    public static final String ACTION_USB_NOT_OPENED = "USB_NOT_OPENED";
    public static final String ON_READ_DATA_FROM_PORT = "ON_READ_DATA_FROM_PORT";

    //Connection Settings
    private int VENDOR_ID = 0;
    private int PRODUCT_ID = 0;
    private int BAUD_RATE    = 115200;
    private int DATA_BIT     = UsbSerialInterface.DATA_BITS_8;
    private int STOP_BIT     = UsbSerialInterface.STOP_BITS_1;
    private int PARITY       =  UsbSerialInterface.PARITY_NONE;
    private int FLOW_CONTROL = UsbSerialInterface.FLOW_CONTROL_OFF;
    private boolean autoConnect = false;

    private Context currentContext;
    private UsbManager usbManager;

    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private UsbDevice currentDevice;
    private List<UsbDevice> devicesList  = new ArrayList<>();

    private boolean serialPortConnected = false;
    private boolean isConnect = false;

    public NSSerialport(Context currentContext) {
        this.currentContext = currentContext;
        setFilters();

        usbManager = (UsbManager) currentContext.getSystemService(Context.USB_SERVICE);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_USB_ATTACHED:
                    if(autoConnect && VENDOR_ID != 0 && PRODUCT_ID != 0) {
                        setCurrentDevice(VENDOR_ID, PRODUCT_ID);
                        requestUserPermission();
                    }
                    break;
                case ACTION_USB_DETACHED:
                    stopConnection();
                    break;
                case ACTION_USB_PERMISSION :
                    boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    startConnection(granted);
                    break;
            }
        }
    };

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_ATTACHED);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        currentContext.registerReceiver(mUsbReceiver, filter);
    }


    public void setBaudRate(int BAUD_RATE) {
        this.BAUD_RATE = BAUD_RATE;
    }

    public void setDataBit(int DATA_BIT) {
        this.DATA_BIT = DATA_BIT;
    }

    public void setStopBit(int STOP_BIT) {
        this.STOP_BIT = STOP_BIT;
    }

    public void setParity(int PARITY) {
        this.PARITY = PARITY;
    }

    public void setFlowControl(int FLOW_CONTROL) {
        this.FLOW_CONTROL = FLOW_CONTROL;
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    public void writeString(String data) {
        serialPort.write(data.getBytes());
    }

    public void connect() {
        if(isConnect) {
            return;
        }

        isConnect = true;
        requestUserPermission();
    }

    public void disconnect() {
        currentContext.unregisterReceiver(mUsbReceiver);

        if(!isConnect) return;
        stopConnection();
        isConnect = false;
    }

    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(currentContext, 0 , new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(currentDevice, mPendingIntent);
    }

    public List<UsbDevice> getDeviceList() {
        devicesList.clear();

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();

        if (!usbDevices.isEmpty()) {
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                devicesList.add(entry.getValue());
            }
        }
        return devicesList;
    }

    public boolean setCurrentDevice(int vendorId, int productId) {
        getDeviceList();
        currentDevice = null;
        for( int i = 0; i < devicesList.size(); ++i) {
            UsbDevice device = devicesList.get(i);
            if (device.getVendorId() == vendorId && device.getProductId() == productId ) {
                currentDevice = device;
                VENDOR_ID = vendorId;
                PRODUCT_ID = productId;
                return true;
            }
        }

        return false;
    }

    private void startConnection(boolean granted) {
        if(granted) {
            currentContext.sendBroadcast(new Intent(ACTION_USB_PERMISSION_GRANTED));
            connection = usbManager.openDevice(currentDevice);
            new ConnectionThread().start();
        } else {
            connection = null;
            currentDevice = null;
            currentContext.sendBroadcast(new Intent(ACTION_USB_PERMISSION_NOT_GRANTED));
        }
    }

    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            SystemClock.sleep(2000);
            serialPort = UsbSerialDevice.createUsbSerialDevice(currentDevice, connection);
            if(serialPort == null) {
                currentContext.sendBroadcast(new Intent(ACTION_USB_NOT_SUPPORTED));
                return;
            }

            if(!serialPort.open()){
                currentContext.sendBroadcast(new Intent(ACTION_USB_NOT_OPENED));
                return;
            }

            serialPortConnected = true;
            serialPort.setBaudRate(BAUD_RATE);
            serialPort.setDataBits(DATA_BIT);
            serialPort.setStopBits(STOP_BIT);
            serialPort.setParity(PARITY);
            serialPort.setFlowControl(FLOW_CONTROL);
            serialPort.read(mCallback);

            currentContext.sendBroadcast(new Intent(ACTION_USB_CONNECT));
        }
    }

    private void stopConnection() {
        if (!serialPortConnected) {
            return;
        }
        serialPort.close();
        connection = null;
        currentDevice = null;
        serialPortConnected = false;
        currentContext.sendBroadcast(new Intent(ACTION_USB_DISCONNECTED));
    }

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] bytes) {
            Intent intent = new Intent(ON_READ_DATA_FROM_PORT);
            intent.putExtra("data", bytes);
            currentContext.sendBroadcast(intent);
        }
    };
}
