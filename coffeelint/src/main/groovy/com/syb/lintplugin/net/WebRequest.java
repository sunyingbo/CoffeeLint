package com.syb.lintplugin.net;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public class WebRequest {

    private OkHttpClient okHttpClient;

    private WebRequest() {
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        okHttpClientBuilder.connectTimeout(5L, TimeUnit.SECONDS);
        okHttpClientBuilder.writeTimeout(60L, TimeUnit.SECONDS);
        okHttpClientBuilder.readTimeout(60L, TimeUnit.SECONDS);
        okHttpClientBuilder.retryOnConnectionFailure(true);
        this.okHttpClient = okHttpClientBuilder.build();
    }

    public static RequestBody getJSONRequestBody(String data) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        return RequestBody.create(JSON, data);
    }

    public static WebRequest getInstance() {
        return WebRequest.InstanceHelper.instance;
    }

    public String getRequest(String url) {
        return this.getRequest(url, (Headers)null);
    }

    public String getRequest(String url, Headers headers) {
        okhttp3.Request.Builder request = new okhttp3.Request.Builder();
        request.url(url);
        if (headers != null) {
            request.headers(headers);
        }

        return this.execute(request.build());
    }

    public String postRequest(String url, RequestBody requestBody) {
        return this.postRequest(url, requestBody, (Headers)null);
    }

    public String postRequest(String url, RequestBody requestBody, Headers headers) {
        okhttp3.Request.Builder request = new okhttp3.Request.Builder();
        request.url(url);
        request.post(requestBody);
        if (headers != null) {
            request.headers(headers);
        }

        return this.execute(request.build());
    }

    public void putRequest(String url, RequestBody requestBody) {
        this.putRequest(url, requestBody, (Headers)null);
    }

    public void putRequest(String url, RequestBody requestBody, Headers headers) {
        okhttp3.Request.Builder request = new okhttp3.Request.Builder();
        request.url(url);
        request.put(requestBody);
        if (headers != null) {
            request.headers(headers);
        }

        this.execute(request.build());
    }

    private String execute(Request request) {
        Call call = this.okHttpClient.newCall(request);
        Response response = null;

        try {
            response = call.execute();
            if (response.code() > 300) {
                response.close();
                return null;
            } else {
                String result = response.body().string();
                response.close();
                return result;
            }
        } catch (IOException var5) {
            if (response != null) {
                response.close();
            }

            var5.printStackTrace();
            return null;
        }
    }

    private String getRequestParam(Request request) {
        Buffer buffer = new Buffer();
        RequestBody requestBody = request.body();
        if (requestBody != null) {
            try {
                requestBody.writeTo(buffer);
            } catch (IOException var6) {
                var6.printStackTrace();
            }

            Charset charset = Charset.forName("UTF-8");
            MediaType contentType = requestBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(charset);
            }

            if (charset == null) {
                charset = Charset.forName("UTF-8");
            }

            return buffer.readString(charset);
        } else {
            return "";
        }
    }

    private static class InstanceHelper {
        private static WebRequest instance = new WebRequest();

        private InstanceHelper() {
        }
    }

}
