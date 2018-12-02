package de.myplan.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class DsbTimetable {

    @SerializedName("ishtml")
    public boolean isHtml;

    @SerializedName("timetabledate")
    public Date date;

    @SerializedName("timetablegroupname")
    public String groupName;

    @SerializedName("timetabletitle")
    public String title;

    @SerializedName("timetableurl")
    public String url;
}
