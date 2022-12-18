package com.example.iot2022group7;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class MainActivity extends AppCompatActivity {

    TextView txv_temp_indoor = null;
    TextView outdoor_light_show = null;
    Switch lightToggle = null;
    Button btnUpdateTemp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        txv_temp_indoor = (TextView) findViewById(R.id.indoorTempShow);
        outdoor_light_show = (TextView) findViewById(R.id.outdoorLightShow);
        lightToggle = (Switch) findViewById(R.id.btnToggle);
        btnUpdateTemp = (Button) findViewById(R.id.btnUpdateTemp);

        // get light status and display in UI


        // run scripts and change UI on toggle
        lightToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    new AsyncTask<Integer, Void, Void>() {
                        @Override
                        protected Void doInBackground(Integer... params) {
                            run("python TurnOnLights.py");
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void v) {
                            outdoor_light_show.setText(R.string.txv_on);
                            outdoor_light_show.setTextColor(getResources().getColor(R.color.teal_700));
                        }
                    }.execute(1);
                } else {
                    new AsyncTask<Integer, Void, Void>() {
                        @Override
                        protected Void doInBackground(Integer... params) {
                            run("python TurnOffLights.py");
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void v) {
                            outdoor_light_show.setText(R.string.txv_off);
                            outdoor_light_show.setTextColor(getResources().getColor(R.color.title_bar_color));
                        }
                    }.execute(1);
                }
            }
        });

        btnUpdateTemp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

            }
        });
    }

    public void run(String command) {
        String hostname = "7.tcp.eu.ngrok.io";
        int port = 14536;
        String username = "pi";
        String password = "pi";
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
            }
            /* Show exit status, if available (otherwise "null") */
            System.out.println("ExitCode: " + sess.getExitStatus());
            sess.close(); // Close this session
            conn.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }

}