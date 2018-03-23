package com.led.led;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class LedControlActivity extends AppCompatActivity {


    private Button btnOn, btnOff, btnDisconnect;
    private TextView txtString, lumn;
    private SeekBar brightness;
    private ProgressDialog progress;
    private static Handler bluetoothIn;

    // used to identify handler message
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    private boolean isBtConnected;

    private ConnectedThread mConnectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led_control);

        //Link the buttons and textViews to respective views
        btnOn = (Button) findViewById(R.id.on_button);
        btnOff = (Button) findViewById(R.id.off_button);
        btnDisconnect = (Button) findViewById(R.id.disconnect_button);
        txtString = (TextView) findViewById(R.id.read);
        lumn = (TextView) findViewById(R.id.lumn);
        brightness = (SeekBar) findViewById(R.id.seekBar);

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                // if message is what we want
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    // keep appending to string until "~"
                    // determine the end-of-line
                    int endOfLineIndex = recDataString.indexOf("~");
                    // make sure there are data before ~
                    if (endOfLineIndex > 0) {
                        // extract string
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);
                        txtString.setText("Data Received = " + dataInPrint);
                        // clear all string data
                        recDataString.delete(0, recDataString.length());
                    }
                }
            }
        };

        // Set up onClick listeners for buttons to send 1 or 0 to turn on/off LED
        btnOff.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("0");    // Send "0" via Bluetooth
            }
        });

        btnOn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("1");    // Send "1" via Bluetooth
            }
        });

        // disconnect BT button
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String brightness = String.valueOf(progress);
                if (fromUser) {
                    lumn.setText(brightness);
                    try {
                        // set brightness
                        btSocket.getOutputStream().write(String.valueOf(progress).getBytes());
                    } catch (IOException e) {

                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();

        // Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();

        // Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(DeviceListActivity.EXTRA_ADDRESS);

        // Call the class to connect Bluetooth
        // ConnectBT is an Async Task
        new ConnectBT().execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    // Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {
        if (btAdapter == null) {
            msg("your phone doesn't support bluetooth");
        } else {
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    // function for making toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private void disconnect() {
        if (btSocket != null) //If the btSocket is busy
        {
            try {
                // close connection
                btSocket.close();
            } catch (IOException e) {
                msg("Error");
            }
        }
        // return to DeviceListActivity
        finish();
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    //read bytes from input buffer
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean connectSuccess = true; // if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            //show a progress dialog
            progress = ProgressDialog.show(LedControlActivity.this, "Connecting...", "Please wait!!!");
        }

        @Override
        protected Void doInBackground(Void... devices) // while the progress dialog is shown, the connection is done in background
        {
            try {
                if (btSocket == null || !isBtConnected) {

                    // create device and set the MAC address
                    btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
                    checkBTState();
                    BluetoothDevice device = btAdapter.getRemoteDevice(address);
                    btSocket = createBluetoothSocket(device);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    // Establish the Bluetooth socket connection.
                    btSocket.connect();
                    mConnectedThread = new ConnectedThread(btSocket);
                    mConnectedThread.start();

                    // I send a character when resuming.beginning transmission to check device is connected
                    // If it is not an exception will be thrown in the write method and finish() will be called
                    mConnectedThread.write("x");
                }
            } catch (IOException e) {
                connectSuccess = false; // if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) // after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);
            if (!connectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                // go back to DeviceListActivity
                finish();
            } else {
                msg("Connected.");
                // update global variable
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }
}

