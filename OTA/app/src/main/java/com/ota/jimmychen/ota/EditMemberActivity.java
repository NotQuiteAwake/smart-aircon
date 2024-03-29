package com.ota.jimmychen.ota;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// Implement person_id: retrieve info from Intent
public class EditMemberActivity extends Activity {
    private static final int PORT_NUMBER = 8080;
    private BarChart mExpChart = null;
    private BarData mExpData = null;
    private ArrayList<BarEntry> yValues = new ArrayList<>();

    private Networking network = new Networking(PORT_NUMBER);
    private String ip_address = null, person_id = "default", state_id = "State", name = "Name";
    private List<Double> mExpList = new ArrayList<>();
    private Thread get_exp_thread = null;
    private List<String> mStateList = new ArrayList<>();

    private int priority, presence;

    private static final int TEAL_COLOR = Color.parseColor("#008080");

    private Button set_priority, set_state;
    private TextView priority_tv, name_tv, state_tv;
    private Switch presence_switch;

    private class ChoiceOnClickListener implements DialogInterface.OnClickListener {
        private int position = 0;

        @Override
        public void onClick(DialogInterface dialogInterface, int position) {
            this.position = position;
        }

        public int getPosition() {
            return this.position;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editmember);

        mExpChart = (BarChart)findViewById(R.id.bar_chart);
        set_priority = (Button) findViewById(R.id.set_priority_bt);
        set_state = (Button) findViewById(R.id.set_state_bt);
        priority_tv = (TextView) findViewById(R.id.priority);
        name_tv = (TextView) findViewById(R.id.name);
        state_tv = (TextView) findViewById(R.id.state);
        presence_switch = (Switch) findViewById(R.id.presence_switch);

        Intent intent = getIntent();
        ip_address = intent.getStringExtra("ip_address");
        person_id = intent.getStringExtra("person_id");

        //TODO: states should also define the time length it takes for the air con to take it to the desired temp.
        network.setIp(ip_address);

