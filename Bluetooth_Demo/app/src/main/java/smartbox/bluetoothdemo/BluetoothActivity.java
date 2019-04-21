package smartbox.bluetoothdemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Yuan Zhang on 2/21/2019.
 */



public class BluetoothActivity extends Activity{

    private class Bluetooth {
        private String name;
        private String UID;
        private String address;

        public Bluetooth(String name, String uid, String address) {
            this.name = name;
            this.UID = uid;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public String getUID() {
            return UID;
        }

        public String getAddress() {
            return address;
        }
    }

    BluetoothSingleton single = new BluetoothSingleton();

    private Button mButton;
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;

    private Set<BluetoothDevice> devices;
    private BluetoothDevice[] devicesArray;

    // initializes and turns on BT
    private void btInit() {
        if(!single.getAdapter().isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
    }

    private void loadBTNames() {
        devices = single.getAdapter().getBondedDevices();

        String[] device_names = new String[devices.size()];
        devicesArray = new BluetoothDevice[devices.size()];
        int i = 0;
        for (BluetoothDevice device : devices) {
            devicesArray[i] = device;
            device_names[i] = device.getName();
            i++;
        }

        RecyclerView.LayoutManager mLayoutManager =
                new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        BTRecyclerViewAdapter namesAdapter = new BTRecyclerViewAdapter(devicesArray);
        mRecyclerView.setAdapter(namesAdapter);
    }

    public void connectBT(String item) {
        int num = Integer.parseInt(item);
        final BluetoothDevice mmDevice = devicesArray[num];
        final ParcelUuid[] uid_list = mmDevice.getUuids();

        try {
            single.setmSocket(mmDevice.createRfcommSocketToServiceRecord(uid_list[uid_list.length - 1].getUuid()));
            single.getSocket().connect();
        } catch (IOException e) {
        }
//        final AtomicBoolean condition = new AtomicBoolean(true);
//
//        final Thread t = new Thread() {
//            public void run() {
//                try {
//                    mSocket.connect();
//                } catch (IOException e) {
//                    condition.set(false);
//                    BluetoothActivity.this.runOnUiThread(new Runnable() {
//                        public void run() {
//                            Toast.makeText(BluetoothActivity.this.getBaseContext(), "Hello", Toast.LENGTH_LONG).show();
//                        }
//                    });
//                }
//            }
//        };
//        t.start();
//
//        if(condition.get() == true)
//            return true;
//        else return false;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_list);

        mButton = findViewById(R.id.button);
        mRecyclerView = findViewById(R.id.bt_list);
        mProgressBar = findViewById(R.id.progressBar);

        single.setmAdapter(BluetoothAdapter.getDefaultAdapter());

        btInit();

        mButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                loadBTNames();
            }
        });

        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getApplicationContext(),
                        mRecyclerView ,new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {

                        BluetoothDevice item = devicesArray[position];
                        single.setmDevice(item);

                        System.out.println(item.getName());

                        // will execute async thread task
                        ConnectTask task = new ConnectTask();
                        task.execute(position + "");
                    }

                    @Override public void onLongItemClick(View view, int position) {
                        // do whatever
                    }
                })
        );

    }

    private class ConnectTask extends AsyncTask<String, Void, String> {
        ProgressDialog progress;
        @Override
        // before execution, will start the spinner
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            progress = new ProgressDialog(BluetoothActivity.this);
            progress.setMessage("Connecting...");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.setProgress(0);
            progress.show();
            super.onPreExecute();
        }

        @Override
        //call to connectBT which will try to connect
        protected String doInBackground(String... params) {
            connectBT(params[0]);

            //Things should do in, until progress bar close
            return null;

        }

        @Override
        // once execution is done, progress bar disappears, and if successful, move to next screen, otherwise toast
        protected void onPostExecute(String result) {
            progress.dismiss();
            if (single.getSocket().isConnected()) {
                Intent myIntent = new Intent(BluetoothActivity.this,
                        WifiListActivity.class);
                startActivity(myIntent);
            } else {
                Toast.makeText(BluetoothActivity.this.getBaseContext(), "Bluetooth Connection Failed", Toast.LENGTH_LONG).show();
            }
        }
    }// end async task

}
