package com.ota.jimmychen.ota;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SetPriorityActivity extends Activity {
    private static String ip_address;
    private static final int PORT_NUMBER = 8080;
    private Networking network = new Networking(PORT_NUMBER);

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setpriority);

        Intent intent = getIntent();
        if (intent.getStringExtra("ip_address") != null) ip_address = intent.getStringExtra("ip_address");

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new listAdapter((List<String>)new ArrayList<String>() {{
            add("a");
            add("b");
            add("c");
            add("d");
        }});
        mRecyclerView.setAdapter(mAdapter);
    }
}
