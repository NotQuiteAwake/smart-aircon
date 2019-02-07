package com.ota.jimmychen.ota;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.List;

// Implement person_id: retrieve info from Intent
public class EditMemberActivity extends Activity {
    private static final int PORT_NUMBER = 8080;
    private BarChart expChart = null;
    private BarData expData = null;
    private ArrayList<BarEntry> yValues = new ArrayList<>();

    private Networking network = new Networking(PORT_NUMBER);
    private String ip_address = null, person_id = "default";
    private List<Double> exp_list = new ArrayList<>();
    private Thread get_exp_thread = null;

    private static final int TEAL_COLOR = Color.parseColor("#008080");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editmember);

        expChart = (BarChart)findViewById(R.id.bar_chart);

        Intent intent = getIntent();
        ip_address = intent.getStringExtra("ip_address");
        person_id = intent.getStringExtra("person_id");

        // TODO: check HTTP:// & check setIp()
        network.setIp(ip_address);
        new Thread(new Runnable() {
            @Override
            public void run() {
                getTemp(person_id);
                initData((ArrayList<Double>) exp_list);
                initBarChart();
            }
        }).start();
    }

    // TODO: Put those methods into the Networking class to improve reusability.
    private void getTemp(final String person_id) {
        if (get_exp_thread != null) { get_exp_thread.interrupt(); }
        get_exp_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                exp_list = network.getExpTemp(person_id);
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
        expData = new BarData(barDataSet);
        float barWidth = 0.45f;
        expData.setBarWidth(barWidth);
    }

    private void initBarChart() {
        if (exp_list.size() == 0) return;
        expChart.setData(expData);
        expChart.setScaleEnabled(true);

        //XAxis Settings
        XAxis xAxis = expChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(10f);
        xAxis.setAxisMinimum(0f);
        YAxis RAxis = expChart.getAxisRight();
        RAxis.setEnabled(false);

        YAxis LAxis = expChart.getAxisLeft();
        RAxis.setDrawAxisLine(false);
        LAxis.setDrawAxisLine(false);
        LAxis.setDrawAxisLine(false);
        LAxis.setDrawGridLines(false);
        LAxis.setAxisMinimum(10f);
        LAxis.setAxisMaximum(30f);

        expChart.setScaleEnabled(false);
        expChart.setScaleXEnabled(true);
        expChart.setScaleYEnabled(false);
        expChart.setDoubleTapToZoomEnabled(false);

        expChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                final int index = (int)e.getX();
                Log.i("expChart.OnChartValueSelected", "index = " + index);
                final EditText et = new EditText(EditMemberActivity.this);
                et.setText(String.valueOf(exp_list.get(index)));
                new AlertDialog.Builder(EditMemberActivity.this)
                        .setTitle("Modify Expected Temperature:")
                        .setIcon(android.R.drawable.sym_def_app_icon)
                        .setView(et)
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final double exp_temp = Double.valueOf(et.getText().toString());
                                exp_list.set(index, exp_temp);
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // TODO: implement person_id
                                        network.setExpTemp(person_id, index, exp_temp);
                                    }
                                }).start();
                                yValues.set(index, new BarEntry(index, double_to_float(exp_temp)));
                                expChart.notifyDataSetChanged();
                                expChart.invalidate();
                            }
                        })
                        .setNegativeButton("Cancel", null).show();
            }

            @Override
            public void onNothingSelected() { }
        });
        expChart.notifyDataSetChanged();
        expChart.invalidate();
    }
}
