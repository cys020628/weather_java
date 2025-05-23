package com.avad.openweatherapp.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.avad.openweatherapp.data.model.ForecastResponse;
import com.avad.openweatherapp.databinding.ItemWeekendWeather2Binding;
import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * ForecastDailyAdapter
 * - 5일치 요약 날씨 정보(요일, 날짜, 아이콘, 최저/최고 기온)를 RecyclerView로 표시
 * - 클릭 시 OnDateClickListener를 통해 날짜 인덱스를 전달
 */
public class ForecastDailyAdapter extends RecyclerView.Adapter<ForecastDailyAdapter.ViewHolder> {

    /**
     * 날짜 클릭 이벤트 전달 인터페이스
     */
    public interface OnDateClickListener {
        /**
         * @param position 0=오늘, 1=내일, 2=모레, …
         */
        void onDateClick(int position);
    }

    private final OnDateClickListener listener;

    // 데이터 표시용 리스트들
    private final List<String> dayList = new ArrayList<>();    // 요일 ("월", "화"…)
    private final List<String> dateList = new ArrayList<>();   // 날짜 (MM.dd)
    private final List<String> iconList = new ArrayList<>();   // 아이콘 코드
    private final List<Double> minList = new ArrayList<>();    // 최저 기온 (°C)
    private final List<Double> maxList = new ArrayList<>();    // 최고 기온 (°C)

    /**
     * @param rawList  3시간 단위 전체 예보 리스트
     * @param listener 날짜 클릭 콜백
     */
    public ForecastDailyAdapter(
            List<ForecastResponse.ForecastItem> rawList,
            OnDateClickListener listener
    ) {
        this.listener = listener;
        buildDailyData(rawList);
    }

    /**
     * 날짜별 그룹화 및 요약 데이터 빌드
     * 날짜별로 그룹핑 (키: yyyy-MM-dd)
     * 첫 5일만 처리
     * 각 날짜 그룹에서 temp_min, temp_max 필드로 최저/최고 기온 계산
     * 첫 아이콘(item.weather.get(0).icon) 사용
     */
    private void buildDailyData(List<ForecastResponse.ForecastItem> list) {
        // TreeMap 사용으로 날짜 키 정렬 보장
        Map<String, List<ForecastResponse.ForecastItem>> grouped = new TreeMap<>();
        SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outDateFmt = new SimpleDateFormat("MM.dd", Locale.getDefault());
        SimpleDateFormat outDayFmt = new SimpleDateFormat("E", Locale.getDefault());

        // 날짜별로 리스트 그룹핑
        for (ForecastResponse.ForecastItem f : list) {
            String dateKey = f.dtTxt.split(" ")[0];  // "2025-05-21"
            grouped.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(f);
        }

        // 첫 5일만 처리
        int count = 0;
        for (String dateKey : grouped.keySet()) {
            if (count++ >= 5) break;  // 최대 5일치
            List<ForecastResponse.ForecastItem> dayItems = grouped.get(dateKey);
            try {
                // 날짜 문자열 파싱
                Date d = inFmt.parse(dateKey);

                // 초기 최저/최고 기온 설정: 첫 요소의 temp_min/temp_max 사용
                double minT = dayItems.get(0).main.temp_min;
                double maxT = dayItems.get(0).main.temp_max;
                // 대표 아이콘: 그룹의 첫 항목
                String icon = dayItems.get(0).weather.get(0).icon;

                // 그룹 내 모든 항목 순회하며 최저/최고 갱신
                for (ForecastResponse.ForecastItem item : dayItems) {
                    // temp_min, temp_max 필드를 사용해 비교
                    minT = Math.min(minT, item.main.temp_min);
                    maxT = Math.max(maxT, item.main.temp_max);
                }

                // 가공 결과를 각 리스트에 추가
                dayList.add(outDayFmt.format(d));      // 요일 (예: "수")
                dateList.add(outDateFmt.format(d));    // 날짜 (예: "05.21")
                iconList.add(icon);                    // 아이콘 코드
                minList.add(minT);                     // 최저 기온
                maxList.add(maxT);                     // 최고 기온

            } catch (ParseException e) {
                Log.e("ForecastAdapter", "날짜 파싱 오류", e);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType
    ) {
        // ViewBinding으로 아이템 레이아웃 인플레이트
        ItemWeekendWeather2Binding binding = ItemWeekendWeather2Binding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, int position
    ) {
        // 요일/날짜 텍스트 세팅
        holder.binding.dayTv.setText(dayList.get(position));
        holder.binding.dateTv.setText(dateList.get(position));

        // 날씨 아이콘 비동기 로드
        String iconUrl = "https://openweathermap.org/img/wn/"
                + iconList.get(position) + "@2x.png";
        Glide.with(holder.binding.getRoot().getContext())
                .load(iconUrl)
                .into(holder.binding.ivWeatherIcon);

        // 최저/최고 온도 텍스트 세팅
        holder.binding.tvWeatherText.setText(
                String.format(Locale.getDefault(), "%.0f℃", minList.get(position))
        );
        holder.binding.tvWeatherText2.setText(
                String.format(Locale.getDefault(), "%.0f℃", maxList.get(position))
        );

        // 클릭 리스너 연결
        holder.binding.itemWeather2Ll.setOnClickListener(v ->
                listener.onDateClick(position)
        );
    }

    @Override
    public int getItemCount() {
        return dayList.size();  // 실제 표시될 아이템 수
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemWeekendWeather2Binding binding;

        ViewHolder(ItemWeekendWeather2Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
