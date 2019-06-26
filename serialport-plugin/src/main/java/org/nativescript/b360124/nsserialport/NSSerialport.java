package org.nativescript.b360124.nsserialport;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NSSerialport {
    private static final String LOG_TAG = "NSSerialport";

    private static final String DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    private static final String DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    private static final String DEVICE_PERMISSION = "com.android.example.USB_PERMISSION";

    public static final String DEVICE_PERMISSION_GRANTED = "DEVICE_PERMISSION_GRANTED";
    public static final String DEVICE_PERMISSION_NOT_GRANTED = "DEVICE_PERMISSION_NOT_GRANTED";
    public static final String DEVICE_NOT_FOUND = "DEVICE_NOT_FOUND";
    public static final String DEVICE_NOT_SUPPORTED = "DEVICE_NOT_SUPPORTED";
    public static final String DEVICE_DISCONNECTED = "DEVICE_DISCONNECTED";
    public static final String DEVICE_CONNECT = "DEVICE_CONNECT";
    public static final String DEVICE_NOT_OPENED = "DEVICE_NOT_OPENED";
    public static final String DEVICE_READ_DATA = "DEVICE_READ_DATA";

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
    private UsbDevice device;
    private List<UsbDevice> devicesList  = new ArrayList<>();

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
                case DEVICE_ATTACHED:
                    if(autoConnect) {
                        connect();
                    }
                    break;
                case DEVICE_DETACHED:
                    stopConnection();
                    break;
                case DEVICE_PERMISSION:
                    boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    startConnection(granted);
                    break;
            }
        }
    };

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DEVICE_ATTACHED);
        filter.addAction(DEVICE_DETACHED);
        filter.addAction(DEVICE_PERMISSION);
        currentContext.registerReceiver(mUsbReceiver, filter);
    }


    public void setVendorId(int VENDOR_ID) {
        this.VENDOR_ID = VENDOR_ID;
    }

    public void setProductId(int PRODUCT_ID) {
        this.PRODUCT_ID = PRODUCT_ID;
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

    public void writeString(String data) { new WriteThread(data).start(); }

    public void connect() {
        if(!isConnect) {
            getDevice();
        }
    }

    public void disconnect() {
        currentContext.unregisterReceiver(mUsbReceiver);
        stopConnection();
    }


    public void getDevice() {
        device = null;

        if (VENDOR_ID != 0 && PRODUCT_ID != 0) {
            HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();

            if (!usbDevices.isEmpty()) {
                for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                    UsbDevice deviceTmp = entry.getValue();
                    if (deviceTmp.getVendorId() == VENDOR_ID && deviceTmp.getProductId() == PRODUCT_ID) {
                        device = deviceTmp;
                        requestUserPermission();
                        return;
                    }
                }
            }
        }
        currentContext.sendBroadcast(new Intent(DEVICE_NOT_FOUND));
    }

    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(currentContext, 0 , new Intent(DEVICE_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
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

    private void startConnection(boolean granted) {
        if(granted) {
            currentContext.sendBroadcast(new Intent(DEVICE_PERMISSION_GRANTED));
            connection = usbManager.openDevice(device);
            new ConnectionThread().start();
        } else {
            connection = null;
            device = null;
            currentContext.sendBroadcast(new Intent(DEVICE_PERMISSION_NOT_GRANTED));
        }
    }

    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if(serialPort == null) {
                currentContext.sendBroadcast(new Intent(DEVICE_NOT_SUPPORTED));
                return;
            }

            if(!serialPort.open()){
                currentContext.sendBroadcast(new Intent(DEVICE_NOT_OPENED));
                return;
            }

            serialPort.setBaudRate(BAUD_RATE);
            serialPort.setDataBits(DATA_BIT);
            serialPort.setStopBits(STOP_BIT);
            serialPort.setParity(PARITY);
            serialPort.setFlowControl(FLOW_CONTROL);
            serialPort.read(mCallback);

            isConnect = true;
            currentContext.sendBroadcast(new Intent(DEVICE_CONNECT));
        }
    }


    private class WriteThread extends Thread {
        private String data;

        public WriteThread (String data) { this.data = data; }

        @Override
        public void run() {
            serialPort.write(data.getBytes());
        }
    }

    private void stopConnection() {
        if (!isConnect) {
            return;
        }
        serialPort.close();
        connection = null;
        device = null;
        isConnect = false;
        currentContext.sendBroadcast(new Intent(DEVICE_DISCONNECTED));
    }

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] bytes) {
            Intent intent = new Intent(DEVICE_READ_DATA);
            intent.putExtra("data", bytes);

            currentContext.sendBroadcast(intent);
        }
    };
}