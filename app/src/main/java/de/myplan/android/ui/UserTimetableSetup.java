package de.myplan.android.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.myplan.android.R;

public class UserTimetableSetup extends AppCompatActivity implements mRecyclerViewAdapter.ItemClickListener {

    private mRecyclerViewAdapter adapter;
    private ArrayAdapter<String> spinner_adapter;
    private ArrayList<String> timetable_items;
    private ArrayList<String> subjects_list;
    private Spinner timetable_spinner;
    private Button timetable_button;
    private EditText timetable_editText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_timetable_setup);

        this.setTitle(String.format("%s", getString(R.string.usertimetablesetup_header)));

        List<String> subjects;
        if (getSubjectList().equals("")) {
            subjects = Arrays.asList(getResources().getStringArray(R.array.tt_base_subjects));
        } else {
            subjects = Arrays.asList(getSubjectList().replaceAll("[\\[\\]]", "").split("\\s*,\\s*"));
        }
        List<String> items = Arrays.asList(getTimetableItems().replaceAll("[\\[\\]]", "").split("\\s*,\\s*"));
        subjects_list = new ArrayList<>(subjects);
        timetable_items = new ArrayList<>(items);
        Collections.sort(subjects_list.subList(1, subjects_list.size()));
        Collections.sort(timetable_items);

        timetable_spinner = findViewById(R.id.timetable_spinner);
        timetable_button = findViewById(R.id.timetable_button);
        timetable_editText = findViewById(R.id.timetable_editText);

        spinner_adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, subjects_list);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timetable_spinner.setAdapter(spinner_adapter);


        timetable_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String subject = timetable_spinner.getSelectedItem().toString();
                String teacher = timetable_editText.getText().toString();
                if ((timetable_spinner.getSelectedItemPosition() >= 1) && (timetable_editText.getText().length() >= 2)) {
                    timetable_items.add(0, subject + " - " + teacher);
                    subjects_list.remove(timetable_spinner.getSelectedItemPosition());
                    if (subjects_list.size() <= 1) {
                        timetable_spinner.setEnabled(false);
                        timetable_editText.setEnabled(false);
                        timetable_button.setEnabled(false);
                    }
                    timetable_editText.setText("");
                    timetable_spinner.setSelection(0);
                    adapter.notifyDataSetChanged();
                    spinner_adapter.notifyDataSetChanged();

                    setTimetableItems(timetable_items.toString());
                    setSubjectList(subjects_list.toString());

                }
            }
        });


        RecyclerView mRecyclerView = findViewById(R.id.timetable_recyclerView);


        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        adapter = new mRecyclerViewAdapter(this, timetable_items);
        adapter.setClickListener(this);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(View view, int position) {
        Toast.makeText(this, "You clicked " + adapter.getItem(position) + " on row number " + position, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemLongClick(View view, final int position) {
        new AlertDialog.Builder(this)
                .setTitle("Eintrag löschen")
                .setMessage("Willst du \"" + adapter.getItem(position) + "\" löschen?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        Pattern p = Pattern.compile("(.+)\\s-");
                        Matcher m = p.matcher(adapter.getItem(position));
                        if (m.find()) subjects_list.add(m.group(1));
                        Collections.sort(subjects_list.subList(1, subjects_list.size()));
                        if (subjects_list.size() >= 1) {
                            timetable_spinner.setEnabled(true);
                            timetable_editText.setEnabled(true);
                            timetable_button.setEnabled(true);
                        }
                        timetable_items.remove(position);
                        adapter.notifyDataSetChanged();
                        spinner_adapter.notifyDataSetChanged();

                        setTimetableItems(timetable_items.toString());
                        setSubjectList(subjects_list.toString());
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private String getTimetableItems() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString("timetable_items", "");
    }

    private void setTimetableItems(String s) {
        SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(this).edit();
        ed.putString("timetable_items", s);
        ed.apply();
    }

    private String getSubjectList() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString("subject_list", "");
    }

    private void setSubjectList(String s) {
        SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(this).edit();
        ed.putString("subject_list", s);
        ed.apply();
    }


}

class mRecyclerViewAdapter extends RecyclerView.Adapter<mRecyclerViewAdapter.ViewHolder> {

    private final List<String> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    mRecyclerViewAdapter(Context context, List<String> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recycler_view_item, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String data = mData.get(position);
        holder.mTextView.setText(data);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);

        void onItemLongClick(View view, int position);
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView mTextView;

        ViewHolder(View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.timetable_item);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View view) {
            if (mClickListener != null) mClickListener.onItemLongClick(view, getAdapterPosition());
            return false;
        }
    }
}