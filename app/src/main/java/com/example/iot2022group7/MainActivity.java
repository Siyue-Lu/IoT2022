package com.example.iot2022group7;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class MainActivity extends AppCompatActivity {

    TextView txv_temp_indoor = null;
    Switch lightToggle = null;
    Button btnUpdateTemp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        txv_temp_indoor = (TextView) findViewById(R.id.indoorTempShow);
        txv_temp_indoor.setText("the fetched indoor temp value");

        lightToggle = (Switch) findViewById(R.id.btnToggle);
        lightToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                } else {

                }}
        });

        btnUpdateTemp = (Button) findViewById(R.id.btnUpdateTemp);
        btnUpdateTemp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

            }
        });
    }
}