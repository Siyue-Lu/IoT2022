package com.example.iot2022group7;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.concurrent.CompletableFuture;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class LampActivity extends AppCompatActivity {
    ImageView back;

    Helper helper = new Helper();
    Handler handler = helper.getHandler();

    TextView outdoor_light_show = null;
    SwitchCompat lightToggle;

    TimePicker timePickerFrom = null;
    TimePicker timePickerTo = null;
    Button btnConfirmTime = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lamp);
        getSupportActionBar().hide();
        back = (ImageView) findViewById(R.id.imageView);
        back.setOnClickListener(view -> startActivity(new Intent(LampActivity.this, MainActivity.class)));

        outdoor_light_show = (TextView) findViewById(R.id.outdoorLightShow);
        lightToggle = (SwitchCompat) findViewById(R.id.lightToggle);
        timePickerFrom = (TimePicker) findViewById(R.id.time_picker_from);
        timePickerTo = (TimePicker) findViewById(R.id.time_picker_to);
        btnConfirmTime = (Button) findViewById(R.id.btnConfirmTime);

        helper.updateDeviceStatus("Lights", outdoor_light_show, lightToggle);

        if (lightToggle != null) {
            lightToggle.setOnCheckedChangeListener(helper.getCheckListener("Lights", outdoor_light_show, lightToggle));
        }

        // get data of two time pickers and switch the light on/off according to the set time
        btnConfirmTime.setOnClickListener(v -> {
            int hourFrom = timePickerFrom.getHour();
            int minuteFrom = timePickerFrom.getMinute();
            int hourTo = timePickerTo.getHour();
            int minuteTo = timePickerTo.getMinute();

            Calendar calendarFrom = Calendar.getInstance();
            calendarFrom.setTimeInMillis(System.currentTimeMillis());
            calendarFrom.set(Calendar.HOUR_OF_DAY, hourFrom);
            calendarFrom.set(Calendar.MINUTE, minuteFrom);
            calendarFrom.set(Calendar.SECOND, 0);

            Calendar calendarTo = Calendar.getInstance();
            calendarTo.setTimeInMillis(System.currentTimeMillis());
            calendarTo.set(Calendar.HOUR_OF_DAY, hourTo);
            calendarTo.set(Calendar.MINUTE, minuteTo);
            calendarTo.set(Calendar.SECOND, 0);

            long timeInMillisFrom = calendarFrom.getTimeInMillis();
            long timeInMillisTo = calendarTo.getTimeInMillis();
            long currentTime = System.currentTimeMillis();

            handler.postDelayed(() -> helper.runAsync("python TurnOnLights.py")
                    .thenAccept(result -> helper.setDeviceStatus(outdoor_light_show, lightToggle, true))
                    .exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    }), timeInMillisFrom - currentTime);

            handler.postDelayed(() -> helper.runAsync("python TurnOffLights.py")
                    .thenAccept(result -> helper.setDeviceStatus(outdoor_light_show, lightToggle, false))
                    .exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    }), timeInMillisTo - currentTime);

            helper.showToast(this, "Schedule");
        });
    }


}