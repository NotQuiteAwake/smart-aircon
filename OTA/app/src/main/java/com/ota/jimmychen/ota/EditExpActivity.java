package com.ota.jimmychen.ota;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class EditExpActivity extends Activity {
    private BarChart expChart = null;
    private BarData expData = null;

    private Networking network = new Networking();
    private String ip_address = null;
    private List<Double> exp_list = new ArrayList<>();
    private Thread get_exp_thread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editexp);

        expChart = (BarChart)findViewById(R.id.bar_chart);

        Intent intent = getIntent();
        ip_address = intent.getStringExtra("ip_address");
        new Thread(new Runnable() {
            @Override
            public void run() {
                getTemp();
                initData((ArrayList<Double>) exp_list);
                initBarChart();
            }
        }).start();
    }

    // TODO: Put those methods into the Networking class to improve reusability.
    private void getTemp() {
        if (get_exp_thread != null) { get_exp_thread.interrupt(); }
        get_exp_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject req = new JSONObject();
                try {
                    req.put("cmd", "data_req");
                } catch (JSONException e) { e.printStackTrace(); /* Nujabes is the great producer */ }
                network.post_request(ip_address, req);
                List<String> exp = new ArrayList<>();
                try {
                    JSONObject data = new JSONObject(network.get_data(ip_address));
                    exp = network.json_to_array(data.getJSONArray("exp"));
                } catch (JSONException e) { e.printStackTrace(); }
                if (exp != null) exp_list = network.str_to_double(exp);
            }
        });
        get_exp_thread.run();
    }

    private float double_to_float(double d) {
        return Float.valueOf(String.valueOf(d));
    }

    private void initData(ArrayList<Double> data) {
        ArrayList<BarEntry> yValues = new ArrayList<>();
        for (int x = 0; x < data.size(); x++) {
            yValues.add(new BarEntry(x, double_to_float(data.get(x))));
        }
        BarDataSet barDataSet = new BarDataSet(yValues, "Expected Temperature");
        barDataSet.setColor(Color.BLUE);
        expData = new BarData(barDataSet);
        float barWidth = 0.45f;
        expData.setBarWidth(barWidth);
    }

    private void initBarChart() {
        if (exp_list.size() == 0) return;
        expChart.setData(expData);
        expChart.setScaleEnabled(true);

        XAxis xAxis = expChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(10f);
        xAxis.setAxisMinimum(0);

        YAxis RAxis = expChart.getAxisRight();
        RAxis.setEnabled(false);

        YAxis LAxis = expChart.getAxisLeft();
        RAxis.setDrawAxisLine(false);
        LAxis.setDrawAxisLine(false);
        LAxis.setDrawAxisLine(false);
        LAxis.setDrawGridLines(false);
        LAxis.setAxisMinimum(0f);
        expChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                // TODO: index = -1; Results in an ArrayIndexOutOfBoundsException
                final int index = h.getDataIndex();
                final EditText et = new EditText(EditExpActivity.this);
                et.setText(String.valueOf(exp_list.get(index)));
                new AlertDialog.Builder(EditExpActivity.this)
                        .setTitle("Modify Expected Temperature:")
                        .setIcon(android.R.drawable.sym_def_app_icon)
                        .setView(et)
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                exp_list.set(index, Double.valueOf(et.getText().toString()));
                                removeDataSet();
                                updateDataSet();
                            }
                        })
                        .setNegativeButton("Cancel", null).show();
            }

            @Override
            public void onNothingSelected() { }
        });
    }

    private void updateDataSet() {
        initData((ArrayList<Double>) exp_list);
        expChart.setData(expData);
        expChart.setScaleEnabled(true);
        expChart.notifyDataSetChanged();
        expChart.invalidate();
    }

    private void removeDataSet() {
        BarData barData = expChart.getBarData();
        if (barData != null) {
            while (barData.getDataSetCount() > 0) {
                removeLastEntry();
            }
            expChart.notifyDataSetChanged();
            expChart.invalidate();
        }
    }

    private void removeLastEntry() {
        BarData barData = expChart.getBarData();
        if (barData != null) {
            int index = barData.getDataSetCount() - 1;
            BarDataSet lastDataSet = (BarDataSet) barData.getDataSetByIndex(index);
            if (lastDataSet != null) {
                Entry lastEntry = lastDataSet.getEntryForIndex(
                        lastDataSet.getEntryCount() - 1);
                barData.removeEntry(lastEntry, index);
               }
        }
    }

}
