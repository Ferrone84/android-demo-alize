package com.example.duret.testalize;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

public class SpeakerListAdapter extends ArrayAdapter<Speaker> {

    protected static final String LOG_TAG = SpeakerListAdapter.class.getSimpleName();

    private List<Speaker> items;
    private int layoutResourceId;
    private Context context;

    public SpeakerListAdapter(Context context, int layoutResourceId, List<Speaker> items) {
        super(context, layoutResourceId, items);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        SpeakerHolder holder = null;

        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        row = inflater.inflate(layoutResourceId, parent, false);

        holder = new SpeakerHolder();
        holder.speaker = items.get(position);

        holder.update = row.findViewById(R.id.update_button);
        holder.update.setTag(holder.speaker);
        holder.test = row.findViewById(R.id.test_button);
        if (holder.speaker.getName().isEmpty())
            holder.test.setEnabled(false);
        else
            holder.test.setEnabled(true);
        holder.test.setTag(holder.speaker);
        holder.remove = row.findViewById(R.id.remove_button);
        holder.remove.setTag(holder.speaker);

        holder.name = row.findViewById(R.id.speaker_name);
        //setNameTextChangeListener(holder);

        row.setTag(holder);

        setupItem(holder);
        return row;
    }

    private void setupItem(SpeakerHolder holder) {
        holder.name.setText(holder.speaker.getName());
    }

    public static class SpeakerHolder {
        Speaker speaker;
        TextView name;
        Button update;
        Button test;
        ImageButton remove;
    }

    /*private void setNameTextChangeListener(final SpeakerHolder holder) {
        holder.name.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                holder.speaker.setName(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }*/
}
