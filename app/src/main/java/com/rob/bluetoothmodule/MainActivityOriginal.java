package com.rob.bluetoothmodule;

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
import android.os.Looper;
import android.os.Message;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivityOriginal extends AppCompatActivity {
    private static final int ENABLE_BT_CODE = 1;
    private static final int DISCOVERABLE_DURATION = 300;
    private static final int DISCOVERABLE_BT_REQUEST_CODE = 2;

    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    ToggleButton btnBluetooth, btnLight, btnConnect;
    ListView listBluetooth;
    Spinner spinPaired;
    TextView textView;

    BluetoothAdapter bluetoothAdapter;
    List<DeviceInfo> deviceInfoList, pairedList;
    ListAdapter adapter, adapterPaired;

    Set<BluetoothDevice> pairedDevices;

    String address = null;
    BluetoothSocket btSocket;

    Handler handler;
    CreateConnectThread createConnectThread;
    ConnectedThread connectedThread;
    private static final int CONNECTING_STATUS = 1; // used by handler for a message status
    private static final int MESSAGE_READ = 2; // used by handler for message update

    WarningDialog warningDialog;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // whenever a remote bluetooth is found
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                // get device object from intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add name and address to array adapter for the list
                adapter.deviceInfoList.add(new DeviceInfo(device.getName(), device.getAddress()));
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        textView = findViewById(R.id.textView);
        btnBluetooth = findViewById(R.id.btnBluetooth);
        btnLight = findViewById(R.id.btnLight);
        listBluetooth = findViewById(R.id.listBluetooth);
        spinPaired = findViewById(R.id.spinPaired);
        btnConnect = findViewById(R.id.btnConnect);

        pairedList = new ArrayList<>();
        adapterPaired = new ListAdapter(getApplicationContext(), pairedList);
        spinPaired.setAdapter(adapterPaired);

        deviceInfoList = new ArrayList<>();
        adapter = new ListAdapter(getApplicationContext(), deviceInfoList);
        listBluetooth.setAdapter(adapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null) {
            WarningDialog warningDialog = new WarningDialog("This device does not support Bluetooth.");
            warningDialog.show(getSupportFragmentManager(), "warning no bluetooth");
            System.exit(1);
        }

        btnBluetooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    enableBT();
                } else {
                    disableBT();
                }
            }
        });

        btnConnect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    try {
                        address = ( (TextView) spinPaired.getSelectedView().findViewById(R.id.txtAddress)
                                ).getText().toString();
                        createConnectThread = new CreateConnectThread(address);
                        // start connection thread
                        createConnectThread.start();
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Failed to obtain device address",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // close connection to socket
                    createConnectThread.closeConnectThread();
                }
            }
        });

        btnLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    try {
                        turnOnLed();
                    } catch (Exception e) {
                        warningDialog = new WarningDialog(e.getMessage());
                        btnLight.setChecked(false);
                    }
                } else {
                    try {
                        turnOffLed();
                    } catch (Exception e) {
                        warningDialog = new WarningDialog(e.getMessage());
                        btnLight.setChecked(false);
                    }
                }
            }
        });

        ////
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch(msg.what) {
                    case CONNECTING_STATUS:
                        switch(msg.arg1){
                            case 1: // successfull
                                switch(msg.arg2) {
                                    case 1: // succesfull
                                        Toast.makeText(getApplicationContext(), "Socket connected.",
                                                Toast.LENGTH_SHORT).show();
                                        break;
                                    case -1: // could not close
                                        Toast.makeText(getApplicationContext(), "Could not close socket.",
                                                Toast.LENGTH_SHORT).show();
                                        break;
                                }
                                break;
                            case -1: // cannot connect
                                Toast.makeText(getApplicationContext(), "Cannot connect to socket.",
                                        Toast.LENGTH_SHORT).show();
                                break;
                        }
                        break;
                    case MESSAGE_READ:
                        switch (msg.arg1) {
                            case 1: // message
                                String arduinoMsg = msg.obj.toString();
                                textView.append(arduinoMsg + "\n");
                                break;
                            case -1:
                                Toast.makeText(getApplicationContext(), "Error in input stream",
                                        Toast.LENGTH_SHORT).show();
                                break;
                        }
                }
            }
        };
    }

    public class CreateConnectThread extends Thread {
        public CreateConnectThread(String address) {
            BluetoothDevice device;
            BluetoothSocket socket = null;
            try {
                device = bluetoothAdapter.getRemoteDevice(address);
                socket = device.createInsecureRfcommSocketToServiceRecord(myUUID);
                //socket.connect(); // start connection
            } catch(Exception e) {
                Toast.makeText(getApplicationContext(), "Error in socket",
                        Toast.LENGTH_SHORT).show();
                btnConnect.setChecked(false);
            }
            btSocket = socket;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();
            try {
                // connect socket
                btSocket.connect(); // start connection
                handler.obtainMessage(CONNECTING_STATUS,1, 1).sendToTarget();
            } catch (Exception e) {
                try {
                    btSocket.close(); // close socket first
                    handler.obtainMessage(CONNECTING_STATUS,-1, -1).sendToTarget();
                } catch(Exception e1) {
                    handler.obtainMessage(CONNECTING_STATUS,1, -1).sendToTarget();
                }
            }

            // connection succesfull at this point
            connectedThread = new ConnectedThread();
            connectedThread.run();
        }

        public void closeConnectThread() {
            try {
                // close connection to socket
                btSocket.close();
                Toast.makeText(getApplicationContext(), "Socket closed",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error in socket",
                        Toast.LENGTH_SHORT).show();
                btnConnect.setChecked(true);
            }
        }
    }

    public class ConnectedThread extends Thread { // obtain input and output from socket
        InputStream inputStream;
        OutputStream outputStream;

        public ConnectedThread() {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // get input and outputs
            try {
                tmpIn = btSocket.getInputStream();
                tmpOut = btSocket.getOutputStream();
            } catch (Exception e) { }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024]; // buffer for storing the stream
            int bytes = 0; // number of bytes returned from stream
            // keep listening to input stream
            while(true) {
                try {
                    // read input stream
                    buffer[bytes] = (byte) inputStream.read();
                    String readMessage;
                    if(buffer[bytes]=='\n') { // if theres a next line
                        readMessage = new String(buffer, 0, bytes);
                        //textView.append(readMessage); // a posible error
                        // send message to handler
                        handler.obtainMessage(MESSAGE_READ, 1, 1, readMessage).sendToTarget();
                        bytes = 0; // restart count
                    } else {
                        bytes++;
                    }
                } catch (Exception e) {
                    handler.obtainMessage(MESSAGE_READ, -1, -1).sendToTarget();
                    break;
                }
            }
        }

        public void serialWrite(String input) throws Exception {
            try {
                outputStream.write(input.getBytes());
            } catch (Exception e) {
                throw e;
            }
        }

        public void closeConnection() {
            try {
                btSocket.close();
            } catch (Exception e) {}
        }
    }

    private void turnOnLed() throws Exception {
        if (btSocket != null) {
            try {
                connectedThread.serialWrite("LY");
            } catch (Exception e) {
                throw new Exception("There has been an error in stream.");
            }
        } else {
            throw new Exception("Not connected.");
        }
    }

    private void turnOffLed() throws Exception {
        if (btSocket != null) {
            try {
                connectedThread.serialWrite("LN");
            } catch (Exception e) {
                throw new Exception("There has been an error in stream.");
            }
        } else {
            throw new Exception("Not connected.");
        }
    }

    private void enableBT() {
        btnBluetooth.setChecked(true);

        if(!bluetoothAdapter.isEnabled()) {
            registerBroadcast();
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, ENABLE_BT_CODE);
        } else {
            // To discover remote Bluetooth devices
            discoverDevices();
            // Make local device discoverable by other devices
            makeDiscoverable();
            // show paired devices
            pairedDevicesList();
        }
    }

    private void disableBT() {
        btnBluetooth.setChecked(false);
        btnConnect.setChecked(false);

        bluetoothAdapter.disable();
        this.unregisterReceiver(broadcastReceiver);

        deviceInfoList.clear();
        adapter.notifyDataSetChanged();
        pairedList.clear();
        adapterPaired.notifyDataSetChanged();

        Toast.makeText(getApplicationContext(), "Bluetooth is OFF",
                Toast.LENGTH_SHORT).show();
    }

    protected void discoverDevices() {
        // to scan bluetooth devices
        if(bluetoothAdapter.startDiscovery()) {
            Toast.makeText(getApplicationContext(), "Discovering other bluetooth devices...",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Discovery failed to start.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    protected void makeDiscoverable(){
        // Make local device discoverable
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
        startActivityForResult(discoverableIntent, DISCOVERABLE_BT_REQUEST_CODE);
    }

    private void pairedDevicesList() {
        pairedDevices = bluetoothAdapter.getBondedDevices();

        if(pairedDevices.size() > 0) {
            btnConnect.setEnabled(true);
            for(BluetoothDevice bt : pairedDevices) {
                pairedList.add(new DeviceInfo(bt.getName(), bt.getAddress()));
            }
        } else {
            btnConnect.setEnabled(false);
        }
        adapterPaired.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ENABLE_BT_CODE) {
            if(resultCode == Activity.RESULT_OK) {
                btnBluetooth.setChecked(true);
                Toast.makeText(getApplicationContext(), "Bluetooth is ON",
                        Toast.LENGTH_SHORT).show();
                makeDiscoverable();
                discoverDevices();
                pairedDevicesList();
            } else {
                btnBluetooth.setChecked(false);
                Toast.makeText(getApplicationContext(), "Bluetooth is OFF",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == DISCOVERABLE_BT_REQUEST_CODE) {
            if (resultCode == DISCOVERABLE_DURATION){
                Toast.makeText(getApplicationContext(), "Your device is now discoverable by other devices for " +
                                DISCOVERABLE_DURATION + " seconds",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Fail to enable discoverability on your device.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(bluetoothAdapter.isEnabled()) {
            btnBluetooth.setChecked(true);
        } else {
            btnBluetooth.setChecked(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register broadcast receiver
        registerBroadcast();
        if(bluetoothAdapter.isEnabled() && spinPaired.getCount()>1) {
            btnConnect.setChecked(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(broadcastReceiver);
    }

    void registerBroadcast() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(broadcastReceiver, filter);
    }

}