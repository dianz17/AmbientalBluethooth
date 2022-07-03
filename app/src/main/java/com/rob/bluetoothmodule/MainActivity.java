package com.rob.bluetoothmodule;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int ENABLE_BT_CODE = 1;
    private static final int DISCOVERABLE_DURATION = 300;
    private static final int DISCOVERABLE_BT_REQUEST_CODE = 2;
    private int counter = 0;

    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    ToggleButton btnBluetooth, btnLight, btnConnect;
    Button btnClear, btnKill;
    Spinner spinPaired;
    TextView textView;
    ProgressBar progressBar;
    ScrollView scrollView;

    BluetoothAdapter bluetoothAdapter;
    List<DeviceInfo> pairedList;
    ListAdapter adapterPaired;
    Set<BluetoothDevice> pairedDevices;

    String address = null;
    BluetoothSocket btSocket;

    Handler handler;
    CreateConnectThread createConnectThread;
    ConnectedThread connectedThread;
    private static final int CONNECTING_STATUS = 1; // used by handler for a message status
    private static final int MESSAGE_READ = 2; // used by handler for message update

    WarningDialog warningDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        textView = findViewById(R.id.textView);
        btnBluetooth = findViewById(R.id.btnBluetooth);
        btnLight = findViewById(R.id.btnLight);
        btnConnect = findViewById(R.id.btnConnect);
        btnClear = findViewById(R.id.btnClear);
        btnKill = findViewById(R.id.btnKill);
        spinPaired = findViewById(R.id.spinPaired);
        progressBar = findViewById(R.id.progressBar);

        //Scrollview abajo
//        scrollView = (ScrollView)findViewById(R.id.scrollView);
//
//        scrollView.post(new Runnable() {
//            public void run() {
//                scrollView.fullScroll(View.FOCUS_DOWN);
//            }
//        });

        //Cambio
        textView.setMovementMethod(new ScrollingMovementMethod());

        pairedList = new ArrayList<>();
        adapterPaired = new ListAdapter(getApplicationContext(), pairedList);
        spinPaired.setAdapter(adapterPaired);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // verify if the device supports bluetooth
        if(bluetoothAdapter == null) {
            WarningDialog warningDialog = new WarningDialog("This device does not support Bluetooth.");
            warningDialog.show(getSupportFragmentManager(), "warning no bluetooth");
            System.exit(1);
        }

        btnBluetooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) enableBT();
                else disableBT();
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
                        progressBar.setVisibility(View.VISIBLE);
                        btnConnect.setEnabled(false);
                        spinPaired.setEnabled(false);
                        btnBluetooth.setEnabled(false);
                        createConnectThread.start();
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Failed to obtain device address",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // close connection to socket
                    //progressBar.setVisibility(View.VISIBLE);
                    //btnConnect.setEnabled(false);
                    spinPaired.setEnabled(true);
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

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textView.setText("");
            }
        });

        btnKill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnLight.setChecked(false);
                btnConnect.setChecked(false);
                btnBluetooth.setChecked(false);
            }
        });

        //// IMPORTANT this will get all the states from threads
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch(msg.what) {
                    case CONNECTING_STATUS:
                        progressBar.setVisibility(View.GONE);
                        btnConnect.setEnabled(true);
                        btnBluetooth.setEnabled(true);
                        switch(msg.arg1){
                            case 1: // successfull
                                btnLight.setEnabled(true);
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
                                btnConnect.setChecked(false);
                                Toast.makeText(getApplicationContext(), "Cannot connect to socket.",
                                        Toast.LENGTH_SHORT).show();
                                break;
                            case 2:
                                btnConnect.setEnabled(true);
                                switch(msg.arg2) {
                                    case 1:
                                        Toast.makeText(getApplicationContext(), "Socket closed",
                                                Toast.LENGTH_SHORT).show();
                                        break;
                                    case -1:
                                        //btnConnect.setChecked(true);
                                        Toast.makeText(getApplicationContext(), "Error in socket.",
                                                Toast.LENGTH_SHORT).show();
                                        break;
                                }
                        }
                        break;
                    case MESSAGE_READ:
                        switch (msg.arg1) {
                            case 1: // message
                                String arduinoMsg = msg.obj.toString();

                                if (arduinoMsg.equals("2") && counter==0) {
                                    textView.append("Metano/Butano: ");
                                    counter++;
                                }
                                else if (arduinoMsg.equals("9") && counter==0){
                                    textView.append("Monóxido de Carbono: ");
                                    counter++;
                                }
                                else if(arduinoMsg.equals("135") && counter==0){
                                    textView.append("Dióxido de Carbono: ");
                                    counter++;
                                }
                                else if(counter == 1){
                                    textView.append(arduinoMsg + "\n");
                                    counter=0;
                                }


//                                if (counter == 0) {
//                                    textView.append("Metano/Butano:" + arduinoMsg + "\n");
//                                    counter++;
//                                }
//                                else if (counter == 1){
//                                    textView.append("Monóxido de Carbono:" + arduinoMsg + "\n");
//                                    counter++;
//                                }
//                                else if(counter==2){
//                                    textView.append("Dióxido de Carbono:" + arduinoMsg + "\n");
//                                    counter=0;
//                                }



                                break;
                            case -1:
                                //btnConnect.setChecked(false);
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

                // connection succesfull at this point
                connectedThread = new ConnectedThread();
                connectedThread.run();
            } catch (Exception e) {
                try {
                    btSocket.close(); // close socket first
                    handler.obtainMessage(CONNECTING_STATUS,-1, -1).sendToTarget();
                } catch(Exception e1) {
                    handler.obtainMessage(CONNECTING_STATUS,1, -1).sendToTarget();
                }
            }


        }

        public void closeConnectThread() {
            try {
                // close connection to socket
                connectedThread.serialWrite("exit");
                btSocket.close();
                handler.obtainMessage(CONNECTING_STATUS,2, 1).sendToTarget();
            } catch (Exception e) {
                handler.obtainMessage(CONNECTING_STATUS,2, -1).sendToTarget();
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
        btnConnect.setChecked(false);
        btnConnect.setEnabled(false);
        btnBluetooth.setChecked(false);

        bluetoothAdapter.disable();

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
                btnConnect.setEnabled(true);
                Toast.makeText(getApplicationContext(), "Bluetooth is ON",
                        Toast.LENGTH_SHORT).show();
                makeDiscoverable();
                discoverDevices();
                pairedDevicesList();
            } else {
                btnBluetooth.setChecked(false);
                btnConnect.setEnabled(false);
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
        if(bluetoothAdapter.isEnabled() && spinPaired.getCount()>1) {
            btnConnect.setChecked(true);
        }
    }

}
