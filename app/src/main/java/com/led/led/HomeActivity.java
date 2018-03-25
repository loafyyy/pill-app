package com.led.led;

import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private Button showDatesBtn, clearDatesBtn, pickTimeBtn;
    private TextView dateTV;
    private ListView listView;

    private Context mContext;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mContext = this;
        sp = mContext.getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);

        dateTV = findViewById(R.id.date_tv);
        listView = findViewById(R.id.home_listview);
        clearDatesBtn = findViewById(R.id.clear_dates_button);
        showDatesBtn = findViewById(R.id.show_dates_button);
        pickTimeBtn = findViewById(R.id.pick_time_button);

        pickTimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment newFragment = new TimePickerFragment();
                newFragment.show(getFragmentManager(), "TimePicker");
            }
        });

        showDatesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDates();
            }
        });

        clearDatesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearDates();
            }
        });
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
            // already on home page
            return true;
        } else if (id == R.id.menu_bluetooth) {
            startActivity(new Intent(mContext, PillboxControlActivity.class));
        }

        return super.onOptionsItemSelected(item);
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

    private void showDates() {

        List<String> dateList = new ArrayList();

        // get number of dates in memory
        int numDates = sp.getInt(getString(R.string.preference_num_dates), 0);

        for (int i = 0; i < numDates; i++) {
            String date = sp.getString(getString(R.string.preference_date) + i, "no date");
            dateList.add(date);
        }

        // set up adapter for list
        final ArrayAdapter adapter = new ArrayAdapter(mContext, android.R.layout.simple_list_item_1, dateList);
        listView.setAdapter(adapter);
    }

    private void clearDates() {
        // get number of dates in memory
        int numDates = sp.getInt(getString(R.string.preference_num_dates), 0);

        for (int i = 0; i < numDates; i++) {
            sp.edit().remove(getString(R.string.preference_date) + i).commit();
        }

        sp.edit().remove(getString(R.string.preference_num_dates)).commit();

        final ArrayAdapter adapter = new ArrayAdapter(mContext, android.R.layout.simple_list_item_1, new ArrayList());
        listView.setAdapter(adapter);
    }

    // adds alarm
    public void setAlarm(final long time) {

        int id = 0;

        final Intent intent = new Intent(mContext, PillAlarmReceiver.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        final PendingIntent alarmIntent = PendingIntent.getBroadcast(mContext, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        final AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        // for testing 5 second notification TODO change to actual time
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5000, alarmIntent);
        //alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + time, alarmIntent);
    }
}
