package com.avad.openweatherapp.data.network;

import com.avad.openweatherapp.data.model.ForecastResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {

    // 5일동안 3시간 간격 40개 데이터 날씨
    @GET("data/2.5/forecast")
    Call<ForecastResponse> getWeekendWeather(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String units,
            @Query("lang") String lang
    );
}