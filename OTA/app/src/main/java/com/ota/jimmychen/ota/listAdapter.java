package com.ota.jimmychen.ota;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class listAdapter extends RecyclerView.Adapter<listAdapter.ListViewHolder> {
    private List<String> mDataset = new ArrayList<>();
    private static final int PORT_NUMBER = 8080;
    private static String ip_address = "";
    private Networking mNetwork = null;
    private static final String CLASS_TAG = "listAdapter";

    // TODO: implement a CardView to hold all the information of the user.
    public class ListViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;

        public ListViewHolder(final TextView v) {
            super(v);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    Log.e(CLASS_TAG, "onClick() event at position " + position);
                    String person_id = mDataset.get(position);
                    Intent intent = new Intent();
                    intent.putExtra("ip_address", ip_address);
                    intent.putExtra("person_id", person_id);
                    Activity CurrentActivity = (Activity) v.getContext();
                    intent.setClass(CurrentActivity, EditMemberActivity.class);
                    CurrentActivity.startActivity(intent);
                }
            });
            mTextView = v;
        }
    }

    public listAdapter(List<String> dataSet, Networking network) {
        mDataset = dataSet;
        mNetwork = network;
        ip_address = network.getIp();
    }

    @Override

    public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final TextView v = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_base, parent, false);

        ListViewHolder vh = new ListViewHolder(v);

        return vh;
    }

    public void onBindViewHolder(ListViewHolder holder, final int position) {
        holder.mTextView.setText(mDataset.get(position));
    }

    public int getItemCount() {
        return mDataset.size();
    }

    public void add(int position, String str) {
        mDataset.add(position, str);
        notifyItemInserted(position);
    }

    public void remove(int position) {
        mDataset.remove(position);
        notifyItemRemoved(position);
        notifyDataSetChanged();
    }
}
