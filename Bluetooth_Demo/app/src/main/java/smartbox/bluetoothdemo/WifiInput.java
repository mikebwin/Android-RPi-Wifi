package smartbox.bluetoothdemo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

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

    final byte delimiter = 33;
    int readBufferPosition = 0;

    BluetoothSingleton single = new BluetoothSingleton();

    private EditText mPasswordView;
    private AutoCompleteTextView mWifiName;
    private Button mConfirmButton;

    private String wifi_name;

    private ParcelUuid[] uid_list;

    private UUID uuid;

//    private void bluetoothInit(){
//
//
//        if(!single.getAdapter().isEnabled())
//        {
//            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBluetooth, 0);
//        }
//
//        Set<BluetoothDevice> pairedDevices = single.getAdapter().getBondedDevices();
//        if(pairedDevices.size() > 0)
//        {
//            for(BluetoothDevice device : pairedDevices)
//            {
//                if(device.getName().contains("smartbox")) //Note, you will need to change this to match the name of your device
//                {
//                    mDevice = device;
//                    uid_list = mDevice.getUuids();
//
//                    try {
//                        mSocket = mDevice.createRfcommSocketToServiceRecord(uid_list[uid_list.length - 1].getUuid());
//                        mSocket.connect();
//                        uuid = uid_list[uid_list.length - 1].getUuid();
//                    } catch (IOException e) {
//                    }
//
//                    break;
//                }
//            }
//        }
//    }

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

    public String sendBluetoothMessage(String messangeToSend) {
//        UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID
//        mErrorText.setText("trying to send message: " + messangeToSend);

        try {
//            mSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            if (!single.getSocket().isConnected()){
                single.getSocket().connect();
            }

            String msg = messangeToSend;
            //msg += "\n";
            try {
                OutputStream mmOutputStream = single.getSocket().getOutputStream();
                mmOutputStream.write(msg.getBytes());
                return receiveData(single.getSocket());
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

        wifi_name = (String) getIntent().getSerializableExtra("wifi_name");
        String security = (String) getIntent().getSerializableExtra("security");

        setContentView(R.layout.activity_wifi_input);

        mPasswordView = findViewById(R.id.password);
        mWifiName = findViewById(R.id.wifi);
        mConfirmButton = findViewById(R.id.wifi_send_button);

        setTitle(wifi_name);

        if (security.equals("None")) {
            mPasswordView.setVisibility(View.GONE);
        }

        final AtomicReference wifiString = new AtomicReference();

        final Handler handler = new Handler();
        final class workerThread implements Runnable {

            private String btMsg;

            public workerThread(String msg) {
                btMsg = msg;
            }

            public void run()
            {
                // here is the call to send a message
                wifiString.set(sendBluetoothMessage(btMsg));
//                while(!Thread.currentThread().isInterrupted())
//                {
                int bytesAvailable;
                boolean workDone = false;
                try {
                    final InputStream mmInputStream;
                    mmInputStream = single.getSocket().getInputStream();
                    bytesAvailable = mmInputStream.available();

                    if(bytesAvailable > 0)
                    {
                        final int BUFFER_SIZE = 10000;
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int bytes = 0;

                        while (true) {
                            try {
                                bytes = mmInputStream.read(buffer, bytes, BUFFER_SIZE - bytes);
                            } catch (IOException e) {
                                System.out.println("exception");
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    System.out.println("Thread Issue");
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