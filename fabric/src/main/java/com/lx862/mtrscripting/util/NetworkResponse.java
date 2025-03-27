package com.lx862.mtrscripting.util;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class NetworkResponse<T> {
    private final int statusCode;
    private final T data;
    private final Map<String, List<String>> headers;

    public NetworkResponse(T data, Map<String, List<String>> headers, int statusCode) {
        this.data = data;
        this.statusCode = statusCode;
        this.headers = headers;
    }

    public T getData() {
        return data;
    }

    public int getResponseCode() {
        return statusCode;
    }

    public Map<String, List<String>> getHeaders() {
        return this.headers;
    }
}
