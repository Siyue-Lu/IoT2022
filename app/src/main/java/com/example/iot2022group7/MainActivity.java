package com.example.iot2022group7;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
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

public class MainActivity extends AppCompatActivity {

    TextView indoor_temp_show = null;
    TextView indoor_heater_show = null;
    SwitchCompat heatToggle = null;
    EditText tempInputMin = null;
    EditText tempInputMax = null;
    Button btnConfirmTemp = null;
    boolean isTempSet = false;
    TextView outdoor_light_show = null;
    SwitchCompat lightToggle = null;
    TimePicker timePickerFrom = null;
    TimePicker timePickerTo = null;
    Button btnConfirmTime = null;
    Handler handler = new Handler();
    private static final int INTERVAL = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        indoor_temp_show = (TextView) findViewById(R.id.indoorTempShow);
        indoor_heater_show = (TextView) findViewById(R.id.indoorHeaterShow);
        heatToggle = (SwitchCompat) findViewById(R.id.heatToggle);
        tempInputMin = (EditText) findViewById(R.id.temp_min);
        tempInputMax = (EditText) findViewById(R.id.temp_max);
        btnConfirmTemp = (Button) findViewById(R.id.btnConfirmTemp);
        outdoor_light_show = (TextView) findViewById(R.id.outdoorLightShow);
        lightToggle = (SwitchCompat) findViewById(R.id.lightToggle);
        timePickerFrom = (TimePicker) findViewById(R.id.time_picker_from);
        timePickerTo = (TimePicker) findViewById(R.id.time_picker_to);
        btnConfirmTime = (Button) findViewById(R.id.btnConfirmTime);
        final float[] tempMin = new float[1];
        final float[] tempMax = new float[1];

        // get temperature periodically, display on UI and run scripts under certain conditions
        new Runnable() {
            @Override
            public void run() {
                runAsync("python SendTemperature.py")
                        .thenAccept(result -> {
                            String currTempStr = indoor_temp_show.getText().toString();
                            float currTemp = Float.parseFloat(result);

                            if (!currTempStr.equals(result)) {
                                runOnUiThread(() -> indoor_temp_show.setText(result));
                            }

                            if (isTempSet) {
                                if (currTemp <= tempMin[0]) {
                                    runAsync("python TurnOnHeater.py")
                                            .thenAccept(res -> setDeviceStatus(indoor_heater_show, heatToggle, true))
                                            .exceptionally(e -> {
                                                e.printStackTrace();
                                                return null;
                                            });
                                }
                                if (currTemp >= tempMax[0]) {
                                    runAsync("python TurnOffHeater.py")
                                            .thenAccept(res -> {
                                                isTempSet = false;
                                                setDeviceStatus(indoor_heater_show, heatToggle, false);
                                                tempInputMin.setHint(R.string.txv_default_hint);
                                                tempInputMax.setHint(R.string.txv_default_hint);
                                            }).exceptionally(e -> {
                                                e.printStackTrace();
                                                return null;
                                            });
                                }
                            }

                            handler.postDelayed(this, INTERVAL);
                        }).exceptionally(e -> {
                            e.printStackTrace();
                            return null;
                        });
            }
        }.run();

        // get light status and display on UI
        updateDeviceStatus(outdoor_light_show, lightToggle);
        updateDeviceStatus(indoor_heater_show, heatToggle);

