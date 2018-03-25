package com.led.led;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;


public class PillboxControlActivity extends AppCompatActivity {

    // views
    private Button btnOn, btnOff, btnConnect;
    private TextView txtString;
    private ImageView imageView;

    private MediaPlayer mediaPlayer = null;

    // code for box opened from Arduino
    private String boxOpened = "o";
    // code for box closed from Arduino
    private String boxClosed = "c";

    private Context mContext;
    private SharedPreferences sp;

    private String address;
    private static boolean isBoxOpen = false;

    private LocalBroadcastManager bManager;
    // BroadcastReceiver for BluetoothService
    public static final String RECEIVE_SERVICE = "BT_RECEIVE_SERVICE";
    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("PillboxControlActivity", "datareceived");

            if (intent.getAction().equals(RECEIVE_SERVICE)) {
                String readIn = intent.getStringExtra("BT string");
                txtString.setText("Data received: " + readIn);
                // box opened
                if (readIn.equals(boxOpened)) {
                    setImage(true);

                    if (!isBoxOpen) {
                        isBoxOpen = true;

                        // todo play message
                        playMessage();

                        boxOpened();
                    }
                } else {
                    setImage(false);
                    isBoxOpen = false;
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pillbox_control);

        // Link the buttons and textViews to respective views
        btnOn = (Button) findViewById(R.id.on_button);
        btnOff = (Button) findViewById(R.id.off_button);
        btnConnect = (Button) findViewById(R.id.connect_button);
        txtString = (TextView) findViewById(R.id.read);
        imageView = (ImageView) findViewById(R.id.image_view);

        mContext = this;
        sp = getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);

        // set up broadcast receiver
        bManager = LocalBroadcastManager.getInstance(mContext);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECEIVE_SERVICE);
        bManager.registerReceiver(bReceiver, intentFilter);

        // set up BT
        // Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();
        // Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(DeviceListActivity.EXTRA_ADDRESS);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // use this to start and trigger a service
                Intent i = new Intent(mContext, BluetoothService.class);
                // potentially add data to the intent
                i.putExtra("address", address);
                mContext.startService(i);
            }
        });

        // Set up onClick listeners for buttons to send 1 or 0 to turn on/off LED
        btnOff.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send "0" via Bluetooth
                sendBTSignalWrite("0");
            }
        });

        btnOn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send "1" via Bluetooth
                sendBTSignalWrite("1");
            }
        });
    }

    // todo
    private void sendBTSignalWrite(String string) {
        Intent i = new Intent(mContext, BluetoothService.class);
        // potentially add data to the intent
        i.putExtra("BT string write", string);
        mContext.startService(i);
    }

    private void setImage(boolean b) {
        if (b) {
            imageView.setImageResource(R.drawable.family);
        } else {
            imageView.setImageResource(R.drawable.family_2);
        }
    }


    private void playMessage() {
        try {
            mediaPlayer = MediaPlayer.create(mContext, R.raw.incentivizing_msg);
            // mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {

        }
    }

    // save the time the box was opened
    private void boxOpened() {
        Log.i("PillboxControlActivity", "box opened");
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        bManager.unregisterReceiver(bReceiver);

        // todo media player
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate menu
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.menu_homepage) {
            startActivity(new Intent(mContext, HomeActivity.class));
            return true;
        } else if (id == R.id.menu_bluetooth) {
            startActivity(new Intent(mContext, PillboxControlActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
}

