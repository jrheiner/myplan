package de.myplan.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class UserTimetable extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_timetable);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        /*
      The {@link android.support.v4.view.PagerAdapter} that will provide
      fragments for each of the sections. We use a
      {@link FragmentPagerAdapter} derivative, which will keep every
      loaded fragment in memory. If this becomes too memory intensive, it
      may be best to switch to a
      {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        /*
      The {@link ViewPager} that will host the section contents.
     */
        ViewPager mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        Calendar c = Calendar.getInstance();
        int weekday = c.get(Calendar.DAY_OF_WEEK);
        switch (weekday) {
            case Calendar.SUNDAY:
                weekday = 0;
                break;
            case Calendar.MONDAY:
                weekday = 0;
                break;
            case Calendar.TUESDAY:
                weekday = 1;
                break;
            case Calendar.WEDNESDAY:
                weekday = 2;
                break;
            case Calendar.THURSDAY:
                weekday = 3;
                break;
            case Calendar.FRIDAY:
                weekday = 4;
                break;
            case Calendar.SATURDAY:
                weekday = 0;
                break;
        }
        mViewPager.setCurrentItem(weekday);

        TabLayout tabLayout = findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.setTitle(String.format("%s", getString(R.string.usertimetable_header)));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_user_timetable, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_reset:
                new AlertDialog.Builder(this)
                        .setTitle("Stundenplan zurücksetzen")
                        .setMessage("Dein gesamter Stundenplan wird gelöscht!")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                resetTimetable();
                                finish();
                                startActivity(getIntent());
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            /*
            case R.id.action_import:
                return true;

            case R.id.action_export:
                return true;
            */
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }


    }

    private void resetTimetable() {
        SharedPreferences sp = this.getSharedPreferences("timetable", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString("timetable", "{\"day1\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day2\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day3\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day4\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day5\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"}}");
        ed.apply();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements AdapterView.OnItemSelectedListener {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        JSONObject jtimetable;
        JSONObject jday1;
        JSONObject jday2;
        JSONObject jday3;
        JSONObject jday4;
        JSONObject jday5;

        Spinner spinner_1;
        Spinner spinner_2;
        Spinner spinner_3;
        Spinner spinner_4;
        Spinner spinner_5;
        Spinner spinner_6;
        Spinner spinner_7;
        Spinner spinner_8;
        Spinner spinner_9;
        Spinner spinner_10;
        Spinner spinner_11;
        Spinner spinner_12;
        Spinner spinner_13;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_user_timetable, container, false);
            //TextView textView = rootView.findViewById(R.id.section_label);
            //textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));

            final ToggleButton toggle = rootView.findViewById(R.id.tt_toggleButton_editsave);

            // TODO cleanup
            // TODO improve loading performance


            final EditText teacher_1 = rootView.findViewById(R.id.tt_EditText_teacher_1);
            final EditText teacher_2 = rootView.findViewById(R.id.tt_EditText_teacher_2);
            final EditText teacher_3 = rootView.findViewById(R.id.tt_EditText_teacher_3);
            final EditText teacher_4 = rootView.findViewById(R.id.tt_EditText_teacher_4);
            final EditText teacher_5 = rootView.findViewById(R.id.tt_EditText_teacher_5);
            final EditText teacher_6 = rootView.findViewById(R.id.tt_EditText_teacher_6);
            final EditText teacher_7 = rootView.findViewById(R.id.tt_EditText_teacher_7);
            final EditText teacher_8 = rootView.findViewById(R.id.tt_EditText_teacher_8);
            final EditText teacher_9 = rootView.findViewById(R.id.tt_EditText_teacher_9);
            final EditText teacher_10 = rootView.findViewById(R.id.tt_EditText_teacher_10);
            final EditText teacher_11 = rootView.findViewById(R.id.tt_EditText_teacher_11);
            final EditText teacher_12 = rootView.findViewById(R.id.tt_EditText_teacher_12);
            final EditText teacher_13 = rootView.findViewById(R.id.tt_EditText_teacher_13);

            spinner_1 = rootView.findViewById(R.id.tt_spinner_1);
            spinner_2 = rootView.findViewById(R.id.tt_spinner_2);
            spinner_3 = rootView.findViewById(R.id.tt_spinner_3);
            spinner_4 = rootView.findViewById(R.id.tt_spinner_4);
            spinner_5 = rootView.findViewById(R.id.tt_spinner_5);
            spinner_6 = rootView.findViewById(R.id.tt_spinner_6);
            spinner_7 = rootView.findViewById(R.id.tt_spinner_7);
            spinner_8 = rootView.findViewById(R.id.tt_spinner_8);
            spinner_9 = rootView.findViewById(R.id.tt_spinner_9);
            spinner_10 = rootView.findViewById(R.id.tt_spinner_10);
            spinner_11 = rootView.findViewById(R.id.tt_spinner_11);
            spinner_12 = rootView.findViewById(R.id.tt_spinner_12);
            spinner_13 = rootView.findViewById(R.id.tt_spinner_13);

            final ArrayAdapter<CharSequence> spinner_adapter = ArrayAdapter.createFromResource(getActivity(),
                    R.array.tt_base_subjects, android.R.layout.simple_spinner_item);
            spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spinner_1.setAdapter(spinner_adapter);
            spinner_2.setAdapter(spinner_adapter);
            spinner_3.setAdapter(spinner_adapter);
            spinner_4.setAdapter(spinner_adapter);
            spinner_5.setAdapter(spinner_adapter);
            spinner_6.setAdapter(spinner_adapter);
            spinner_7.setAdapter(spinner_adapter);
            spinner_8.setAdapter(spinner_adapter);
            spinner_9.setAdapter(spinner_adapter);
            spinner_10.setAdapter(spinner_adapter);
            spinner_11.setAdapter(spinner_adapter);
            spinner_12.setAdapter(spinner_adapter);
            spinner_13.setAdapter(spinner_adapter);

            spinner_1.setOnItemSelectedListener(this);
            spinner_3.setOnItemSelectedListener(this);
            spinner_5.setOnItemSelectedListener(this);
            spinner_8.setOnItemSelectedListener(this);
            spinner_10.setOnItemSelectedListener(this);
            spinner_12.setOnItemSelectedListener(this);

            disableSpinner(rootView);

            try {
                jtimetable = new JSONObject(getTimetable());
                jday1 = jtimetable.getJSONObject("day1");
                if (jday1 == null) jday1 = new JSONObject();
                jday2 = jtimetable.getJSONObject("day2");
                if (jday2 == null) jday1 = new JSONObject();
                jday3 = jtimetable.getJSONObject("day3");
                if (jday3 == null) jday1 = new JSONObject();
                jday4 = jtimetable.getJSONObject("day4");
                if (jday4 == null) jday1 = new JSONObject();
                jday5 = jtimetable.getJSONObject("day5");
                if (jday5 == null) jday1 = new JSONObject();

                switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                    case 1:
                        spinner_1.setSelection(Integer.valueOf(jday1.getString("1").split(";")[0]));
                        spinner_2.setSelection(Integer.valueOf(jday1.getString("2").split(";")[0]));
                        spinner_3.setSelection(Integer.valueOf(jday1.getString("3").split(";")[0]));
                        spinner_4.setSelection(Integer.valueOf(jday1.getString("4").split(";")[0]));
                        spinner_5.setSelection(Integer.valueOf(jday1.getString("5").split(";")[0]));
                        spinner_6.setSelection(Integer.valueOf(jday1.getString("6").split(";")[0]));
                        spinner_7.setSelection(Integer.valueOf(jday1.getString("7").split(";")[0]));
                        spinner_8.setSelection(Integer.valueOf(jday1.getString("8").split(";")[0]));
                        spinner_9.setSelection(Integer.valueOf(jday1.getString("9").split(";")[0]));
                        spinner_10.setSelection(Integer.valueOf(jday1.getString("10").split(";")[0]));
                        spinner_11.setSelection(Integer.valueOf(jday1.getString("11").split(";")[0]));
                        spinner_12.setSelection(Integer.valueOf(jday1.getString("12").split(";")[0]));
                        spinner_13.setSelection(Integer.valueOf(jday1.getString("13").split(";")[0]));
                        teacher_1.setText(jday1.getString("1").split(";")[1].replaceAll(" ", ""));
                        teacher_2.setText(jday1.getString("2").split(";")[1].replaceAll(" ", ""));
                        teacher_3.setText(jday1.getString("3").split(";")[1].replaceAll(" ", ""));
                        teacher_4.setText(jday1.getString("4").split(";")[1].replaceAll(" ", ""));
                        teacher_5.setText(jday1.getString("5").split(";")[1].replaceAll(" ", ""));
                        teacher_6.setText(jday1.getString("6").split(";")[1].replaceAll(" ", ""));
                        teacher_7.setText(jday1.getString("7").split(";")[1].replaceAll(" ", ""));
                        teacher_8.setText(jday1.getString("8").split(";")[1].replaceAll(" ", ""));
                        teacher_9.setText(jday1.getString("9").split(";")[1].replaceAll(" ", ""));
                        teacher_10.setText(jday1.getString("10").split(";")[1].replaceAll(" ", ""));
                        teacher_11.setText(jday1.getString("11").split(";")[1].replaceAll(" ", ""));
                        teacher_12.setText(jday1.getString("12").split(";")[1].replaceAll(" ", ""));
                        teacher_13.setText(jday1.getString("13").split(";")[1].replaceAll(" ", ""));
                        break;
                    case 2:
                        spinner_1.setSelection(Integer.valueOf(jday2.getString("1").split(";")[0]));
                        spinner_2.setSelection(Integer.valueOf(jday2.getString("2").split(";")[0]));
                        spinner_3.setSelection(Integer.valueOf(jday2.getString("3").split(";")[0]));
                        spinner_4.setSelection(Integer.valueOf(jday2.getString("4").split(";")[0]));
                        spinner_5.setSelection(Integer.valueOf(jday2.getString("5").split(";")[0]));
                        spinner_6.setSelection(Integer.valueOf(jday2.getString("6").split(";")[0]));
                        spinner_7.setSelection(Integer.valueOf(jday2.getString("7").split(";")[0]));
                        spinner_8.setSelection(Integer.valueOf(jday2.getString("8").split(";")[0]));
                        spinner_9.setSelection(Integer.valueOf(jday2.getString("9").split(";")[0]));
                        spinner_10.setSelection(Integer.valueOf(jday2.getString("10").split(";")[0]));
                        spinner_11.setSelection(Integer.valueOf(jday2.getString("11").split(";")[0]));
                        spinner_12.setSelection(Integer.valueOf(jday2.getString("12").split(";")[0]));
                        spinner_13.setSelection(Integer.valueOf(jday2.getString("13").split(";")[0]));
                        teacher_1.setText(jday2.getString("1").split(";")[1].replaceAll(" ", ""));
                        teacher_2.setText(jday2.getString("2").split(";")[1].replaceAll(" ", ""));
                        teacher_3.setText(jday2.getString("3").split(";")[1].replaceAll(" ", ""));
                        teacher_4.setText(jday2.getString("4").split(";")[1].replaceAll(" ", ""));
                        teacher_5.setText(jday2.getString("5").split(";")[1].replaceAll(" ", ""));
                        teacher_6.setText(jday2.getString("6").split(";")[1].replaceAll(" ", ""));
                        teacher_7.setText(jday2.getString("7").split(";")[1].replaceAll(" ", ""));
                        teacher_8.setText(jday2.getString("8").split(";")[1].replaceAll(" ", ""));
                        teacher_9.setText(jday2.getString("9").split(";")[1].replaceAll(" ", ""));
                        teacher_10.setText(jday2.getString("10").split(";")[1].replaceAll(" ", ""));
                        teacher_11.setText(jday2.getString("11").split(";")[1].replaceAll(" ", ""));
                        teacher_12.setText(jday2.getString("12").split(";")[1].replaceAll(" ", ""));
                        teacher_13.setText(jday2.getString("13").split(";")[1].replaceAll(" ", ""));
                        break;
                    case 3:
                        spinner_1.setSelection(Integer.valueOf(jday3.getString("1").split(";")[0]));
                        spinner_2.setSelection(Integer.valueOf(jday3.getString("2").split(";")[0]));
                        spinner_3.setSelection(Integer.valueOf(jday3.getString("3").split(";")[0]));
                        spinner_4.setSelection(Integer.valueOf(jday3.getString("4").split(";")[0]));
                        spinner_5.setSelection(Integer.valueOf(jday3.getString("5").split(";")[0]));
                        spinner_6.setSelection(Integer.valueOf(jday3.getString("6").split(";")[0]));
                        spinner_7.setSelection(Integer.valueOf(jday3.getString("7").split(";")[0]));
                        spinner_8.setSelection(Integer.valueOf(jday3.getString("8").split(";")[0]));
                        spinner_9.setSelection(Integer.valueOf(jday3.getString("9").split(";")[0]));
                        spinner_10.setSelection(Integer.valueOf(jday3.getString("10").split(";")[0]));
                        spinner_11.setSelection(Integer.valueOf(jday3.getString("11").split(";")[0]));
                        spinner_12.setSelection(Integer.valueOf(jday3.getString("12").split(";")[0]));
                        spinner_13.setSelection(Integer.valueOf(jday3.getString("13").split(";")[0]));
                        teacher_1.setText(jday3.getString("1").split(";")[1].replaceAll(" ", ""));
                        teacher_2.setText(jday3.getString("2").split(";")[1].replaceAll(" ", ""));
                        teacher_3.setText(jday3.getString("3").split(";")[1].replaceAll(" ", ""));
                        teacher_4.setText(jday3.getString("4").split(";")[1].replaceAll(" ", ""));
                        teacher_5.setText(jday3.getString("5").split(";")[1].replaceAll(" ", ""));
                        teacher_6.setText(jday3.getString("6").split(";")[1].replaceAll(" ", ""));
                        teacher_7.setText(jday3.getString("7").split(";")[1].replaceAll(" ", ""));
                        teacher_8.setText(jday3.getString("8").split(";")[1].replaceAll(" ", ""));
                        teacher_9.setText(jday3.getString("9").split(";")[1].replaceAll(" ", ""));
                        teacher_10.setText(jday3.getString("10").split(";")[1].replaceAll(" ", ""));
                        teacher_11.setText(jday3.getString("11").split(";")[1].replaceAll(" ", ""));
                        teacher_12.setText(jday3.getString("12").split(";")[1].replaceAll(" ", ""));
                        teacher_13.setText(jday3.getString("13").split(";")[1].replaceAll(" ", ""));
                        break;
                    case 4:
                        spinner_1.setSelection(Integer.valueOf(jday4.getString("1").split(";")[0]));
                        spinner_2.setSelection(Integer.valueOf(jday4.getString("2").split(";")[0]));
                        spinner_3.setSelection(Integer.valueOf(jday4.getString("3").split(";")[0]));
                        spinner_4.setSelection(Integer.valueOf(jday4.getString("4").split(";")[0]));
                        spinner_5.setSelection(Integer.valueOf(jday4.getString("5").split(";")[0]));
                        spinner_6.setSelection(Integer.valueOf(jday4.getString("6").split(";")[0]));
                        spinner_7.setSelection(Integer.valueOf(jday4.getString("7").split(";")[0]));
                        spinner_8.setSelection(Integer.valueOf(jday4.getString("8").split(";")[0]));
                        spinner_9.setSelection(Integer.valueOf(jday4.getString("9").split(";")[0]));
                        spinner_10.setSelection(Integer.valueOf(jday4.getString("10").split(";")[0]));
                        spinner_11.setSelection(Integer.valueOf(jday4.getString("11").split(";")[0]));
                        spinner_12.setSelection(Integer.valueOf(jday4.getString("12").split(";")[0]));
                        spinner_13.setSelection(Integer.valueOf(jday4.getString("13").split(";")[0]));
                        teacher_1.setText(jday4.getString("1").split(";")[1].replaceAll(" ", ""));
                        teacher_2.setText(jday4.getString("2").split(";")[1].replaceAll(" ", ""));
                        teacher_3.setText(jday4.getString("3").split(";")[1].replaceAll(" ", ""));
                        teacher_4.setText(jday4.getString("4").split(";")[1].replaceAll(" ", ""));
                        teacher_5.setText(jday4.getString("5").split(";")[1].replaceAll(" ", ""));
                        teacher_6.setText(jday4.getString("6").split(";")[1].replaceAll(" ", ""));
                        teacher_7.setText(jday4.getString("7").split(";")[1].replaceAll(" ", ""));
                        teacher_8.setText(jday4.getString("8").split(";")[1].replaceAll(" ", ""));
                        teacher_9.setText(jday4.getString("9").split(";")[1].replaceAll(" ", ""));
                        teacher_10.setText(jday4.getString("10").split(";")[1].replaceAll(" ", ""));
                        teacher_11.setText(jday4.getString("11").split(";")[1].replaceAll(" ", ""));
                        teacher_12.setText(jday4.getString("12").split(";")[1].replaceAll(" ", ""));
                        teacher_13.setText(jday4.getString("13").split(";")[1].replaceAll(" ", ""));
                        break;
                    case 5:
                        spinner_1.setSelection(Integer.valueOf(jday5.getString("1").split(";")[0]));
                        spinner_2.setSelection(Integer.valueOf(jday5.getString("2").split(";")[0]));
                        spinner_3.setSelection(Integer.valueOf(jday5.getString("3").split(";")[0]));
                        spinner_4.setSelection(Integer.valueOf(jday5.getString("4").split(";")[0]));
                        spinner_5.setSelection(Integer.valueOf(jday5.getString("5").split(";")[0]));
                        spinner_6.setSelection(Integer.valueOf(jday5.getString("6").split(";")[0]));
                        spinner_7.setSelection(Integer.valueOf(jday5.getString("7").split(";")[0]));
                        spinner_8.setSelection(Integer.valueOf(jday5.getString("8").split(";")[0]));
                        spinner_9.setSelection(Integer.valueOf(jday5.getString("9").split(";")[0]));
                        spinner_10.setSelection(Integer.valueOf(jday5.getString("10").split(";")[0]));
                        spinner_11.setSelection(Integer.valueOf(jday5.getString("11").split(";")[0]));
                        spinner_12.setSelection(Integer.valueOf(jday5.getString("12").split(";")[0]));
                        spinner_13.setSelection(Integer.valueOf(jday5.getString("13").split(";")[0]));
                        teacher_1.setText(jday5.getString("1").split(";")[1].replaceAll(" ", ""));
                        teacher_2.setText(jday5.getString("2").split(";")[1].replaceAll(" ", ""));
                        teacher_3.setText(jday5.getString("3").split(";")[1].replaceAll(" ", ""));
                        teacher_4.setText(jday5.getString("4").split(";")[1].replaceAll(" ", ""));
                        teacher_5.setText(jday5.getString("5").split(";")[1].replaceAll(" ", ""));
                        teacher_6.setText(jday5.getString("6").split(";")[1].replaceAll(" ", ""));
                        teacher_7.setText(jday5.getString("7").split(";")[1].replaceAll(" ", ""));
                        teacher_8.setText(jday5.getString("8").split(";")[1].replaceAll(" ", ""));
                        teacher_9.setText(jday5.getString("9").split(";")[1].replaceAll(" ", ""));
                        teacher_10.setText(jday5.getString("10").split(";")[1].replaceAll(" ", ""));
                        teacher_11.setText(jday5.getString("11").split(";")[1].replaceAll(" ", ""));
                        teacher_12.setText(jday5.getString("12").split(";")[1].replaceAll(" ", ""));
                        teacher_13.setText(jday5.getString("13").split(";")[1].replaceAll(" ", ""));
                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {

                        spinner_1.setEnabled(true);
                        spinner_2.setEnabled(true);
                        spinner_3.setEnabled(true);
                        spinner_4.setEnabled(true);
                        spinner_5.setEnabled(true);
                        spinner_6.setEnabled(true);
                        spinner_7.setEnabled(true);
                        spinner_8.setEnabled(true);
                        spinner_9.setEnabled(true);
                        spinner_10.setEnabled(true);
                        spinner_11.setEnabled(true);
                        spinner_12.setEnabled(true);
                        spinner_13.setEnabled(true);

                        teacher_1.setEnabled(true);
                        teacher_2.setEnabled(true);
                        teacher_3.setEnabled(true);
                        teacher_4.setEnabled(true);
                        teacher_5.setEnabled(true);
                        teacher_6.setEnabled(true);
                        teacher_7.setEnabled(true);
                        teacher_8.setEnabled(true);
                        teacher_9.setEnabled(true);
                        teacher_10.setEnabled(true);
                        teacher_11.setEnabled(true);
                        teacher_12.setEnabled(true);
                        teacher_13.setEnabled(true);
                    } else {
                        disableSpinner(rootView);
                        try {
                            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                                case 1:
                                    jday1.put("1", String.format("%s;%s", spinner_1.getSelectedItemPosition(), teacher_1.getText() + " "));
                                    jday1.put("2", String.format("%s;%s", spinner_2.getSelectedItemPosition(), teacher_2.getText() + " "));
                                    jday1.put("3", String.format("%s;%s", spinner_3.getSelectedItemPosition(), teacher_3.getText() + " "));
                                    jday1.put("4", String.format("%s;%s", spinner_4.getSelectedItemPosition(), teacher_4.getText() + " "));
                                    jday1.put("5", String.format("%s;%s", spinner_5.getSelectedItemPosition(), teacher_5.getText() + " "));
                                    jday1.put("6", String.format("%s;%s", spinner_6.getSelectedItemPosition(), teacher_6.getText() + " "));
                                    jday1.put("7", String.format("%s;%s", spinner_7.getSelectedItemPosition(), teacher_7.getText() + " "));
                                    jday1.put("8", String.format("%s;%s", spinner_8.getSelectedItemPosition(), teacher_8.getText() + " "));
                                    jday1.put("9", String.format("%s;%s", spinner_9.getSelectedItemPosition(), teacher_9.getText() + " "));
                                    jday1.put("10", String.format("%s;%s", spinner_10.getSelectedItemPosition(), teacher_10.getText() + " "));
                                    jday1.put("11", String.format("%s;%s", spinner_11.getSelectedItemPosition(), teacher_11.getText() + " "));
                                    jday1.put("12", String.format("%s;%s", spinner_12.getSelectedItemPosition(), teacher_12.getText() + " "));
                                    jday1.put("13", String.format("%s;%s", spinner_13.getSelectedItemPosition(), teacher_13.getText() + " "));
                                    break;
                                case 2:
                                    jday2.put("1", String.format("%s;%s", spinner_1.getSelectedItemPosition(), teacher_1.getText() + " "));
                                    jday2.put("2", String.format("%s;%s", spinner_2.getSelectedItemPosition(), teacher_2.getText() + " "));
                                    jday2.put("3", String.format("%s;%s", spinner_3.getSelectedItemPosition(), teacher_3.getText() + " "));
                                    jday2.put("4", String.format("%s;%s", spinner_4.getSelectedItemPosition(), teacher_4.getText() + " "));
                                    jday2.put("5", String.format("%s;%s", spinner_5.getSelectedItemPosition(), teacher_5.getText() + " "));
                                    jday2.put("6", String.format("%s;%s", spinner_6.getSelectedItemPosition(), teacher_6.getText() + " "));
                                    jday2.put("7", String.format("%s;%s", spinner_7.getSelectedItemPosition(), teacher_7.getText() + " "));
                                    jday2.put("8", String.format("%s;%s", spinner_8.getSelectedItemPosition(), teacher_8.getText() + " "));
                                    jday2.put("9", String.format("%s;%s", spinner_9.getSelectedItemPosition(), teacher_9.getText() + " "));
                                    jday2.put("10", String.format("%s;%s", spinner_10.getSelectedItemPosition(), teacher_10.getText() + " "));
                                    jday2.put("11", String.format("%s;%s", spinner_11.getSelectedItemPosition(), teacher_11.getText() + " "));
                                    jday2.put("12", String.format("%s;%s", spinner_12.getSelectedItemPosition(), teacher_12.getText() + " "));
                                    jday2.put("13", String.format("%s;%s", spinner_13.getSelectedItemPosition(), teacher_13.getText() + " "));
                                    break;
                                case 3:
                                    jday3.put("1", String.format("%s;%s", spinner_1.getSelectedItemPosition(), teacher_1.getText() + " "));
                                    jday3.put("2", String.format("%s;%s", spinner_2.getSelectedItemPosition(), teacher_2.getText() + " "));
                                    jday3.put("3", String.format("%s;%s", spinner_3.getSelectedItemPosition(), teacher_3.getText() + " "));
                                    jday3.put("4", String.format("%s;%s", spinner_4.getSelectedItemPosition(), teacher_4.getText() + " "));
                                    jday3.put("5", String.format("%s;%s", spinner_5.getSelectedItemPosition(), teacher_5.getText() + " "));
                                    jday3.put("6", String.format("%s;%s", spinner_6.getSelectedItemPosition(), teacher_6.getText() + " "));
                                    jday3.put("7", String.format("%s;%s", spinner_7.getSelectedItemPosition(), teacher_7.getText() + " "));
                                    jday3.put("8", String.format("%s;%s", spinner_8.getSelectedItemPosition(), teacher_8.getText() + " "));
                                    jday3.put("9", String.format("%s;%s", spinner_9.getSelectedItemPosition(), teacher_9.getText() + " "));
                                    jday3.put("10", String.format("%s;%s", spinner_10.getSelectedItemPosition(), teacher_10.getText() + " "));
                                    jday3.put("11", String.format("%s;%s", spinner_11.getSelectedItemPosition(), teacher_11.getText() + " "));
                                    jday3.put("12", String.format("%s;%s", spinner_12.getSelectedItemPosition(), teacher_12.getText() + " "));
                                    jday3.put("13", String.format("%s;%s", spinner_13.getSelectedItemPosition(), teacher_13.getText() + " "));
                                    break;
                                case 4:
                                    jday4.put("1", String.format("%s;%s", spinner_1.getSelectedItemPosition(), teacher_1.getText() + " "));
                                    jday4.put("2", String.format("%s;%s", spinner_2.getSelectedItemPosition(), teacher_2.getText() + " "));
                                    jday4.put("3", String.format("%s;%s", spinner_3.getSelectedItemPosition(), teacher_3.getText() + " "));
                                    jday4.put("4", String.format("%s;%s", spinner_4.getSelectedItemPosition(), teacher_4.getText() + " "));
                                    jday4.put("5", String.format("%s;%s", spinner_5.getSelectedItemPosition(), teacher_5.getText() + " "));
                                    jday4.put("6", String.format("%s;%s", spinner_6.getSelectedItemPosition(), teacher_6.getText() + " "));
                                    jday4.put("7", String.format("%s;%s", spinner_7.getSelectedItemPosition(), teacher_7.getText() + " "));
                                    jday4.put("8", String.format("%s;%s", spinner_8.getSelectedItemPosition(), teacher_8.getText() + " "));
                                    jday4.put("9", String.format("%s;%s", spinner_9.getSelectedItemPosition(), teacher_9.getText() + " "));
                                    jday4.put("10", String.format("%s;%s", spinner_10.getSelectedItemPosition(), teacher_10.getText() + " "));
                                    jday4.put("11", String.format("%s;%s", spinner_11.getSelectedItemPosition(), teacher_11.getText() + " "));
                                    jday4.put("12", String.format("%s;%s", spinner_12.getSelectedItemPosition(), teacher_12.getText() + " "));
                                    jday4.put("13", String.format("%s;%s", spinner_13.getSelectedItemPosition(), teacher_13.getText() + " "));
                                    break;
                                case 5:
                                    jday5.put("1", String.format("%s;%s", spinner_1.getSelectedItemPosition(), teacher_1.getText() + " "));
                                    jday5.put("2", String.format("%s;%s", spinner_2.getSelectedItemPosition(), teacher_2.getText() + " "));
                                    jday5.put("3", String.format("%s;%s", spinner_3.getSelectedItemPosition(), teacher_3.getText() + " "));
                                    jday5.put("4", String.format("%s;%s", spinner_4.getSelectedItemPosition(), teacher_4.getText() + " "));
                                    jday5.put("5", String.format("%s;%s", spinner_5.getSelectedItemPosition(), teacher_5.getText() + " "));
                                    jday5.put("6", String.format("%s;%s", spinner_6.getSelectedItemPosition(), teacher_6.getText() + " "));
                                    jday5.put("7", String.format("%s;%s", spinner_7.getSelectedItemPosition(), teacher_7.getText() + " "));
                                    jday5.put("8", String.format("%s;%s", spinner_8.getSelectedItemPosition(), teacher_8.getText() + " "));
                                    jday5.put("9", String.format("%s;%s", spinner_9.getSelectedItemPosition(), teacher_9.getText() + " "));
                                    jday5.put("10", String.format("%s;%s", spinner_10.getSelectedItemPosition(), teacher_10.getText() + " "));
                                    jday5.put("11", String.format("%s;%s", spinner_11.getSelectedItemPosition(), teacher_11.getText() + " "));
                                    jday5.put("12", String.format("%s;%s", spinner_12.getSelectedItemPosition(), teacher_12.getText() + " "));
                                    jday5.put("13", String.format("%s;%s", spinner_13.getSelectedItemPosition(), teacher_13.getText() + " "));
                                    break;
                            }
                            jtimetable.put("day1", jday1);
                            jtimetable.put("day2", jday2);
                            jtimetable.put("day3", jday3);
                            jtimetable.put("day4", jday4);
                            jtimetable.put("day5", jday5);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        setTimetable(jtimetable.toString());

                        teacher_1.setEnabled(false);
                        teacher_2.setEnabled(false);
                        teacher_3.setEnabled(false);
                        teacher_4.setEnabled(false);
                        teacher_5.setEnabled(false);
                        teacher_6.setEnabled(false);
                        teacher_7.setEnabled(false);
                        teacher_8.setEnabled(false);
                        teacher_9.setEnabled(false);
                        teacher_10.setEnabled(false);
                        teacher_11.setEnabled(false);
                        teacher_12.setEnabled(false);
                        teacher_13.setEnabled(false);

                    }
                }
            });

            return rootView;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            int spinner_id = parent.getId();
            if (position == 0) return;
            switch (spinner_id) {
                case R.id.tt_spinner_1:
                    if (spinner_2.getSelectedItemPosition() == 0) {
                        spinner_2.setSelection(position);
                    }
                    break;
                case R.id.tt_spinner_3:
                    if (spinner_4.getSelectedItemPosition() == 0) {
                        spinner_4.setSelection(position);
                    }
                    break;
                case R.id.tt_spinner_5:
                    if (spinner_6.getSelectedItemPosition() == 0) {
                        spinner_6.setSelection(position);
                    }
                    break;
                case R.id.tt_spinner_8:
                    if (spinner_9.getSelectedItemPosition() == 0) {
                        spinner_9.setSelection(position);
                    }
                    break;
                case R.id.tt_spinner_10:
                    if (spinner_11.getSelectedItemPosition() == 0) {
                        spinner_11.setSelection(position);
                    }
                    break;
                case R.id.tt_spinner_12:
                    if (spinner_13.getSelectedItemPosition() == 0) {
                        spinner_13.setSelection(position);
                    }
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

        public void disableSpinner(View view) {
            ArrayList<View> vSpinner = view.getTouchables();
            for (View v : vSpinner) {
                if (v instanceof Spinner) {
                    v.setEnabled(false);
                }
            }
        }

        private String getTimetable() {
            SharedPreferences sp = this.getActivity().getSharedPreferences("timetable", MODE_PRIVATE);
            return sp.getString("timetable", "{\"day1\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day2\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day3\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day4\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day5\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"}}");
        }

        private void setTimetable(String s) {
            SharedPreferences sp = this.getActivity().getSharedPreferences("timetable", MODE_PRIVATE);
            SharedPreferences.Editor ed = sp.edit();
            ed.putString("timetable", s);
            ed.apply();
        }
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return 5;
        }
    }
}
