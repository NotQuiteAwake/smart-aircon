package com.ota.jimmychen.ota;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String ACTIVITY_TAG="MainActivity";
    private String ip_address = "";
    boolean isInitialized = false;
    boolean active_before_restore = false;

    TextView info_tv;
    Button set_ip, set_param, view_temp;
    EditText ipInput, man_param;

    Networking network = new Networking();

    Thread data_proc_thread = null;
    Thread task_post_thread = null;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (active_before_restore) {
            data_proc(ip_address);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (data_proc_thread != null) { data_proc_thread.interrupt(); active_before_restore = true; }
        if (task_post_thread != null) { task_post_thread.interrupt(); active_before_restore = true; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        info_tv = (TextView)findViewById(R.id.info_tv);
        set_ip = (Button)findViewById(R.id.set_ip);
        ipInput = (EditText)findViewById(R.id.ip_input);
        set_param = (Button)findViewById(R.id.manual_set);
        man_param = (EditText)findViewById(R.id.man_param);
        view_temp = (Button)findViewById(R.id.view_temp);

        info_tv.setMovementMethod(ScrollingMovementMethod.getInstance());
        requestPermissions(new String[]{Manifest.permission.INTERNET}, 0);

        set_ip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ip_address = "http://" + ipInput.getText().toString();
                data_proc(ip_address);
                Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
            }
        });

        set_param.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ip_address == null) { Toast.makeText(MainActivity.this, "Not Connected.", Toast.LENGTH_SHORT).show(); }
                else {
                    String pref = man_param.getText().toString();
                    predict_alter(ip_address, pref);
                    Toast.makeText(MainActivity.this, "Request Made.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // TODO: 2019/2/3  Add precautions for ip_address unavailable problem

        view_temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, ViewTempActivity.class);
                intent.putExtra("ip_address", ip_address);
                startActivity(intent);
            }
        });
    }

    public void predict_alter(final String ip_address, final String pref) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (task_post_thread != null) {
                    try {
                        task_post_thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                task_post_thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject req = new JSONObject();
                        double exp_time = Double.parseDouble(pref.substring(0, pref.indexOf(":")));
                        double exp_temp = Double.parseDouble(pref.substring(pref.indexOf(":") + 1, pref.length()));
                        try {
                            req.put("cmd", "modify_exp");
                            req.put("exp_time", exp_time);
                            req.put("exp_temp", exp_temp);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        network.post_request(ip_address, req);
                        String res = network.get_data(ip_address);
                        try {
                            JSONObject json_res = new JSONObject(res);
                            int status = json_res.getInt("status");

                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                });
                task_post_thread.start();
            }
        }).start();
    }

    private void update_init_state(String ip_address) {
        try {
            JSONObject req = new JSONObject();
            req.put("cmd", "check_init_state");
            network.post_request(ip_address, req);
            JSONObject response = new JSONObject(network.get_data(ip_address));
            if (response.getInt("status") == -1) { Log.e(MainActivity.ACTIVITY_TAG, "update_init_state failed to fetch state."); }
            else {
                int init_state_int = response.getInt("init_state");
                if (init_state_int > 0) { isInitialized = true; }
                else isInitialized = false;
            }
        } catch (JSONException e) { e.printStackTrace(); }

    }

    public void data_proc(final String ip_address) {
        if (data_proc_thread != null) {
            Toast.makeText(MainActivity.this, "Thread exists. Restarting.", Toast.LENGTH_SHORT).show();
            data_proc_thread.interrupt();
        }
        data_proc_thread = new Thread() {
            @Override
            public void run() {
                isInitialized = false;
                while (!isInitialized) {
                    update_init_state(ip_address);
                    if (Thread.currentThread().isInterrupted()) { return; }
                    info_tv.setText("The connection has been established.\nHowever, initialization is not yet done.");
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
                while (true) {
                    String output = "";
                    if (Thread.currentThread().isInterrupted()) { return; }
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                    JSONObject json = new JSONObject();
                    try {
                        json.put("cmd", "data_req");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    network.post_request(ip_address, json);
                    String res = network.get_data(ip_address);
                    try {
                        JSONObject json_rep = new JSONObject(res);
                        int status = json_rep.getInt("status");
                        if (status == 1) {
                            List<String> temp = network.json_to_array(json_rep.getJSONArray("temp"));
                            List<String> exp = network.json_to_array(json_rep.getJSONArray("exp"));
                            int time = json_rep.getInt("time");
                            int p_time = json_rep.getInt("p_time");
                            output = "";
                            output = output + "Current time:\n" + time + "\n";
                            output = output + "Temperature records:\n" + String.join(",", temp) + "\n";
                            output = output + "Expected Temperature:\n" + String.join(",", exp) + "\n";
                            output = output + "Next predicted turn-on time:\n" + p_time + "\n";
                        } else if (status == -1) {
                            output = "Fetch failed: " + status;
                            Log.e(MainActivity.ACTIVITY_TAG + " data_proc", "status = -1");
                        }
                    } catch (JSONException e){ e.printStackTrace(); /*I guess that I forgive you*/ }
                    info_tv.setText(output);
                }
            }
        };
        data_proc_thread.start();
        Toast.makeText(MainActivity.this, "Thread started.", Toast.LENGTH_SHORT).show();
    }

    public static boolean hasPermission(Context context) {
        int result = ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
