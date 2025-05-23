package com.avad.openweatherapp.ui.viewmodel;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.avad.openweatherapp.data.model.ForecastResponse;
import com.avad.openweatherapp.repository.WeatherRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * WeatherViewModel
 * - OpenWeather 3시간 간격 예보 데이터를 받아
 *   날짜별로 그룹화(groupedForecast)하고,
 *   현재 선택된 날짜(selectedDate)를 관리
 */
public class WeatherViewModel extends ViewModel {
    private final WeatherRepository repository = new WeatherRepository();

    /**
     * 해당 날짜의 ForecastItem 리스트
     * */
    private final MutableLiveData<Map<String, List<ForecastResponse.ForecastItem>>> groupedForecast = new MutableLiveData<>();

    /** 현재 화면에 표시할 날짜 문자열 (ex: "2025-05-20") */
    private final MutableLiveData<String> selectedDate = new MutableLiveData<>();

    /**
     * 5일치 예보(3시간 단위) 요청
     * - 응답 수신 시:
     *   1) dtTxt의 날짜 부분(yyyy-MM-dd)으로 그룹화
     *   2) groupedForecast LiveData에 setValue
     *   3) groupedForecast 최초 비어 있지 않을 때, 첫 날짜(dateList[0])를 selectedDate에 초기 설정
     *
     * @param lat 위도
     * @param lon 경도
     */
    public void fetchForecast(double lat, double lon) {
        repository.getWeekendWeather(lat, lon, new Callback<ForecastResponse>() {
            @Override
            public void onResponse(@NonNull Call<ForecastResponse> call,
                                   @NonNull Response<ForecastResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("WeatherViewModel", "API 요청 URL: " + call.request().url());

                    // 날짜별 그룹화
                    Map<String, List<ForecastResponse.ForecastItem>> grouped = new LinkedHashMap<>();
                    for (ForecastResponse.ForecastItem item : response.body().list) {
                        String date = item.dtTxt.split(" ")[0];  // yyyy-MM-dd
                        grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(item);
                    }

                    // LiveData에 반영 -> groupedForecast 옵저버 트리거
                    groupedForecast.setValue(grouped);

                    // 최초 그룹이 있으면 첫 날짜로 selectedDate 초기화
                    if (!grouped.isEmpty()) {
                        String firstDate = new ArrayList<>(grouped.keySet()).get(0);
                        selectedDate.setValue(firstDate);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ForecastResponse> call, @NonNull Throwable t) {
                Log.e("WeatherViewModel", "예보 API 호출 실패: " + t.getMessage());
            }
        });
    }

    /**
     * groupedForecast LiveData 접근자
     */
    public LiveData<Map<String, List<ForecastResponse.ForecastItem>>> getGroupedForecast() {
        return groupedForecast;
    }

    /** selectedDate LiveData 접근자 */
    public LiveData<String> getSelectedDate() {
        return selectedDate;
    }

    /**
     * 특정 인덱스(value)에 해당하는 날짜로 selectedDate 변경
     * - map: groupedForecast의 현재 값
     * - value: 0=첫째 날짜, 1=둘째 날 ...
     *
     * @param value 이동할 날짜 인덱스
     */
    public void moveDate(int value) {
        Map<String, List<ForecastResponse.ForecastItem>> map = groupedForecast.getValue();
        String current = selectedDate.getValue();

        // 데이터가 아직 로드되지 않았거나 selectedDate가 없으면 무시
        if (map == null || current == null) return;

        List<String> dateList = new ArrayList<>(map.keySet());
        if (value >= 0 && value < dateList.size()) {
            String newDate = dateList.get(value);
            selectedDate.setValue(newDate);
        }
    }
}