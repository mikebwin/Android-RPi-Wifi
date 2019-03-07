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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class WifiInput extends Activity {

    BluetoothSocket mSocket;
    BluetoothDevice mDevice = null;

    final byte delimiter = 33;
    int readBufferPosition = 0;

    private BluetoothAdapter mBluetoothAdapter;

    private EditText mPasswordView;
    private AutoCompleteTextView mWifiName;
    private Button mConfirmButton;

    private String wifi_name;

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
                    mDevice = device;
                    uid_list = mDevice.getUuids();

                    try {
                        mSocket = mDevice.createRfcommSocketToServiceRecord(uid_list[uid_list.length - 1].getUuid());
                        mSocket.connect();
                        uuid = uid_list[uid_list.length - 1].getUuid();
                    } catch (IOException e) {
                    }

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
//            mSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            if (!mSocket.isConnected()){
                mSocket.connect();
            }

            String msg = messangeToSend;
            //msg += "\n";
            try {
                OutputStream mmOutputStream = mSocket.getOutputStream();
                mmOutputStream.write(msg.getBytes());
                return receiveData(mSocket);
            } catch (IOException ioEx) {
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
        }

        return "no message received";

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = this.getIntent().getExtras();
        if (b != null)
            mDevice = b.getParcelable("bluetooth_device");

        wifi_name = (String) getIntent().getSerializableExtra("wifi_name");
        String security = (String) getIntent().getSerializableExtra("security");

        try {
            mSocket = mDevice.createRfcommSocketToServiceRecord(mDevice.getUuids()[mDevice.getUuids().length - 1].getUuid());
        } catch (IOException e) {
            System.out.println("socket issue");
        }

        setContentView(R.layout.activity_wifi_input);

        final Handler handler = new Handler();

        mPasswordView = findViewById(R.id.password);
        mWifiName = findViewById(R.id.wifi);
        mConfirmButton = findViewById(R.id.wifi_send_button);

        setTitle(wifi_name);

        if (security.equals("None")) {
            mPasswordView.setVisibility(View.GONE);
        }

        final class workerThread implements Runnable {

            private String btMsg;

            public workerThread(String msg) {
                btMsg = msg;
            }

            public void run()
            {
                while(!Thread.currentThread().isInterrupted())
                {
                    int bytesAvailable;
                    boolean workDone = false;
                    try {
                        final InputStream mmInputStream;
                        mmInputStream = mSocket.getInputStream();
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
                                mSocket.close();
                                break;
                            }
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                    }
                }
            }
        };

        // start temp button handler

        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on temp button click
                String wifi_pass = mPasswordView.getText().toString();

                (new Thread(new workerThread("iwconfig wlan0 essid " + wifi_name + " key s:" + wifi_pass))).start();
            }
        });

        if (security.equals("None")) {
            (new Thread(new workerThread("iwconfig wlan0 essid " + wifi_name))).start();
        }

//        if(!mBluetoothAdapter.isEnabled())
//        {
//            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBluetooth, 0);
//        }
    }

}