package smartbox.bluetoothdemo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

public class WifiInput extends Activity {

    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;

    final byte delimiter = 33;
    int readBufferPosition = 0;

    private BluetoothAdapter mBluetoothAdapter;

    private EditText mPasswordView;
    private AutoCompleteTextView mWifiName;
    private Button mConfirmButton;
    private EditText mDeviceName;
    private EditText mDeviceAddress;
    private EditText mDeviceUID;
    private EditText mData;
    private EditText mErrorText;

    private ParcelUuid[] uid_list;

    private UUID uuid;

    private void bluetoothInit(){

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().contains("smartbox")) //Note, you will need to change this to match the name of your device
                {
                    mmDevice = device;
                    mDeviceAddress.setText(device.getAddress());
                    mDeviceName.setText(device.getName());
                    uid_list = mmDevice.getUuids();

//                    boolean success = false;
//                    int i = 0;

//                    for (int i = 0; i < uid_list.length; i++) {
//                        System.out.println(uid_list[i]);
//                    }

                    try {
                        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uid_list[uid_list.length - 1].getUuid());
                        mmSocket.connect();
                        uuid = uid_list[uid_list.length - 1].getUuid();
                        mDeviceUID.setText(uuid + "");
                    } catch (IOException e) {
                        mErrorText.setText("error with UID");
                    }

//                    while (i < uid_list.length && !success) {
//                        try {
//                            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uid_list[i].getUuid());
//                            mmSocket.connect();
//                            uuid = uid_list[0].getUuid();
//                            success = true;
//                        } catch (IOException e) {
//                            mErrorText.setText("error with fetching UID");
//                        }
//                        i++;
//                    }
//
//                    if(success == false) {
//                        mErrorText.setText("end of for loop");
//                    } else {
//                        mDeviceUID.setText("" + uuid);
//                    }
                    break;
                }
            }
        }
    }

    public String receiveData(BluetoothSocket socket) throws IOException{
        InputStream socketInputStream =  socket.getInputStream();
        byte[] buffer = new byte[256];
        int bytes;
        String readMessage = "stuff";

        // Keep looping to listen for received messages
        while (true) {
            try {
                bytes = socketInputStream.read(buffer);            //read bytes from input buffer
                readMessage = new String(buffer, 0, bytes);
                System.out.println("read message: " + readMessage);
                return readMessage;
            } catch (IOException e) {
                break;
            }
        }

        return readMessage;
    }

    public String sendBluetoothMessage(String messangeToSend, EditText textField) {
//        UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID
//        mErrorText.setText("trying to send message: " + messangeToSend);

        try {
//            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            if (!mmSocket.isConnected()){
                mmSocket.connect();
            }

            String msg = messangeToSend;
            //msg += "\n";
            try {
                OutputStream mmOutputStream = mmSocket.getOutputStream();
                mmOutputStream.write(msg.getBytes());
                return receiveData(mmSocket);
            } catch (IOException ioEx) {
                mErrorText.setText("output stream issue");
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            mErrorText.setText("socket connection error");
        }

        return "no message received";

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_input);

        final Handler handler = new Handler();

        mPasswordView = findViewById(R.id.password);
        mWifiName = findViewById(R.id.wifi);
        mConfirmButton = findViewById(R.id.wifi_send_button);
        mDeviceName = findViewById(R.id.device_name);
        mDeviceAddress = findViewById(R.id.device_address);
        mDeviceUID = findViewById(R.id.device_uid);
        mErrorText = findViewById(R.id.error_text);

        bluetoothInit();

        final class workerThread implements Runnable {

            private String btMsg;

            public workerThread(String msg) {
                btMsg = msg;
            }

            public void run()
            {
                mErrorText.setText(sendBluetoothMessage(btMsg, mData));
                while(!Thread.currentThread().isInterrupted())
                {
                    int bytesAvailable;
                    boolean workDone = false;
                    try {
                        final InputStream mmInputStream;
                        mmInputStream = mmSocket.getInputStream();
                        bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            byte[] readBuffer = new byte[1024];
                            mmInputStream.read(packetBytes);

                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    //The variable data now contains our full command
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            mErrorText.setText(data);
                                        }
                                    });

                                    workDone = true;
                                    break;
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                            if (workDone == true){
                                mmSocket.close();
                                break;
                            }
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        mErrorText.setText("Thread Issue");
                    }
                }
            }
        };

        // start temp button handler

        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on temp button click
                String wifi_name = mWifiName.getText().toString();
                String wifi_pass = mPasswordView.getText().toString();

                (new Thread(new workerThread(wifi_name + wifi_pass))).start();
            }
        });

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
    }

}