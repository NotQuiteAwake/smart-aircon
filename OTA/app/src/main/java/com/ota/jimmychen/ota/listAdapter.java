package com.ota.jimmychen.ota;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class listAdapter extends RecyclerView.Adapter<listAdapter.listViewHolder> {
    private List<String> mDataset;

    public static class listViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public listViewHolder(TextView v) {
            super(v);
            mTextView = v;
        }
    }

    public listAdapter(List<String> dataSet) { mDataset = dataSet; }

    @Override
    public listAdapter.listViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView v = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_base, parent, false);
        listViewHolder vh = new listViewHolder(v);
        return vh;
    }

    public void onBindViewHolder(listViewHolder holder, int position) {
        holder.mTextView.setText(mDataset.get(position));
    }

    public int getItemCount() {
        return mDataset.size();
    }
}
