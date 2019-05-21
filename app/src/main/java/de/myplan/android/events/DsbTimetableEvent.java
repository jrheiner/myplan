package de.myplan.android.events;

import java.util.Date;

import de.myplan.android.model.DsbTimetable;

public class DsbTimetableEvent {

    private final DsbTimetable[] timetables;
    private final boolean changed;
    private final Date lastUpdate;

    public DsbTimetableEvent(DsbTimetable[] timetables, boolean changed, Date lastUpdate) {
        this.timetables = timetables;
        this.changed = changed;
        this.lastUpdate = lastUpdate;
    }

    public DsbTimetable[] getTimetables() {
        return timetables;
    }

    public boolean isChanged() {
        return changed;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }
}
