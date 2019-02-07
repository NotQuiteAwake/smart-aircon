package com.ota.jimmychen.ota;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Window;

import java.util.List;

public class MemberManagerActivity extends Activity {
    private static String ip_address = "";
    private static final int PORT_NUMBER = 8080;
    private Networking network = new Networking(PORT_NUMBER);

    List<String> member_list;
    private final Handler handler = new Handler();
    final Runnable setMemberAdapterRunnable = new Runnable() {
        @Override
        public void run() {
            mAdapter = new listAdapter(member_list, network);
            mRecyclerView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
        }
    };

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private Thread get_member_thread;

    @Override
    protected void onStop() {
        super.onStop();
        if (get_member_thread != null) get_member_thread.interrupt();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_membermanager);

        Intent intent = getIntent();
        if (intent.getStringExtra("ip_address") != null) ip_address = intent.getStringExtra("ip_address");

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemViewCacheSize(-1);
        setMemberAdapter();
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    // TODO: make sure every server list has a built-in default option
    private void setMemberAdapter() {
        if (get_member_thread != null) get_member_thread.interrupt();
        get_member_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (Thread.currentThread().isInterrupted()) { return; }
                member_list = network.getMemberList();
                handler.post(setMemberAdapterRunnable);
            }
        });
        get_member_thread.start();
    }


}
