package de.myplan.android.util;

import androidx.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response;

import org.jsoup.nodes.Document;

public class JsoupRequestChain {
    private final String[] urls;
    private final Response.Listener<Document[]> listener;
    private final Response.ErrorListener errorListener;
    private final Document[] responses;

    public JsoupRequestChain(@NonNull String[] urls, @NonNull Response.Listener<Document[]> listener, @NonNull Response.ErrorListener errorListener) {
        if (urls.length == 0)
            throw new IllegalArgumentException("urls.length must be greater than 0");
        this.urls = urls;
        this.listener = listener;
        this.errorListener = errorListener;
        responses = new Document[urls.length];
    }

    public void execute(RequestQueue queue) {
        execute(queue, 0);
    }

    private void execute(RequestQueue queue, int index) {
        JsoupRequest request = new JsoupRequest(urls[index],
                response -> {
                    responses[index] = response;
                    int next = index + 1;
                    if (next < urls.length)
                        execute(queue, next);
                    else
                        listener.onResponse(responses);
                },
                errorListener);
        queue.add(request);
    }

}
