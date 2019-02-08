package com.ota.jimmychen.ota;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;

public class ViewTempActivity extends AppCompatActivity {
    private BarChart mTempChart = null;
    private BarData mTempData = null;
    private final static int PORT_NUMBER = 8080;

    private Networking network = new Networking(PORT_NUMBER);
    private String ip_address = null;
    private List<Double> mTempList = new ArrayList<>();
    private Thread get_temp_thread = null;
    private static final int TEAL_COLOR = Color.parseColor("#008080");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewtemp);

        mTempChart = (BarChart)findViewById(R.id.bar_chart);

        Intent intent = getIntent();
        ip_address = intent.getStringExtra("ip_address");

        new Thread(new Runnable() {
            @Override
            public void run() {
                getTemp();
                initData((ArrayList<Double>) mTempList);
                initBarChart();
            }
        }).start();
    }

    private void getTemp() {
        if (get_temp_thread != null) { get_temp_thread.interrupt(); }
        get_temp_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                mTempList = network.getTemp();
            }
        });
        get_temp_thread.run();
    }

    private float double_to_float(double d) {
        return Float.valueOf(String.valueOf(d));
    }

    private void initData(ArrayList<Double> data) {
        ArrayList<BarEntry> yValues = new ArrayList<>();
        for (int x = 0; x < data.size(); x++) {
            yValues.add(new BarEntry(x, double_to_float(data.get(x))));
        }
        BarDataSet barDataSet = new BarDataSet(yValues, "Temperature Record");
        barDataSet.setColor(TEAL_COLOR);
        mTempData = new BarData(barDataSet);
    }

    private void initBarChart() {
        if (mTempList.size() == 0) return;
        float barWidth = 0.45f;
        mTempData.setBarWidth(barWidth);
        mTempChart.setData(mTempData);
        mTempChart.setScaleEnabled(true);

        XAxis xAxis = mTempChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(10f);
        xAxis.setAxisMinimum(0);

        YAxis RAxis = mTempChart.getAxisRight();
        RAxis.setEnabled(false);

        YAxis LAxis = mTempChart.getAxisLeft();
        RAxis.setDrawAxisLine(false);
        LAxis.setDrawAxisLine(false);
        LAxis.setDrawAxisLine(false);
        LAxis.setDrawGridLines(false);
        LAxis.setAxisMinimum((float)getMinData());
        LAxis.setAxisMaximum((float)getMaxData());

        mTempChart.setScaleEnabled(false);
        mTempChart.setScaleXEnabled(true);
        mTempChart.setScaleYEnabled(false);
        mTempChart.setDoubleTapToZoomEnabled(false);
        mTempChart.notifyDataSetChanged();
        mTempChart.invalidate();
    }

    private double getMaxData() {
        double max = -100;
        for (int i = 0; i < mTempList.size(); i++) {
            max = Math.max(max, mTempList.get(i));
        }
        return max + 1;
    }

    private double getMinData() {
        double min = 100;
        for (int i = 0; i < mTempList.size(); i++) {
            min = Math.min(min, mTempList.get(i));
        }
        return min - 1;
    }

}
