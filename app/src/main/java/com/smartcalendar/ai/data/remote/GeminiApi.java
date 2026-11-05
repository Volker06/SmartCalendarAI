package com.smartcalendar.ai.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface GeminiApi {

    @POST("v1beta/models/{model}:generateContent")
    Call<GeminiModels.Response> generate(
            @Header("x-goog-api-key") String apiKey,
            @Path("model") String model,
            @Body GeminiModels.Request body);

    @GET("v1beta/models")
    Call<GeminiModels.ModelList> listModels(
            @Header("x-goog-api-key") String apiKey,
            @Query("pageSize") int pageSize);
}
