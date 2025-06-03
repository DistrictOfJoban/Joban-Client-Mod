package com.lx862.mtrscripting.util;

import org.apache.commons.io.IOUtils;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.NativeObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@SuppressWarnings("unused")
public class NetworkingUtil {
    private static final String USER_AGENT_STRING = "Joban Client Mod (https://jcm.joban.org)";

    public static NetworkResponse<?> fetchString(String urlStr) throws IOException {
        return fetchString(urlStr, null);
    }

    public static NetworkResponse<BufferedImage> fetchImage(String urlStr) throws IOException {
        return fetchImage(urlStr, null);
    }

    public static NetworkResponse<?> fetchString(String urlStr, NativeObject requestObject) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        processRequestObject(requestObject, urlConnection);

        try (InputStream is = urlConnection.getInputStream()) {
            String str = IOUtils.toString(is, StandardCharsets.UTF_8);
            return new NetworkResponse<>(str, urlConnection.getHeaderFields(), urlConnection.getResponseCode());
        }
    }

    public static NetworkResponse<BufferedImage> fetchImage(String urlStr, NativeObject requestObject) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        processRequestObject(requestObject, urlConnection);
        try(InputStream is = urlConnection.getInputStream()) {
            return new NetworkResponse<>(ImageIO.read(is), urlConnection.getHeaderFields(), urlConnection.getResponseCode());
        }
    }

    private static void processRequestObject(NativeObject requestObject, HttpURLConnection connection) throws IOException {
        String body = null;

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

        // Override User-Agent
        connection.setRequestProperty("User-Agent", USER_AGENT_STRING);

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
}
