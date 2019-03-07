package smartbox.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
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

    private Button mButton;
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;

    private BluetoothSocket mmSocket;
    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> devices;
    private BluetoothDevice[] devicesArray;

    // initializes and turns on BT
    private void btInit() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
    }

    private void loadBTNames() {
        devices = mBluetoothAdapter.getBondedDevices();

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

    public boolean connectBT(BluetoothDevice device) {
        final BluetoothDevice mmDevice = device;
        final ParcelUuid[] uid_list = mmDevice.getUuids();

//        ProgressDialog progress=new ProgressDialog(this);
//        progress.setMessage("Connecting...");
//        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        progress.setIndeterminate(true);
////                        progress.setProgress(0);
//        progress.show();

        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uid_list[uid_list.length - 1].getUuid());
            mmSocket.connect();
        } catch (IOException e) {
            Toast.makeText(BluetoothActivity.this, "Bluetooth connection failed", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;

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

                        System.out.println(item.getName());

                        if(connectBT(item)) {
                            Intent myIntent = new Intent(BluetoothActivity.this,
                                    WifiListActivity.class);
                            Bundle b = new Bundle();

                            //had to close socket for it to work after passed through
                            //though maybe a one time use global class thingy might be better for all bluetooth stuff
                            try {
                                mmSocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            b.putParcelable("bluetooth_device", item);
                            myIntent.putExtras(b);
                            startActivity(myIntent);
                        } else {
                        }
                    }

                    @Override public void onLongItemClick(View view, int position) {
                        // do whatever
                    }
                })
        );

    }

}
