package com.lx862.mtrscripting.util;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class NetworkResponse<T> {
    private final int statusCode;
    private final T data;
    private final Map<String, List<String>> headers;
    private final Exception exception;

    public NetworkResponse(T data, Map<String, List<String>> headers, int statusCode, Exception exception) {
        this.data = data;
        this.statusCode = statusCode;
        this.headers = headers;
        this.exception = exception;
    }

    public NetworkResponse(T data, Map<String, List<String>> headers, int statusCode) {
        this(data, headers, statusCode, null);
    }

    public T getData() {
        return data;
    }

    /**
     * Obtain the HTTP status code.
     * -1 if the HTTP request failed to send
     */
    public int getResponseCode() {
        return statusCode;
    }

    public boolean success() {
        return this.exception == null;
    }

    public boolean ok() {
        return this.statusCode >= 200 && this.statusCode <= 299;
    }

    public Exception exception() {
        return this.exception;
    }

    public Map<String, List<String>> getHeaders() {
        return this.headers;
    }
}