        // run scripts under certain conditions and change UI on toggle
        CompoundButton.OnCheckedChangeListener checkListener = (buttonView, isChecked) -> {
            boolean isLight = getResources().getResourceName(buttonView.getId()).contains("light");
            runAsync("python Turn" + (isChecked ? "On" : "Off") + (isLight ? "Lights" : "Heater") + ".py")
                    .thenAccept(result -> setDeviceStatus((isLight ? outdoor_light_show : indoor_heater_show),
                            (isLight ? lightToggle : heatToggle),
                            isChecked))
                    .exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    });
        };
        lightToggle.setOnCheckedChangeListener(checkListener);
        heatToggle.setOnCheckedChangeListener(checkListener);

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

            handler.postDelayed(() -> runAsync("python TurnOnLights.py")
                    .thenAccept(result -> setDeviceStatus(outdoor_light_show, lightToggle, true))
                    .exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    }), timeInMillisFrom - currentTime);

            handler.postDelayed(() -> runAsync("python TurnOffLights.py")
                    .thenAccept(result -> setDeviceStatus(outdoor_light_show, lightToggle, false))
                    .exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    }), timeInMillisTo - currentTime);

            showToast(btnConfirmTime);
        });

        // limit the decimal places of the input temperature to 1
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String input = editable.toString();
                int decimalPointIndex = input.indexOf(".");
                if (decimalPointIndex != -1 && input.length() - decimalPointIndex > 2) {
                    editable.delete(decimalPointIndex + 2, input.length());
                }
            }
        };
        tempInputMin.addTextChangedListener(textWatcher);
        tempInputMax.addTextChangedListener(textWatcher);

        // get input temperature on click, display the input value in hint
        btnConfirmTemp.setOnClickListener(view -> {
            tempMin[0] = Float.parseFloat(tempInputMin.getText().toString());
            tempMax[0] = Float.parseFloat(tempInputMax.getText().toString());
            isTempSet = true;
            showToast(btnConfirmTemp);
            runOnUiThread(() -> {
                tempInputMin.setText("");
                tempInputMax.setText("");
                tempInputMin.setHint(Float.toString(tempMin[0]));
                tempInputMax.setHint(Float.toString(tempMax[0]));
            });
        });
    }

    // change status UI without running update script
    private void setDeviceStatus(TextView statusText, SwitchCompat toggle, boolean isOn) {
        runOnUiThread(() -> {
            statusText.setText((isOn ? R.string.txv_on : R.string.txv_off));
            statusText.setTextColor(ContextCompat.getColor(this, (isOn ? R.color.teal_700 : R.color.title_bar_color)));
            if (isOn != toggle.isChecked()) {
                toggle.setChecked(isOn);
            }
        });
    }

    // get data from running scripts and update status UI
    private void updateDeviceStatus(TextView statusText, SwitchCompat toggle) {
        runAsync("python Send" + (statusText == outdoor_light_show ? "Lights" : "Heater") + "Status.py")
                .thenApply(result -> {
                    setDeviceStatus(statusText, toggle, result.equals("True"));
                    return null;
                }).exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    // show toast on buttons click
    private void showToast(Button btn) {
        Toast.makeText(this,
                        (btn == btnConfirmTime ? "Schedule" : "Temperature") + " set!",
                        Toast.LENGTH_SHORT)
                .show();
    }

    // async function for scripts execution through SSH connection
    private CompletableFuture<String> runAsync(String command) {
        return CompletableFuture.supplyAsync(() -> {
            String hostname = "4.tcp.eu.ngrok.io";
            int port = 11617;
            String username = "pi";
            String password = "pi";
            StringBuilder result = new StringBuilder();
            try {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                Connection conn = new Connection(hostname, port); //init connection
                conn.connect(); //start connection to the hostname
                boolean isAuthenticated = conn.authenticateWithPassword(username, password);
                if (!isAuthenticated)
                    throw new IOException("Authentication failed.");
                Session sess = conn.openSession();
                sess.execCommand(command);
                InputStream stdout = new StreamGobbler(sess.getStdout());
                BufferedReader br = new BufferedReader(new InputStreamReader(stdout));//reads text
                String line;
                while ((line = br.readLine()) != null) {
                    result.append(line);
                }
                /* Show exit status, if available (otherwise "null") */
                System.out.println("ExitCode: " + sess.getExitStatus());
                sess.close(); // Close this session
                conn.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
                System.exit(2);
            }
            return result.toString();
        });
    }
}