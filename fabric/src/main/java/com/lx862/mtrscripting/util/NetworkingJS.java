package com.lx862.mtrscripting.util;

import com.lx862.jcm.mod.config.JCMClientConfig;
import org.apache.commons.io.IOUtils;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.NativeObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class NetworkingJS {
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final int DEFAULT_READ_TIMEOUT = 10000;
    private static final String USER_AGENT_STRING = "Joban Client Mod (https://jcm.joban.org)";

    public static NetworkResponse<DataReaderJS> fetch(String urlStr) throws IOException {
        return fetch(urlStr, null);
    }

    public static NetworkResponse<DataReaderJS> fetch(String urlStr, NativeObject requestObject) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        urlConnection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        urlConnection.setReadTimeout(DEFAULT_READ_TIMEOUT);
        processRequestObject(requestObject, urlConnection);

        try(InputStream is = urlConnection.getInputStream()) {
            byte[] data = is.readAllBytes();
            return new NetworkResponse<>(new DataReaderJS(() -> new ByteArrayInputStream(data)), urlConnection.getHeaderFields(), urlConnection.getResponseCode());
        } catch (IOException e) {
            return new NetworkResponse<>(null, null, -1, e);
        }
    }

    @Deprecated
    public static NetworkResponse<?> fetchString(String urlStr) throws IOException {
        return fetchString(urlStr, null);
    }

    @Deprecated
    public static NetworkResponse<?> fetchString(String urlStr, NativeObject requestObject) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        urlConnection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        urlConnection.setReadTimeout(DEFAULT_READ_TIMEOUT);
        processRequestObject(requestObject, urlConnection);

        try (InputStream is = urlConnection.getInputStream()) {
            String str = IOUtils.toString(is, StandardCharsets.UTF_8);
            return new NetworkResponse<>(str, urlConnection.getHeaderFields(), urlConnection.getResponseCode());
        } catch (IOException e) {
            return new NetworkResponse<>(null, null, -1, e);
        }
    }

    @Deprecated
    public static NetworkResponse<BufferedImage> fetchImage(String urlStr) throws IOException {
        return fetchImage(urlStr, null);
    }

    @Deprecated
    public static NetworkResponse<BufferedImage> fetchImage(String urlStr, NativeObject requestObject) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        urlConnection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        urlConnection.setReadTimeout(DEFAULT_READ_TIMEOUT);

        processRequestObject(requestObject, urlConnection);
        try(InputStream is = urlConnection.getInputStream()) {
            return new NetworkResponse<>(ImageIO.read(is), urlConnection.getHeaderFields(), urlConnection.getResponseCode());
        } catch (IOException e) {
            return new NetworkResponse<>(null, null, -1, e);
        }
    }

    private static void processRequestObject(NativeObject requestObject, HttpURLConnection connection) throws IOException {
        String body = null;

        // Default UA
        connection.setRequestProperty("User-Agent", USER_AGENT_STRING);

        if(requestObject != null) {
            if(requestObject.containsKey("method")) {
                connection.setRequestMethod((String)requestObject.get("method"));
            }
            if(requestObject.containsKey("connectTimeout")) {
                Object timeoutObj = requestObject.get("connectTimeout");
                if(timeoutObj instanceof Double) {
                    connection.setConnectTimeout((int)(double)timeoutObj);
                }
                if(timeoutObj instanceof Integer) {
                    connection.setConnectTimeout((int)timeoutObj);
                }
            }
            if(requestObject.containsKey("readTimeout")) {
                Object timeoutObj = requestObject.get("readTimeout");
                if(timeoutObj instanceof Double) {
                    connection.setReadTimeout((int)(double)timeoutObj);
                }
                if(timeoutObj instanceof Integer) {
                    connection.setReadTimeout((int)timeoutObj);
                }
            }
            if(requestObject.containsKey("headers")) {
                NativeObject headerObject = (NativeObject)requestObject.get("headers");
                for(Map.Entry<Object, Object> header : headerObject.entrySet()) {
                    connection.setRequestProperty((String)header.getKey(), (String)header.getValue());
                }
            }

            if(requestObject.containsKey("body")) {
                body = (String)requestObject.get("body");
            }
        }

        // Override User-Agent if scripting restrictions not disabled
        if(!JCMClientConfig.INSTANCE.scripting.disableScriptRestrictions.value()) {
            connection.setRequestProperty("User-Agent", USER_AGENT_STRING);
        }

        if(body != null) {
            byte[] bodyByte = body.getBytes();
            long contentLength = bodyByte.length;
            connection.setRequestProperty("Content-Length", String.valueOf(contentLength));
            connection.setDoOutput(true);
            try(DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                dos.write(bodyByte);
            }
        }
    }

    @SuppressWarnings("unused")
    public static class NetworkResponse<T> {
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
}
