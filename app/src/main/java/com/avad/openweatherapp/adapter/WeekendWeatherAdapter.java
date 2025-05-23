package com.avad.openweatherapp.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.avad.openweatherapp.data.model.ForecastResponse;
import com.avad.openweatherapp.databinding.ItemWeekendWeatherBinding;
import com.bumptech.glide.Glide;

import java.util.List;

/**
 * WeekendWeatherAdapter
 * - 3시간 단위의 예보 리스트를 가로 RecyclerView로 표시
 * - 시간, 온도, 설명, 아이콘을 각 아이템에 바인딩
 */
public class WeekendWeatherAdapter extends RecyclerView.Adapter<WeekendWeatherAdapter.ForecastViewHolder> {

    /**
     * 3시간 단위 ForecastItem 리스트
     */
    private final List<ForecastResponse.ForecastItem> forecastList;

    /**
     * @param forecastList 예보 데이터 리스트
     */
    public WeekendWeatherAdapter(List<ForecastResponse.ForecastItem> forecastList) {
        this.forecastList = forecastList;
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ItemWeekendWeatherBinding으로 레이아웃 인플레이트
        ItemWeekendWeatherBinding binding = ItemWeekendWeatherBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ForecastViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        ForecastResponse.ForecastItem item = forecastList.get(position);

        // 시간 텍스트 추출: "yyyy-MM-dd HH:mm:ss" → "HH:mm"
        String time = item.dtTxt.split(" ")[1].substring(0, 5);

        // 기온 텍스트: 반올림 후 °C 추가
        String tempText = Math.round(item.main.temp) + "°C";

        // 설명 텍스트: weather 리스트 첫 번째 description
        String contentText = item.weather.get(0).description;

        // 아이콘 URL 구성
        String iconUrl = "https://openweathermap.org/img/wn/"
                + item.weather.get(0).icon + "@2x.png";

        // 뷰 바인딩
        holder.binding.weatherTimeTv.setText(time);
        holder.binding.weatherTempTv.setText(tempText);
        holder.binding.weatherContentTv.setText(contentText);

        // Glide로 아이콘 비동기 로드
        Glide.with(holder.binding.getRoot().getContext())
                .load(iconUrl)
                .into(holder.binding.weatherIconIv);
    }

    @Override
    public int getItemCount() {
        return forecastList.size();
    }

    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        final ItemWeekendWeatherBinding binding;

        public ForecastViewHolder(@NonNull ItemWeekendWeatherBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
