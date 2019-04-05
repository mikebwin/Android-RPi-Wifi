package smartbox.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static smartbox.bluetoothdemo.Tools.getSSIDs;

/**
 * Created by Yuan Zhang on 2/25/2019.
 */

public class WifiListActivity extends Activity{

    private Button mButton;
    private RecyclerView mRecyclerView;
//    private BluetoothDevice mDevice;
//    private BluetoothSocket mmSocket;

    private AtomicReference wifiString;
    private String[] wifi_list;
    private String[] security_list;

    final byte delimiter = 33;
    int readBufferPosition = 0;

    BluetoothSingleton single = new BluetoothSingleton();

    private String receiveData(BluetoothSocket socket) throws IOException{
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

    private String sendBluetoothMessage(String messangeToSend) {
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

    private void loadWifiNames(String[] list) {
        int i = 0;

        RecyclerView.LayoutManager mLayoutManager =
                new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        StringRecyclerViewAdapter namesAdapter = new StringRecyclerViewAdapter(list);
        mRecyclerView.setAdapter(namesAdapter);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Bundle b = this.getIntent().getExtras();
//        if (b != null)
//            mDevice = b.getParcelable("bluetooth_device");
//
//        try {
//            mmSocket = mDevice.createRfcommSocketToServiceRecord(mDevice.getUuids()[mDevice.getUuids().length - 1].getUuid());
//        } catch (IOException e) {
//            System.out.println("socket issue");
//        }

        setContentView(R.layout.activity_wifi_list);

        mButton = findViewById(R.id.button);
        mRecyclerView = findViewById(R.id.wifi_list);

        wifiString = new AtomicReference();


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
//                            byte[] packetBytes = new byte[bytesAvailable];
//                            byte[] readBuffer = new byte[2048];
//                            mmInputStream.read(packetBytes);
//
//                            for(int i=0;i<bytesAvailable;i++)
//                            {
//                                byte b = packetBytes[i];
//                                if(b == delimiter)
//                                {
//                                    byte[] encodedBytes = new byte[readBufferPosition];
//                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
//                                    final String data = new String(encodedBytes, "US-ASCII");
//                                    readBufferPosition = 0;
//
//                                    //The variable data now contains our full command
//                                    handler.post(new Runnable()
//                                    {
//                                        public void run(){}
//                                    });
//
//                                    workDone = true;
//                                    break;
//                                }
//                                else
//                                {
//                                    readBuffer[readBufferPosition++] = b;
//                                }
//                            }
//                            if (workDone == true){
//                                mSocket.close();
////                                break;
//                            }
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        System.out.println("Thread Issue");
                    }
//                }
            }
        };

        mButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                Thread thread = (new Thread(new workerThread("sudo iwlist wlan0 scanning")));
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(wifiString.toString());
                HashMap<String, String> wifi_map = getSSIDs(wifiString.toString());
                Set<String> wifi_names = wifi_map.keySet();
                System.out.println(wifi_map);
                wifi_list = new String[wifi_names.size()];
                security_list = new String[wifi_names.size()];
                int i = 0;
                for (String name : wifi_names) {
                    wifi_list[i] = name;
                    security_list[i] = wifi_map.get(name);
                    i++;
                }
                loadWifiNames(wifi_list);
            }
        });

        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getApplicationContext(),
                    mRecyclerView ,new RecyclerItemClickListener.OnItemClickListener() {
                        @Override public void onItemClick(View view, int position) {
                            String wifi_name = wifi_list[position];
                            String security = security_list[position];

                            Intent myIntent = new Intent(WifiListActivity.this, WifiInput.class);
                            myIntent.putExtra("wifi_name", wifi_name);
                            myIntent.putExtra("security", security);

//                            Bundle b = new Bundle();
//                            b.putParcelable("bluetooth_device", mDevice);
//                            myIntent.putExtras(b);

                            //had to close socket for it to work after passed through
                            //though maybe a one time use global class thingy might be better for all bluetooth stuff
//                            try {mmSocket.close();} catch (IOException e) {e.printStackTrace();}

                            startActivity(myIntent);
                        }

                    @Override public void onLongItemClick(View view, int position) {
                        // do whatever
                    }
                })
        );

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {

            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Toast.makeText(WifiListActivity.this, "Bluetooth Disconnected", Toast.LENGTH_LONG).show();
                Intent myItent = new Intent(WifiListActivity.this, BluetoothActivity.class);
                startActivity(myItent);
            }
        }
    };

}
