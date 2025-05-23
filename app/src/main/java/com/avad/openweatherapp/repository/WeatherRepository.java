package com.avad.openweatherapp.repository;

import com.avad.openweatherapp.BuildConfig;
import com.avad.openweatherapp.data.model.ForecastResponse;
import com.avad.openweatherapp.data.network.RetrofitClient;
import com.avad.openweatherapp.data.network.WeatherApiService;

import retrofit2.Callback;

public class WeatherRepository {

    private final WeatherApiService apiService;

    /**
     * OpenWeatherMap API 공통 파라미터
     */
    private static final String UNIT = "metric";  // 섭씨
    private static final String LANG = "kr";      // 한글 응답

    public WeatherRepository() {
        this.apiService = RetrofitClient.getInstance();
    }

    /**
     * 예보 데이터 조회 (3시간 간격, 최대 5일치)
     */
    public void getWeekendWeather(double lat, double lon, Callback<ForecastResponse> callback) {
        apiService.getWeekendWeather(lat, lon, BuildConfig.BASE_URL, UNIT, LANG)
                .enqueue(callback);
    }
}