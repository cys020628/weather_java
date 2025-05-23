package com.avad.openweatherapp.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.avad.openweatherapp.R;
import com.avad.openweatherapp.data.model.ForecastResponse;
import com.avad.openweatherapp.repository.WeatherRepository;
import com.avad.openweatherapp.ui.view.MainActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * WeatherUpdateService
 * - 위젯의 날씨 정보를 백그라운드에서 업데이트
 * - JobIntentService를 사용 (SDK 33 이상은 WorkManager 권장)
 */
public class WeatherUpdateService extends JobIntentService {

    private static final int JOB_ID = 1001;

    public static final String EXTRA_REFRESH = "refresh_requested";

    /**
     * 인-메모리 캐시: summaryForecastByDay 결과
     * 앱 전체 런타임 동안 유지
     */
    private static List<ForecastResponse.ForecastItem> cachedForecastList = null;

    /**
     * 서비스 실행 등록
     * @param context 컨텍스트
     * @param refreshRequested 강제 새로고침 플래그
     */
    public static void enqueueWork(Context context, boolean refreshRequested) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_REFRESH, refreshRequested);  // 새로고침 여부 전달
        enqueueWork(context, WeatherUpdateService.class, JOB_ID, intent);
    }

    /**
     * 기본 enqueueWork (refresh 없이)
     * @param context 컨텍스트
     */
    public static void enqueueWork(Context context) {
        enqueueWork(context, false);
    }

    /**
     * 백그라운드에서 실행되는 메인 로직
     * - SharedPreferences에서 인덱스와 저장된 예보 JSON 로드
     * - 리모트(강제 새로고침) 또는 캐시된 데이터로 위젯 업데이트
     */
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        Gson gson = new Gson();
        boolean refreshRequested = intent.getBooleanExtra(EXTRA_REFRESH, false);

        // SharedPreferences에서 현재 forecast_index 읽기
        int index = prefs.getInt("forecast_index", 0);

        // 캐시가 비어있으면 JSON 문자열에서 복원
        if (cachedForecastList == null) {
            String json = prefs.getString("forecast_list", "");
            Type type = new TypeToken<List<ForecastResponse.ForecastItem>>() {}.getType();
            cachedForecastList = gson.fromJson(json, type);
        }

        // RemoteViews와 AppWidgetManager 초기화
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.weather_widget);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName widget = new ComponentName(this, WeatherWidget.class);

        // refreshRequested=true인 경우 로딩 표시
        if (refreshRequested) {
            views.setViewVisibility(R.id.widget_progress_bar, View.VISIBLE);
            manager.updateAppWidget(widget, views);
        }

        // 현재 인덱스 기반으로 위젯 UI 업데이트
        updateWidgetUI(this, views, index);
        setupWidgetButtons(this, views);
        manager.updateAppWidget(widget, views);

        // 서버에서 새로고침 요청 시
        if (refreshRequested) {
            double lat = Double.longBitsToDouble(prefs.getLong("lat", 0));
            double lon = Double.longBitsToDouble(prefs.getLong("lon", 0));

            new WeatherRepository().getWeekendWeather(lat, lon, new retrofit2.Callback<ForecastResponse>() {
                @Override
                public void onResponse(Call<ForecastResponse> call, Response<ForecastResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // 서버로부터 받은 3시간 단위 rawList → 요약 리스트
                        List<ForecastResponse.ForecastItem> summarized = summarizeForecastByDay(response.body().list);

                        // 새로고침 후 인덱스 초기화 (오늘)
                        int refreshedIndex = 0;

                        // 캐시 및 SharedPreferences 갱신
                        cachedForecastList = summarized;
                        prefs.edit()
                                .putString("forecast_list", gson.toJson(summarized))
                                .putInt("forecast_index", refreshedIndex)
                                .apply();

                        // 위젯 UI 재갱신
                        updateWidgetUI(getApplicationContext(), views, refreshedIndex);
                        setupWidgetButtons(getApplicationContext(), views);
                    }
                    // 로딩바 숨김 처리
                    views.setViewVisibility(R.id.widget_progress_bar, View.GONE);
                    manager.updateAppWidget(widget, views);
                }

                @Override
                public void onFailure(Call<ForecastResponse> call, Throwable t) {
                    Log.e("WeatherService", "Forecast fetch failed", t);
                    views.setViewVisibility(R.id.widget_progress_bar, View.GONE);
                    manager.updateAppWidget(widget, views);
                }
            });
        }
    }

    /**
     * 인덱스 기반으로 위젯 UI 컨텐츠 세팅
     * @param context 컨텍스트
     * @param views RemoteViews 인스턴스
     * @param index 표시할 ForecastItem 인덱스
     */
    private static void updateWidgetUI(Context context, RemoteViews views, int index) {
        // 캐시가 null이거나 인덱스 범위 초과 시 리턴
        if (cachedForecastList == null || cachedForecastList.size() <= index) return;

        // ForecastItem 가져오기
        ForecastResponse.ForecastItem item = cachedForecastList.get(index);

        // 텍스트뷰 세팅
        views.setTextViewText(R.id.widget_high_temp_tv, String.format("최고 %.1f°C", item.main.temp_max));
        views.setTextViewText(R.id.widget_low_temp_tv, String.format("최저 %.1f°C", item.main.temp_min));
        views.setTextViewText(R.id.widget_date_tv, getFormattedDateWithDayOfWeek(item.dtTxt));
        views.setTextViewText(R.id.widget_description_tv, getShortDescription(item.weather.get(0).description));

        // 아이콘 비동기 로드
        if (item.weather != null && !item.weather.isEmpty()) {
            String iconUrl = "https://openweathermap.org/img/wn/" + item.weather.get(0).icon + "@2x.png";
            AppWidgetTarget target = new AppWidgetTarget(
                    context, R.id.widget_icon_iv, views,
                    new ComponentName(context, WeatherWidget.class));
            Glide.with(context.getApplicationContext())
                    .asBitmap()
                    .load(iconUrl)
                    .into(target);
        }

        // 이전/다음 화살표 visibility 제어
        views.setViewVisibility(R.id.widget_prev_day_iv, index == 0 ? View.INVISIBLE : View.VISIBLE);
        views.setViewVisibility(R.id.widget_next_day_iv,
                index == cachedForecastList.size() - 1 ? View.INVISIBLE : View.VISIBLE);
    }

    /**
     * 위젯 버튼 클릭 리스너 설정
     * - PREV/NEXT: AlarmReceiver broadcast
     * - REFRESH: 이 서비스 재실행
     * - 위젯 전체 클릭: MainActivity 실행
     */
    private static void setupWidgetButtons(Context context, RemoteViews views) {
        // 이전
        PendingIntent prev = PendingIntent.getBroadcast(context, 0,
                new Intent(context, AlarmReceiver.class).setAction("com.avad.widget.PREV"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_prev_day_iv, prev);

        // 다음
        PendingIntent next = PendingIntent.getBroadcast(context, 1,
                new Intent(context, AlarmReceiver.class).setAction("com.avad.widget.NEXT"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_next_day_iv, next);

        // 새로고침
        PendingIntent refresh = PendingIntent.getBroadcast(context, 2,
                new Intent(context, AlarmReceiver.class).setAction("com.avad.widget.REFRESH"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh_iv, refresh);

        // 위젯 클릭 시 MainActivity 실행
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int idx = prefs.getInt("forecast_index", 0);
        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.putExtra("widget_index", idx);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent mainIntent = PendingIntent.getActivity(
                context, 3, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_ll, mainIntent);
    }

    /**
     * rawList를 날짜별로 그룹화 후 최소/최대 기온으로 요약
     * @param rawList 3시간 단위 원본 리스트
     * @return 요약 리스트 (최대 5일)
     */
    private List<ForecastResponse.ForecastItem> summarizeForecastByDay(List<ForecastResponse.ForecastItem> rawList) {
        // 날짜별로 ForecastItem을 담을 맵 생성
        //    LinkedHashMap으로 선언: rawList 순서(시계열)를 유지하기 위함
        Map<String, List<ForecastResponse.ForecastItem>> grouped = new LinkedHashMap<>();

        // rawList 순회하며 날짜별로 그룹핑
        for (ForecastResponse.ForecastItem item : rawList) {
            // dtTxt는 "YYYY-MM-DD HH:mm:ss" 형태이므로, 공백 기준으로 split 후 앞부분(날짜)만 추출
            String dateKey = item.dtTxt.split(" ")[0];

            // 해당 날짜Key가 없으면 새 ArrayList 생성, 있으면 기존 리스트 반환
            grouped.computeIfAbsent(dateKey, k -> new ArrayList<>())
                    // 2-3) 현재 아이템을 해당 날짜 그룹에 추가
                    .add(item);
        }

        // 최종 요약 결과를 담을 리스트
        List<ForecastResponse.ForecastItem> result = new ArrayList<>();

        // 날짜별 그룹맵 순회
        for (Map.Entry<String, List<ForecastResponse.ForecastItem>> entry : grouped.entrySet()) {
            //그 날짜 그룹에서 최저·최고 기온을 계산하기 위한 초기화
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;

            //날씨 정보(아이콘, 설명 등)를 가져오기 위해 첫 번째 아이템을 샘플로 참조
            ForecastResponse.ForecastItem sample = entry.getValue().get(0);

            // 해당 날짜 그룹 안의 모든 ForecastItem 순회하며
            for (ForecastResponse.ForecastItem it : entry.getValue()) {
                // temp_min이 현재 min보다 작으면 업데이트
                if (it.main.temp_min < min) {
                    min = it.main.temp_min;
                }
                // temp_max이 현재 max보다 크면 업데이트
                if (it.main.temp_max > max) {
                    max = it.main.temp_max;
                }
            }

            // 계산된 최저·최고 기온으로 새로운 요약 ForecastItem 객체 생성
            ForecastResponse.ForecastItem summary = new ForecastResponse.ForecastItem();
            //  dtTxt는 해당 날짜 00:00:00으로 설정 (데이터 식별용)
            summary.dtTxt = entry.getKey() + " 00:00:00";
            //  main 필드 초기화 후 온도 세팅
            summary.main = new ForecastResponse.Main();
            summary.main.temp_min = min;
            summary.main.temp_max = max;
            //  날씨 정보는 샘플의 weather(리스트) 그대로 복사
            summary.weather = sample.weather;

            // 결과 리스트에 추가
            result.add(summary);

            // 최대 5일치까지만 요약하도록 제한
            if (result.size() == 5) {
                break;
            }
        }

        return result;
    }

    /**
     * "yyyy-MM-dd HH:mm:ss" 문자열을 "MM월 dd일(요일)" 포맷으로 변환
     */
    public static String getFormattedDateWithDayOfWeek(String dateStr) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
            Date date = input.parse(dateStr);
            SimpleDateFormat dateFmt = new SimpleDateFormat("MM월 dd일", Locale.KOREA);
            String part = dateFmt.format(date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            String[] days = {"일","월","화","수","목","금","토"};
            return part + "(" + days[cal.get(Calendar.DAY_OF_WEEK)-1] + ")";
        } catch (ParseException e) {
            e.printStackTrace();
            return "날짜 오류";
        }
    }

    /**
     * 원본 설명 문자열을 간략화
     */
    private static String getShortDescription(String original) {
        if (original.contains("맑음")) return "맑음";
        if (original.contains("흐림") || original.contains("구름")) return "흐림";
        if (original.contains("비")) return original.contains("강한") ? "폭우" : "비";
        if (original.contains("눈")) return original.contains("강한") ? "폭설" : "눈";
        if (original.contains("천둥") || original.contains("뇌우")) return "뇌우";
        if (original.contains("안개")) return "안개";
        return original;
    }

    /**
     * 빠른 위젯 업데이트 (서비스 재실행 없이 인덱스 변경 시)
     */
    public static void quickUpdateWidget(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        int index = prefs.getInt("forecast_index", 0);
        if (cachedForecastList == null) {
            String json = prefs.getString("forecast_list", "");
            Type type = new TypeToken<List<ForecastResponse.ForecastItem>>() {}.getType();
            cachedForecastList = gson.fromJson(json, type);
        }
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, WeatherWidget.class);
        updateWidgetUI(context, views, index);
        setupWidgetButtons(context, views);
        manager.updateAppWidget(widget, views);
    }
}