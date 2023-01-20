package com.example.iot2022group7;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    CardView bulb;
    CardView heat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        getSupportActionBar().hide();

        bulb = (CardView) findViewById(R.id.cardView1);
        heat=(CardView) findViewById(R.id.cardView2) ;

        bulb.setOnClickListener(v -> startActivity(new Intent(MainActivity.this,LampActivity.class)));
        heat=(CardView) findViewById(R.id.cardView2) ;


        heat.setOnClickListener(v -> startActivity(new Intent(MainActivity.this,HeaterActivity.class)));

    }
}