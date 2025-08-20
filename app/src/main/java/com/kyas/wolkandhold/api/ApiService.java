package com.kyas.wolkandhold.api;

import com.kyas.wolkandhold.api.requests.LoginRequest;
import com.kyas.wolkandhold.api.requests.PolygonRequest;
import com.kyas.wolkandhold.api.response.AuthResponse;
import com.kyas.wolkandhold.api.response.PolygonResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @POST
    Call<AuthResponse> login(@Body LoginRequest loginRequest);

    @POST
    Call<AuthResponse> signup(@Body LoginRequest loginRequest);

    @GET
    Call<String> me();

    @GET("/api/polygons/{lat}/{lon}/{radius}")
    Call<List<PolygonResponse>> getPolygonsInRadius(@Path("lat") double latitude,
                                                    @Path("lon") double longitude,
                                                    @Path("radius") double radius);

    @POST("/api/polygons")
    Call<PolygonResponse> upsertPolygon(@Body PolygonRequest polygonRequest);

}
