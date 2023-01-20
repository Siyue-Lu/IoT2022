package com.example.iot2022group7;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.StrictMode;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class Helper extends AppCompatActivity {

    private final Handler handler = new Handler();

    protected Handler getHandler() {
        return handler;
    }

    // run scripts under certain conditions and change UI on toggle
    protected CompoundButton.OnCheckedChangeListener getCheckListener(String device, TextView statusText, SwitchCompat toggle) {
        return (buttonView, isChecked) -> runAsync("python Turn" + (isChecked ? "On" : "Off") + device + ".py")
                .thenAccept(result -> setDeviceStatus(statusText, toggle, isChecked))
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }


    // change status UI without running update script
    protected void setDeviceStatus(TextView statusText, SwitchCompat toggle, boolean isOn) {
        runOnUiThread(() -> {
            statusText.setText((isOn ? R.string.txv_on : R.string.txv_off));
            statusText.setTextColor(ContextCompat.getColor(statusText.getContext(), (isOn ? R.color.teal_700 : R.color.title_bar_color)));
            if (isOn != toggle.isChecked()) {
                toggle.setChecked(isOn);
            }
        });
    }

    // get data from running scripts and update status UI
    protected void updateDeviceStatus(String device, TextView statusText, SwitchCompat toggle) {
        runAsync("python Send" + device + "Status.py")
                .thenApply(result -> {
                    setDeviceStatus(statusText, toggle, result.equals("True"));
                    return null;
                }).exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    // show toast on buttons click
    protected void showToast(Context context, String s) {
        Toast.makeText(context, s + " set!", Toast.LENGTH_SHORT).show();
    }

    // async function for scripts execution through SSH connection
    protected CompletableFuture<String> runAsync(String command) {
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