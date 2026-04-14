package com.kyas.wolkandhold.data.api;

import com.kyas.wolkandhold.data.api.requests.LoginRequest;
import com.kyas.wolkandhold.data.api.requests.PolygonRequest;
import com.kyas.wolkandhold.data.api.requests.SignupRequest;
import com.kyas.wolkandhold.data.api.response.AuthResponse;
import com.kyas.wolkandhold.data.api.response.PolygonResponse;
import com.kyas.wolkandhold.data.api.response.ValidateTokenResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest loginRequest);

    @POST("/api/auth/signup")
    Call<AuthResponse> signup(@Body SignupRequest signupRequest);

    @GET("/api/auth/me")
    Call<ValidateTokenResponse> me();

    @GET("/api/polygons/{lat}/{lon}/{radius}")
    Call<List<PolygonResponse>> getPolygonsInRadius(@Path("lat") double latitude,
                                                    @Path("lon") double longitude,
                                                    @Path("radius") double radius);

    @POST("/api/polygons")
    Call<PolygonResponse> insertPolygon(@Body PolygonRequest polygonRequest);

}
