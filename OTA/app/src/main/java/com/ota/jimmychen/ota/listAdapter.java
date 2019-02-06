package com.ota.jimmychen.ota;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class listAdapter extends RecyclerView.Adapter<listAdapter.ListViewHolder> {
    private List<String> mDataset;
    private static final int PORT_NUMBER = 8080;
    private static String ip_address;
    private Networking mNetwork = null;

    // TODO: implement a CardView to hold all the information of the user.
    public static class ListViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        private String mIndex;

        public ListViewHolder(TextView v) {
            super(v);
            mTextView = v;
        }

        public void setIndex(String index) {
            mIndex = index;
        }

        public String getIndex() {
            return mIndex;
        }
    }

    public listAdapter(List<String> dataSet, Networking network) {
        mDataset = dataSet;
        mNetwork = network;
        ip_address = network.getIp();
    }

    @Override
    public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView v = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_base, parent, false);
        ListViewHolder vh = new ListViewHolder(v);
        final String index = vh.getIndex();

        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity CurrentActivity = (Activity) v.getContext();
                Intent intent = new Intent();
                // TODO: expand EditExpActivity into a full managing activity
                intent.setClass(CurrentActivity, EditExpActivity.class);
                intent.putExtra("ip_address", ip_address);
                intent.putExtra("person_id", index);
                CurrentActivity.startActivity(intent);
            }
        });

        return vh;
    }

    public void onBindViewHolder(ListViewHolder holder, final int position) {
        holder.mTextView.setText(mDataset.get(position));
        holder.setIndex(mDataset.get(position));
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
