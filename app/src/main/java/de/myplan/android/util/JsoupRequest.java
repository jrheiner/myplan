package de.myplan.android.util;

import androidx.annotation.NonNull;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;

public class JsoupRequest extends Request<Document> {
    private final Response.Listener<Document> listener;

    public JsoupRequest(String url, Response.Listener<Document> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        this.listener = listener;
    }

    @Override
    protected final void deliverResponse(Document response) {
        listener.onResponse(response);
    }

    @Override
    protected final Response<Document> parseNetworkResponse(@NonNull NetworkResponse response) {
        try {
            String html = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return Response.success(Jsoup.parse(html), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        }
    }
}
