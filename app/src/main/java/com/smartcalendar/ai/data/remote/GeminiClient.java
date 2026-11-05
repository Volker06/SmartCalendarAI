package com.smartcalendar.ai.data.remote;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class GeminiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";

    private static volatile GeminiApi api;

    private GeminiClient() {}

    public static GeminiApi getApi() {
        if (api == null) {
            synchronized (GeminiClient.class) {
                if (api == null) {
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .build();

                    api = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(GeminiApi.class);
                }
            }
        }
        return api;
    }
}
