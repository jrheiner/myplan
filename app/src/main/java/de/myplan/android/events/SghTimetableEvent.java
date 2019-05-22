package de.myplan.android.events;

import de.myplan.android.model.SghTimetable;

public class SghTimetableEvent {

    private final SghTimetable timetable;

    public SghTimetableEvent(SghTimetable timetable) {
        this.timetable = timetable;
    }

    public SghTimetable getTimetable() {
        return timetable;
    }
}
