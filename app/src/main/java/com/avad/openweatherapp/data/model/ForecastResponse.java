package com.avad.openweatherapp.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ForecastResponse {

    @SerializedName("list")
    public List<ForecastItem> list;

    public static class ForecastItem {
        @SerializedName("dt_txt")
        public String dtTxt;

        @SerializedName("main")
        public Main main;

        @SerializedName("weather")
        public List<Weather> weather;
    }

    public static class Main {
        @SerializedName("temp")
        public float temp;

        @SerializedName("feels_like")
        public double feelsLike;

        @SerializedName("humidity")
        public int humidity;

        @SerializedName("temp_min")
        public double temp_min;

        @SerializedName("temp_max")
        public double temp_max;
    }

    public static class Weather {
        @SerializedName("description")
        public String description;

        @SerializedName("icon")
        public String icon;
    }
}