package smartbox.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Yuan Zhang on 3/7/2019.
 */

public class BluetoothSingleton {

    private static BluetoothDevice mDevice;
    private static BluetoothSocket mSocket;
    private static BluetoothAdapter mAdapter;


    public BluetoothSingleton() {
    }

    public BluetoothDevice getDevice() {
        return BluetoothSingleton.mDevice;
    }

    public BluetoothSocket getSocket() {
        return BluetoothSingleton.mSocket;
    }

    public BluetoothAdapter getAdapter() {
        return BluetoothSingleton.mAdapter;
    }


    public static void setmDevice(BluetoothDevice mDevice) {
        BluetoothSingleton.mDevice = mDevice;
    }

    public static void setmSocket(BluetoothSocket mSocket) {
        BluetoothSingleton.mSocket = mSocket;
    }

    public static void setmAdapter(BluetoothAdapter mAdapter) {
        BluetoothSingleton.mAdapter = mAdapter;
    }



}
