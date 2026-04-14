package com.kyas.wolkandhold.ui.data;

import android.content.SharedPreferences;
import android.util.Log;

import com.kyas.wolkandhold.BuildConfig;
import com.kyas.wolkandhold.data.api.ApiService;
import com.kyas.wolkandhold.data.api.AuthInterceptor;
import com.kyas.wolkandhold.data.api.requests.LoginRequest;
import com.kyas.wolkandhold.data.api.response.AuthResponse;
import com.kyas.wolkandhold.data.api.response.ValidateTokenResponse;
import com.kyas.wolkandhold.ui.data.model.LoggedInUser;

import java.io.IOException;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    private final Retrofit retrofit;
    private final ApiService apiService;

    public LoginDataSource() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public Result<LoggedInUser> login(String username, String password) {

        try {
            Log.d("AUTH", "login: trying logged in");
            Response<AuthResponse> resp = apiService.login(new LoginRequest(username, password)).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                LoggedInUser user =
                        new LoggedInUser(
                                resp.body().getUserId(),
                                resp.body().getUsername(),
                                resp.body().getToken());
                Log.d("AUTH", "We get user while login: " + user);
                return new Result.Success<>(user);
            } else {
                return new Result.Error(new Exception("Error login"));
            }

        } catch (Exception e) {
            Log.e("ERROR AUTH", "Crashed login here: ", e);
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public Result<LoggedInUser> validateToken(SharedPreferences sharedPreferences) {
        try {
            Log.d("AUTH", "validateToken: trying validate token");
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(sharedPreferences))
                    .build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);
            Response<ValidateTokenResponse> resp = apiService.me().execute();
            if (resp.isSuccessful() && resp.body() != null) {
                LoggedInUser user = new LoggedInUser(
                        resp.body().getUserId(),
                        resp.body().getUsername(),
                        sharedPreferences.getString("token", ""));
                Log.d("AUTH", "We get user while validate: " + user);
                return new Result.Success<>(user);
            } else {
                return new Result.Error(new Exception("Error validate"));
            }
        } catch (Exception e) {
            Log.e("ERROR LOGIN", "Crashed validate here: ", e);
            return new Result.Error(new IOException("Error validating", e));
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }
}