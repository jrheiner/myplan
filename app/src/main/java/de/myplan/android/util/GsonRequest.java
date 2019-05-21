package de.myplan.android.util;

import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;

public class GsonRequest<T> extends Request<T> {
    private final Gson gson;
    private final Class<T> clazz;
    private final Response.Listener<T> listener;

    public GsonRequest(String url, @Nullable Gson gson, Class<T> clazz, Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        this.gson = gson != null ? gson : new Gson();
        this.clazz = clazz;
        this.listener = listener;
    }

    @Override
    protected final void deliverResponse(T response) {
        listener.onResponse(response);
    }

    @Override
    protected final Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return Response.success(gson.fromJson(json, clazz), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        }
    }
}
