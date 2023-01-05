package com.craftmend.openaudiomc.generic.rest;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.generic.platform.interfaces.TaskService;
import com.craftmend.openaudiomc.generic.rest.response.AbstractRestResponse;
import com.craftmend.openaudiomc.generic.rest.response.IntermediateResponse;
import com.craftmend.openaudiomc.generic.rest.response.SectionError;
import com.craftmend.openaudiomc.generic.rest.response.ShorthandResponse;
import com.craftmend.openaudiomc.generic.rest.target.Endpoint;
import com.craftmend.openaudiomc.generic.rest.target.Method;
import lombok.Getter;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RestRequest<T extends AbstractRestResponse> {

    private SectionError sectionError = SectionError.NONE;
    private Endpoint endpoint;
    private Map<String, String> queryParameters = new HashMap<>();
    public T response;
    private String baseUrl = null;
    private RequestBody postBody = null;
    private Method method = Method.GET;
    private int timeout = -1;
    private Class<T> typeClass;

    public RestRequest(Class<T> typeClass, Endpoint endpoint) {
        this.endpoint = endpoint;
        this.typeClass = typeClass;
    }

    public boolean hasError() {
        return sectionError != SectionError.NONE;
    }

    public RestRequest<T> setQuery(String key, String value) {
        queryParameters.put(key, value);
        return this;
    }

    public RestRequest<T> setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public RestRequest<T> withPostJsonObject(Object o) {
        this.postBody = RequestBody.create(OpenAudioMc.getGson().toJson(o), MediaType.get("application/json; charset=utf-8"));
        this.method = Method.POST;
        return this;
    }

    public RestRequest<T> setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public void run() {
        HttpRes res = this.preformRequest();

        // catch timeout
        if (res.code == 408) {
            sectionError = SectionError.TIMEOUT;
            return;
        }

        // is it a 500?
        if (res.code == 500) {
            sectionError = SectionError.SERVER_ERROR;
            return;
        }

        // is it a 404?
        if (res.code == 404) {
            sectionError = SectionError.NOT_FOUND;
            return;
        }

        // ok, now parse it
        IntermediateResponse<T> intermediateResponse = IntermediateResponse.fromJson(typeClass, res.body);

        // copy over the error and response
        sectionError = intermediateResponse.getError();
        response = intermediateResponse.getResponse(typeClass);
    }

    public CompletableFuture<ShorthandResponse<T>> runAsync() {
        CompletableFuture<ShorthandResponse<T>> future = new CompletableFuture<>();
        OpenAudioMc.resolveDependency(TaskService.class).runAsync(() -> {
            try {
                this.run();
                future.complete(new ShorthandResponse<T>(this.response, this.sectionError));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private HttpRes preformRequest() {
        // reset state
        sectionError = SectionError.NONE;
        response = null;

        String url = endpoint.getURL(this.baseUrl); // uses the default baseurl if null

        // add query params
        if (queryParameters.size() > 0) {
            url = url + "?";
            for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
                url = url + entry.getKey() + "=" + entry.getValue() + "&";
            }
            url = url.substring(0, url.length() - 1);
        }

        // create request
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("oa-env", OpenAudioMc.SERVER_ENVIRONMENT.toString());

        if (method == Method.POST) {
            requestBuilder = requestBuilder.post(postBody);
        } else {
            requestBuilder = requestBuilder.get();
        }

        OkHttpClient.Builder clientBuilder = new OkHttpClient().newBuilder();
        if (timeout != -1) {
            clientBuilder = clientBuilder.readTimeout(timeout, java.util.concurrent.TimeUnit.SECONDS);
            clientBuilder = clientBuilder.connectTimeout(timeout, java.util.concurrent.TimeUnit.SECONDS);
        }

        try {
            okhttp3.Response response = clientBuilder.build().newCall(requestBuilder.build()).execute();
            return new HttpRes(response.code(), response.body().string());
        } catch (Exception e) {
            e.printStackTrace();
            return new HttpRes(408, "Request timed out");
        }
    }

    @Getter
    static class HttpRes {
        private final int code;
        private final String body;

        public HttpRes(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }

}
