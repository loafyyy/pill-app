package com.led.led;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class BluetoothService extends Service {

    // BT stuff
    static public boolean isBtConnected;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    private ConnectedThread mConnectedThread;

    // handler
    private static Handler bluetoothIn;
    // used to identify handler message
    final int handlerState = 0;
    private StringBuilder recDataString = new StringBuilder();

    private SharedPreferences sp;

    public BluetoothService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "Bluetooh Service Created", Toast.LENGTH_SHORT).show();

        sp = getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);

        // set up handler
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
                        Log.i("dataInPrint", dataInPrint);
                        sendBTSignal(dataInPrint);

                        // clear all string data
                        recDataString.delete(0, recDataString.length());
                    }
                }
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Bluetooh Service Started", Toast.LENGTH_SHORT).show();

        if (intent == null) {
            return Service.START_NOT_STICKY;
        }

        String writeOut = intent.getStringExtra("BT string write");
        if (writeOut != null && isBtConnected) {

            /* todo
            if (writeOut.equals("1")) {
                boxOpened();
            }*/

            mConnectedThread.write(writeOut);
            Log.i("writeout", writeOut);
        }

        // connect to BT
        if (!isBtConnected) {
            address = intent.getStringExtra("address");
            if (address == null) {
                msg("no address BT Service");
            } else {
                // Call the class to connect Bluetooth
                // ConnectBT is an Async Task
                new ConnectBT().execute();
            }
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Bluetooth Service Stopped", Toast.LENGTH_SHORT).show();
        disconnect();
    }

    private void sendBTSignal(String string) {
        // send broadcast with updated numSteps
        Intent intent = new Intent(PillboxControlActivity.RECEIVE_SERVICE);
        intent.putExtra("BT string", string);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {
        if (btAdapter == null) {
            msg("your phone doesn't support bluetooth");
        } else {
            if (!btAdapter.isEnabled()) {
                // Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    // save the time the box was opened
    private void boxOpened() {
        Date currentTime = Calendar.getInstance().getTime();
        String currTimeStr = currentTime.toString();

        // get number of dates in memory
        int numDates = sp.getInt(getString(R.string.preference_num_dates), 0);

        SharedPreferences.Editor editor = sp.edit();

        // put date in memory
        editor.putString(getString(R.string.preference_date) + numDates, currTimeStr);
        editor.commit();

        // add 1 to number of dates
        numDates += 1;
        editor.putInt(getString(R.string.preference_num_dates), numDates);
        editor.commit();
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    // Connect thread - reads and writes from/to Arduino
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
            }
        }
    }

    // Async task for connecting to BT
    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean connectSuccess = true; // if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            msg("Connecting to BT please wait !!!");
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
                    mConnectedThread = new BluetoothService.ConnectedThread(btSocket);
                    mConnectedThread.start();

                    // I send a character when resuming.beginning transmission to check device is connected
                    // If it is not an exception will be thrown in the write method and finish() will be called
                    mConnectedThread.write("x");
                }
            } catch (IOException e) {
                connectSuccess = false; // if the try failed, you can check the exception here
                // todo go back
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) // after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);
            if (!connectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                // TODO go back to DeviceListActivity
            } else {
                msg("Connected.");
                // update global variable
                isBtConnected = true;
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
        // todo return to DeviceListActivity
    }
}
