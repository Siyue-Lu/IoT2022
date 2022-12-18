package com.example.iot2022group7;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class MainActivity extends AppCompatActivity {

    // initialise global const/var
    TextView txv_temp_indoor = null;
    TextView outdoor_light_show = null;
    Switch lightToggle = null;
    TimePicker timePickerFrom = null;
    TimePicker timePickerTo = null;
    Button btnConfirmTime = null;
    Handler handler = new Handler();
    private static final int INTERVAL = 3000;

    private void setLightText(int text, int colour) {
        outdoor_light_show.setText(text);
        outdoor_light_show.setTextColor(getResources().getColor(colour));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        txv_temp_indoor = (TextView) findViewById(R.id.indoorTempShow);
        outdoor_light_show = (TextView) findViewById(R.id.outdoorLightShow);
        lightToggle = (Switch) findViewById(R.id.btnToggle);
        timePickerFrom = (TimePicker) findViewById(R.id.time_picker_from);
        timePickerTo = (TimePicker) findViewById(R.id.time_picker_to);
        btnConfirmTime = (Button) findViewById(R.id.btnConfirmTime);

        // get temperature and display in UI periodically
        new Runnable() {
            @Override
            public void run() {
                String temp = runScript("python SendTemperature.py");
                txv_temp_indoor.setText(temp);
                handler.postDelayed(this, INTERVAL);
            }
        }.run();

        // get light status and display in UI on create
        ((Runnable) () -> {
            boolean isOn = runScript("python SendLightsStatus.py").equals("True");
            if (isOn) {
                setLightText(R.string.txv_on, R.color.teal_700);
            } else {
                setLightText(R.string.txv_off, R.color.title_bar_color);
            }
            lightToggle.setChecked(isOn);
        }).run();

        // run scripts and change UI on toggle
        lightToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ((Runnable) () -> {
                        runScript("python TurnOnLights.py");
                        setLightText(R.string.txv_on, R.color.teal_700);
                    }).run();
                } else {
                    ((Runnable) () -> {
                        runScript("python TurnOffLights.py");
                        setLightText(R.string.txv_off, R.color.title_bar_color);
                    }).run();
                }
            }
        });

        // get data of two time pickers and switch the light on the off according to the set time
        btnConfirmTime.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
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

                new Runnable() {
                    @Override
                    public void run() {
                        runScript("python TurnOnLights.py");
                        handler.postDelayed(this, timeInMillisFrom - System.currentTimeMillis());
                    }
                }.run();

                new Runnable() {
                    @Override
                    public void run() {
                        runScript("python TurnOffLights.py");
                        handler.postDelayed(this, timeInMillisTo - System.currentTimeMillis());
                    }
                }.run();
            }
        });
    }

    public String runScript(String command) {
        String hostname = "7.tcp.eu.ngrok.io";
        int port = 14536;
        String username = "pi";
        String password = "pi";
        StringBuilder lines = new StringBuilder();
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            Connection conn = new Connection(hostname, port); //init connection
            conn.connect(); //start connection to the hostname
            boolean isAuthenticated = conn.authenticateWithPassword(username, password);
            if (isAuthenticated == false)
                throw new IOException("Authentication failed.");
            Session sess = conn.openSession();
            sess.execCommand(command);
            InputStream stdout = new StreamGobbler(sess.getStdout());
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));//reads text
            while (true) {
                String line = br.readLine(); // read line
                if (line == null)
                    break;
                lines.append(line);
            }
            /* Show exit status, if available (otherwise "null") */
            System.out.println("ExitCode: " + sess.getExitStatus());
            sess.close(); // Close this session
            conn.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(2);
        }
        return lines.toString();
    }
}