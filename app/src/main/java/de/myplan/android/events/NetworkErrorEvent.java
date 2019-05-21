package de.myplan.android.events;

import com.android.volley.VolleyError;

public class NetworkErrorEvent {

    private final VolleyError error;

    public NetworkErrorEvent(VolleyError error) {
        this.error = error;
    }

    public VolleyError getError() {
        return error;
    }
}
