package com.ntc2019.walkie_talkie;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TalkAdapter extends RecyclerView.Adapter<TalkAdapter.TalkViewHolder> {
    private List<Talk> data;
    private Context context;
    private LayoutInflater layoutInflater;

    public TalkAdapter(Context context) {
        this.data = new ArrayList<>();
        this.context = context;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public TalkViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = null;
        switch (viewType) {
            case 0: itemView = layoutInflater.inflate(R.layout.layout_talk_message, parent, false);
                break;
            case 1: itemView = layoutInflater.inflate(R.layout.layout_talk_item_start, parent, false);
                break;
            case 2: itemView = layoutInflater.inflate(R.layout.layout_talk_item_stop, parent, false);
                break;
            default:
                break;
        }
        return new TalkViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TalkViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        Talk talk = data.get(position);
        if (talk.getIsMessage()) return 0;
        else return talk.getStart()? 1 : 2;
    }

    public void setData(List<Talk> talks) {
        data = talks;
        notifyDataSetChanged();
    }

    class TalkViewHolder extends RecyclerView.ViewHolder {
        private TextView talkerName;

        public TalkViewHolder(View itemView) {
            super(itemView);

            talkerName = itemView.findViewById(R.id.talk_item_talker);
        }

        void bind(final Talk talk) {
            if (talk.getIsMessage()) {
                String s = talk.getMessage();
                talkerName.setText(s);
            } else {
                talkerName.setText(talk.getSpeakerName());
            }
        }
    }
}
