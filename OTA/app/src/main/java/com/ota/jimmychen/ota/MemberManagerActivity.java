package com.ota.jimmychen.ota;

import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import java.util.List;

public class MemberManagerActivity extends AppCompatActivity {
    private static String ip_address = "";
    private static final int PORT_NUMBER = 8080;
    private Networking network = new Networking(PORT_NUMBER);

    List<String> mMemberList;
    private final Handler handler = new Handler();
    final Runnable setMemberAdapterRunnable = new Runnable() {
        @Override
        public void run() {
            mAdapter = new listAdapter(mMemberList, network);
            mRecyclerView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
        }
    };

    private RecyclerView mRecyclerView;
    private listAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private FloatingActionButton mFAB;

    private Thread get_member_thread;

    @Override
    protected void onResume() {
        super.onResume();
        setMemberAdapter();
    }

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

        mFAB = (FloatingActionButton) findViewById(R.id.fab);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemViewCacheSize(-1);
        setMemberAdapter();
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        mFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Create new person
                final String new_id = "New User";
                mAdapter.add(mAdapter.getDataSize(), "New User");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        network.addUser(new_id);
                        Intent intent = new Intent();
                        intent.setClass(MemberManagerActivity.this, EditMemberActivity.class);
                        intent.putExtra("ip_address", ip_address);
                        intent.putExtra("person_id", new_id);
                        startActivity(intent);
                    }
                }).start();
            }
        });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                final String person_id = mMemberList.get(position);
                mMemberList.remove(position);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (get_member_thread != null) {
                            try {
                                get_member_thread.join();
                            } catch (InterruptedException e) { e.printStackTrace(); }
                        }
                        network.removeUser(person_id);
                    }
                }, "removeUserThread").start();
                mAdapter.notifyItemRemoved(position);
            }

            @Override
            public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    final float alpha = 1 - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
                    viewHolder.itemView.setAlpha(alpha);
                    viewHolder.itemView.setTranslationX(dX);
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

    }

    // TODO: make sure every server list has a built-in default option
    private void setMemberAdapter() {
        if (get_member_thread != null) get_member_thread.interrupt();
        get_member_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (Thread.currentThread().isInterrupted()) { return; }
                mMemberList = network.getMemberList();
                handler.post(setMemberAdapterRunnable);
            }
        }, "getMemberThread");
        get_member_thread.start();
    }


}