        new Thread(new Runnable() {
            @Override
            public void run() {
                //TODO: Add respective methods!
                try {
                    JSONObject json = new JSONObject(network.getUserJson(person_id));
                    name = person_id;
                    priority = json.getInt("priority");
                    //TODO: Check presence & state_id
                    presence = json.getInt("presence");
                    state_id = json.getString("state_id");
                    mStateList = network.getStateList();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            name_tv.setText(name);
                            priority_tv.setText("Priority: " + priority);
                            state_tv.setText("State: " + state_id);
                            presence_switch.setChecked((presence > 0));
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                getTemp(person_id);
                initData((ArrayList<Double>) mExpList);
                initBarChart();
            }
            //TODO: add a name for each Thread
        }, "initParamThread").start();

        set_priority.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText et = new EditText(EditMemberActivity.this);

                new AlertDialog.Builder(EditMemberActivity.this)
                        .setTitle("Set Priority")
                        .setIcon(android.R.drawable.sym_def_app_icon)
                        .setView(et)
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // TODO: implement priority edition
                                priority = Integer.parseInt(et.getText().toString());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        priority_tv.setText("Priority: " + priority);
                                    }
                                });

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        network.setPriority(person_id, priority);
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        //TODO: use an EditText?
        name_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText et = new EditText(EditMemberActivity.this);

                new AlertDialog.Builder(EditMemberActivity.this)
                        .setTitle("Change Your Name")
                        .setIcon(android.R.drawable.sym_def_app_icon)
                        .setView(et)
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                name = et.getText().toString();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        name_tv.setText(name);
                                    }
                                });

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        network.setName(person_id, name);
                                        // TODO: Consider better methods
                                        person_id = name;
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        // TODO: Fix bug where presence don't automatically change and "person_id":""
        presence_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, final boolean isChecked) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        network.setPresence(person_id, isChecked);
                    }
                }).start();
            }
        });

        set_state.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final ChoiceOnClickListener choiceOnClickListener = new ChoiceOnClickListener();
                new AlertDialog.Builder(EditMemberActivity.this)
                        .setTitle("Set Your State")
                        .setIcon(android.R.drawable.sym_def_app_icon)
                        .setSingleChoiceItems(mStateList.toArray(new String[mStateList.size()]), 0, choiceOnClickListener)
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int position = choiceOnClickListener.getPosition();
                                state_id = mStateList.get(position);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        state_tv.setText("State: " + state_id);
                                    }
                                });
                                new Thread(new Runnable() {
                                    //TODO: add a StateManagerActivity!
                                    @Override
                                    public void run() {
                                        network.setState(person_id, state_id);
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final int position = choiceOnClickListener.getPosition();
                                final String removed_id = mStateList.remove(position);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        state_tv.setText("State: " + mStateList.get(0));
                                    }
                                });

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        network.removeState(removed_id);
                                        network.setState(person_id, mStateList.get(0));
                                    }
                                }).start();
                            }
                        })
                        .setNeutralButton("Add", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final View addStateView = LayoutInflater.from(EditMemberActivity.this).inflate(R.layout.add_state, null);
                                new AlertDialog.Builder(EditMemberActivity.this)
                                        .setView(addStateView)
                                        .setTitle("Set Param:")
                                        .setIcon(android.R.drawable.sym_def_app_icon)
                                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                EditText state_id_et = addStateView.findViewById(R.id.state_id_et);
                                                EditText temp_diff_et = addStateView.findViewById(R.id.temp_diff_et);
                                                final String new_id = state_id_et.getText().toString();
                                                mStateList.add(new_id);
                                                final int temp_diff = Integer.parseInt(temp_diff_et.getText().toString());
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        state_tv.setText("State: " + new_id);
                                                    }
                                                });
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        network.addState(new_id, temp_diff);
                                                        network.setState(person_id, new_id);
                                                    }
                                                }).start();
                                            }
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();
                            }
                        })
                        .show();
            }
        });

    }

    // TODO: Put those methods into the Networking class to improve usability.
    private void getTemp(final String person_id) {
        if (get_exp_thread != null) { get_exp_thread.interrupt(); }
        get_exp_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                mExpList = network.getExpTemp(person_id);
            }
        });
        get_exp_thread.run();
    }

    private float double_to_float(double d) {
        return Float.valueOf(String.valueOf(d));
    }

    private void initData(ArrayList<Double> data) {
        for (int x = 0; x < data.size(); x++) {
            yValues.add(new BarEntry(x, double_to_float(data.get(x))));
        }
        BarDataSet barDataSet = new BarDataSet(yValues, "Expected Temperature");
        barDataSet.setColor(TEAL_COLOR);
        barDataSet.setDrawValues(false);
        mExpData = new BarData(barDataSet);
        float barWidth = 0.45f;
        mExpData.setBarWidth(barWidth);
    }

    private void initBarChart() {
        if (mExpList.size() == 0) return;
        mExpChart.setData(mExpData);
        mExpChart.setScaleEnabled(true);

        mExpChart.getDescription().setEnabled(false);
        //XAxis Settings
        XAxis xAxis = mExpChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(10f);
        xAxis.setAxisMinimum(0f);
        YAxis RAxis = mExpChart.getAxisRight();
        RAxis.setEnabled(false);

        YAxis LAxis = mExpChart.getAxisLeft();
        RAxis.setDrawAxisLine(false);
        LAxis.setDrawAxisLine(false);
        LAxis.setDrawAxisLine(false);
        LAxis.setDrawGridLines(false);
        LAxis.setAxisMinimum(10f);
        LAxis.setAxisMaximum(30f);

        mExpChart.setScaleEnabled(false);
        mExpChart.setScaleXEnabled(true);
        mExpChart.setScaleYEnabled(false);
        mExpChart.setDoubleTapToZoomEnabled(false);

        mExpChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                final int index = (int)e.getX();
                Log.i("mExpChart.OnChartValueSelected", "index = " + index);
                final EditText et = new EditText(EditMemberActivity.this);
                et.setText(String.valueOf(mExpList.get(index)));
                new AlertDialog.Builder(EditMemberActivity.this)
                        .setTitle("Modify Expected Temperature:")
                        .setIcon(android.R.drawable.sym_def_app_icon)
                        .setView(et)
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final double exp_temp = Double.valueOf(et.getText().toString());
                                mExpList.set(index, exp_temp);
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // TODO: implement person_id
                                        network.setExpTemp(person_id, index, exp_temp);
                                    }
                                }).start();
                                yValues.set(index, new BarEntry(index, double_to_float(exp_temp)));
                                mExpChart.notifyDataSetChanged();
                                mExpChart.invalidate();
                            }
                        })
                        .setNegativeButton("Cancel", null).show();
            }

            @Override
            public void onNothingSelected() { }
        });
        mExpChart.notifyDataSetChanged();
        mExpChart.invalidate();
    }
}
