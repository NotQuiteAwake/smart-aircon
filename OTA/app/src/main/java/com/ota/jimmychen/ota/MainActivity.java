package com.ota.jimmychen.ota;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String ACTIVITY_TAG=MainActivity.class.getSimpleName();
    private String ip_address = "", pref = "";
    private static final int PORT_NUMBER = 8080;
    private boolean isInitialized = false;
    private boolean active_before_restore = false;

    private TextView info_tv;
    private Button set_ip, set_param, view_temp, connect, set_pref, set_prio;

    private Networking network = new Networking(PORT_NUMBER);

    private Thread data_proc_thread = null;
    private Thread task_post_thread = null;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (active_before_restore) {
            dataProc(ip_address);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (data_proc_thread != null) { data_proc_thread.interrupt(); active_before_restore = true; }
        if (task_post_thread != null) { task_post_thread.interrupt(); active_before_restore = true; }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (data_proc_thread != null) data_proc_thread.interrupt();
        if (task_post_thread != null) task_post_thread.interrupt();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        info_tv = (TextView)findViewById(R.id.info_tv);
        set_ip = (Button)findViewById(R.id.set_ip);
        set_param = (Button)findViewById(R.id.manual_set);
        view_temp = (Button)findViewById(R.id.view_temp);
        connect = (Button)findViewById(R.id.connect_button);
        set_pref = (Button)findViewById(R.id.set_pref);
        set_prio = (Button)findViewById(R.id.set_prio);

        info_tv.setMovementMethod(ScrollingMovementMethod.getInstance());
        requestPermissions(new String[]{Manifest.permission.INTERNET}, 0);

        set_prio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, MemberManagerActivity.class);
                intent.putExtra("ip_address", ip_address);
                startActivity(intent);
            }
        });

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("connect", ip_address);
                dataProc(ip_address);
            }
        });

        set_ip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText et = new EditText(MainActivity.this);
                et.setText(ip_address);
                new AlertDialog.Builder(MainActivity.this).setTitle("IP:")
                        .setIcon(android.R.drawable.sym_def_app_icon)
                        .setView(et)
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // TODO: There are usages of ip_address that didn't come with http://
                                ip_address = "http://" + et.getText().toString();
                                network.setIp(ip_address);
                            }
                        })
                        .setNegativeButton("Cancel", null).show();
                // TODO: Find better methods for waiting. Candidate: jaredrummler/BlockingDialog
            }
        });

        set_param.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ip_address == null) { Toast.makeText(MainActivity.this, "Not Connected.", Toast.LENGTH_SHORT).show(); }
                else {
                    final EditText et = new EditText(MainActivity.this);
                    new AlertDialog.Builder(MainActivity.this).setTitle("Next Time")
                            .setIcon(android.R.drawable.sym_def_app_icon)
                            .setView(et)
                            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    pref = et.getText().toString();
                                    predictAlter(ip_address, pref);
                                    Log.i("set_param", "Request Made");
                                }
                            })
                            .setNegativeButton("Cancel", null).show();
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

        set_pref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, EditExpActivity.class);
                intent.putExtra("ip_address", ip_address);
                startActivity(intent);
            }
        });
        scan();
    }

    public void predictAlter(final String ip_address, final String pref) {
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
                        double exp_time = Double.parseDouble(pref);
                        int status = network.setExpTime(exp_time);
                        if (status == -1) { Log.e(ACTIVITY_TAG, "setExpTime error"); }
                    }
                });
                task_post_thread.start();
            }
        }).start();
    }

    private void updateInitState(String ip_address) {
        isInitialized = network.getInitState();
    }

    public void scan() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NetworkScanner scanner = new NetworkScanner();
                scanner.scan();
                String ip = scanner.getAcAddress();
                if (ip != null) ip_address = ip;
                Log.i(ACTIVITY_TAG, "Scan complete. Address is " + ip_address);
            }
        }).start();
    }

    public void dataProc(final String ip_address) {
        if (data_proc_thread != null) {
            Toast.makeText(MainActivity.this, "Thread exists. Restarting.", Toast.LENGTH_SHORT).show();
            data_proc_thread.interrupt();
        }
        data_proc_thread = new Thread() {
            @Override
            public void run() {
                isInitialized = false;
                while (!isInitialized) {
                    updateInitState(ip_address);
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
                    if (Thread.currentThread().isInterrupted()) { return; }
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        Log.i(ACTIVITY_TAG, "dataProc interrupted during sleep()");
                        e.printStackTrace();
                        return;
                    }
                    String output = network.getStat();
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
