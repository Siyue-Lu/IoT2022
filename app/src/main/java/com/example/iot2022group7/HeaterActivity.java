package com.example.iot2022group7;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
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

public class HeaterActivity extends AppCompatActivity {
    ImageView back1;

    Helper helper = new Helper();
    Handler handler = helper.getHandler();

    TextView indoor_temp_show = null;
    TextView indoor_heater_show = null;
    SwitchCompat heatToggle;
    EditText tempInputMin = null;
    EditText tempInputMax = null;
    Button btnConfirmTemp = null;
    boolean isTempSet = false;

    private static final int INTERVAL = 5000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heater);
        getSupportActionBar().hide();
        back1 = (ImageView) findViewById(R.id.imageView1);
        back1.setOnClickListener(view -> startActivity(new Intent(HeaterActivity.this, MainActivity.class)));

        indoor_temp_show = (TextView) findViewById(R.id.indoorTempShow);
        indoor_heater_show = (TextView) findViewById(R.id.indoorHeaterShow);
        heatToggle = (SwitchCompat) findViewById(R.id.heatToggle);
        tempInputMin = (EditText) findViewById(R.id.temp_min);
        tempInputMax = (EditText) findViewById(R.id.temp_max);
        btnConfirmTemp = (Button) findViewById(R.id.btnConfirmTemp);

        final float[] tempMin = new float[1];
        final float[] tempMax = new float[1];

        // get temperature periodically, display on UI and run scripts under certain conditions
        new Runnable() {
            @Override
            public void run() {
                helper.runAsync("python SendTemperature.py")
                        .thenAccept(result -> {
                            String currTempStr = indoor_temp_show.getText().toString();
                            float currTemp = Float.parseFloat(result);

                            if (!currTempStr.equals(result)) {
                                runOnUiThread(() -> indoor_temp_show.setText(result));
                            }

                            if (isTempSet) {
                                if (currTemp <= tempMin[0]) {
                                    helper.runAsync("python TurnOnHeater.py")
                                            .thenAccept(res -> helper.setDeviceStatus(indoor_heater_show, heatToggle, true))
                                            .exceptionally(e -> {
                                                e.printStackTrace();
                                                return null;
                                            });
                                }
                                if (currTemp >= tempMax[0]) {
                                    helper.runAsync("python TurnOffHeater.py")
                                            .thenAccept(res -> {
                                                isTempSet = false;
                                                helper.setDeviceStatus(indoor_heater_show, heatToggle, false);
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

        helper.updateDeviceStatus("Heater", indoor_heater_show, heatToggle);

        if (heatToggle != null) {
            heatToggle.setOnCheckedChangeListener(helper.getCheckListener("Heater", indoor_heater_show, heatToggle));
        }

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
            helper.showToast(this, "Temperature");
            runOnUiThread(() -> {
                tempInputMin.setText("");
                tempInputMax.setText("");
                tempInputMin.setHint(Float.toString(tempMin[0]));
                tempInputMax.setHint(Float.toString(tempMax[0]));
            });
        });
    }
}