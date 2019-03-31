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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ButtonUnlock extends Activity {

    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;

    final byte delimiter = 33;
    int readBufferPosition = 0;

    private BluetoothAdapter mBluetoothAdapter;

    private Button mUnlockButton;
    private EditText mLockStatus;

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
                    } catch (IOException e) {
                        mLockStatus.setText("error with UID");
                    }

//                    while (i < uid_list.length && !success) {
//                        try {
//                            mSocket = mmDevice.createRfcommSocketToServiceRecord(uid_list[i].getUuid());
//                            mSocket.connect();
//                            uuid = uid_list[0].getUuid();
//                            success = true;
//                        } catch (IOException e) {
//                            mLockStatus.setText("error with fetching UID");
//                        }
//                        i++;
//                    }
//
//                    if(success == false) {
//                        mLockStatus.setText("end of for loop");
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
//        mLockStatus.setText("trying to send message: " + messangeToSend);

        try {
//            mSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
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
                mLockStatus.setText("output stream issue");
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            mLockStatus.setText("socket connection error");
        }

        return "no message received";

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_button_unlock);

        final Handler handler = new Handler();

        mUnlockButton = findViewById(R.id.button);
        mLockStatus = findViewById(R.id.lock_status);

        bluetoothInit();

        final class workerThread implements Runnable {

            private String btMsg;

            public workerThread(String msg) {
                btMsg = msg;
            }

            public void run()
            {
                mLockStatus.setText(sendBluetoothMessage(btMsg, mLockStatus));
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
                                            mLockStatus.setText(data);
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
                        mLockStatus.setText("Thread Issue");
                    }
                }
            }
        };

        // start temp button handler

        mUnlockButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on temp button click

                (new Thread(new workerThread("python motor.py"))).start();
            }
        });

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
    }

}