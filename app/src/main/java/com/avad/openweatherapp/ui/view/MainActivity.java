package com.avad.openweatherapp.ui.view;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.avad.openweatherapp.R;
import com.avad.openweatherapp.adapter.ForecastDailyAdapter;
import com.avad.openweatherapp.adapter.WeekendWeatherAdapter;
import com.avad.openweatherapp.data.model.ForecastResponse;
import com.avad.openweatherapp.databinding.ActivityMainBinding;
import com.avad.openweatherapp.ui.viewmodel.WeatherViewModel;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private WeatherViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;

    /**
     * 위젯에서 전달된 클릭 인덱스 저장 (0=오늘, 1=내일, 2=모레…)
     */
    private int widgetIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // binding 설정
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 위젯에서 전달된 인덱스 읽기
        widgetIndex = getIntent().getIntExtra("widget_index", 0);

        // 시스템 바 여백 처리
        initSystemBars();

        // ViewModel 초기화
        initViewModel();

        // 위치 클라이언트 초기화 & 권한 요청 -> 성공 시 데이터 가져오기
        initLocationClient();
        requestLocationPermission();

        // LiveData 관찰 및 UI 세팅 (위젯 인덱스 적용)
        observeWeatherData();
    }

    /**
     * 이미 실행된 Activity에 다시 호출되었을 때도
     * 새 Intent의 widgetIndex 를 갱신해주기 위한 onNewIntent override
     */
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        widgetIndex = intent.getIntExtra("widget_index", 0);
    }

    /**
     * 시스템 바 높이만큼 루트 뷰에 padding 추가
     */
    private void initSystemBars() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });
    }

    /**
     * ViewModel 초기화
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this)
                .get(WeatherViewModel.class);
    }

    /**
     * FusedLocationProviderClient 인스턴스 획득
     */
    private void initLocationClient() {
        fusedLocationClient = LocationServices
                .getFusedLocationProviderClient(this);
    }

    /**
     * 위치 권한 요청
     * - 허용 시 getCurrentLocation()
     * - 거부 시 토스트 후 종료
     */
    private void requestLocationPermission() {
        ActivityResultLauncher<String> launcher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) getCurrentLocation();
                    else {
                        Toast.makeText(this,
                                "위치 권한이 필요합니다.\n설정에서 허용해주세요.",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                });

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * 마지막 위치 정보 조회 후
     * - SharedPreferences 에 위도/경도 저장
     * - 뷰모델 fetchForecast() 호출
     * - 주소(행정구/동) 텍스트뷰에 세팅
     */
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, loc -> {
                    if (loc != null) {
                        SharedPreferences prefs =
                                this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putLong("lat", Double.doubleToRawLongBits(loc.getLatitude()))
                                .putLong("lon", Double.doubleToRawLongBits(loc.getLongitude()))
                                .apply();

                        viewModel.fetchForecast(loc.getLatitude(), loc.getLongitude());
                        showAddressFromLatLng(loc.getLatitude(), loc.getLongitude());
                    }
                });
    }

    /**
     * Geocoder 로 행정구/동 문자열 얻어와서 TextView 및 SharedPreferences에 저장
     */
    private void showAddressFromLatLng(double lat, double lon) {
        try {
            List<Address> list = new Geocoder(this, Locale.KOREA)
                    .getFromLocation(lat, lon, 1);
            if (!list.isEmpty()) {
                Address a = list.get(0);
                String addr = a.getAdminArea() + " " + a.getSubLocality();
                binding.locationTv.setText(addr);
                getSharedPreferences("prefs", Context.MODE_PRIVATE)
                        .edit().putString("location", addr).apply();
            }
        } catch (IOException e) {
            Log.e("Geocoder", e.getMessage());
        }
    }

    /**
     * LiveData 관찰 및 위젯 클릭 인덱스 적용 로직
     * 1) groupedForecast 로드 시에는 아무 것도 하지 않음
     * 2) 첫 selectedDate emit 시 widgetIndex > 0 이면 moveDate 호출
     * 3) 이후 selectedDate 변경 시에만 updateForecastUI() 실행
     */
    private void observeWeatherData() {
        // groupedForecast 단순 로드 트리거용
        viewModel.getGroupedForecast().observe(this, grouped -> {
        });

        // selectedDate 변경 시
        viewModel.getSelectedDate().observe(this, date -> {
            if (date == null) return;

            // 최초 emit 된 dateList[0] 일 때 widgetIndex 적용
            if (widgetIndex > 0) {
                viewModel.moveDate(widgetIndex);
                widgetIndex = 0;
                return;  // moveDate 로 다시 emit 될 때 UI 갱신 분기 수행
            }

            // 최종 selectedDate가 확정시 UI 갱신
            binding.selectedDateTv.setText(date);
            updateForecastUI();
        });
    }

    /**
     * 현재 selectedDate 기준으로
     * - 3시간 단위 예보(가로 RecyclerView)
     * - 5일 요약 세로 RecyclerView
     * - summary 정보 갱신
     */
    private void updateForecastUI() {
        Map<String, List<ForecastResponse.ForecastItem>> grouped =
                viewModel.getGroupedForecast().getValue();
        String selDate = viewModel.getSelectedDate().getValue();
        if (grouped == null || selDate == null) return;

        // 3시간 단위 예보: Horizontal
        List<ForecastResponse.ForecastItem> list = grouped.get(selDate);
        if (list != null) {
            binding.weatherRv.setLayoutManager(
                    new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            binding.weatherRv.setAdapter(new WeekendWeatherAdapter(list));
            binding.loadingLl.setVisibility(GONE);
            binding.topDivider.setVisibility(VISIBLE);
            binding.currentTempTv.setVisibility(VISIBLE);
            binding.locationTv.setVisibility(VISIBLE);
            updateSummaryWeather(list);
        }

        // 5일 요약: Vertical
        List<ForecastResponse.ForecastItem> fullList = new ArrayList<>();
        for (List<ForecastResponse.ForecastItem> dayList : grouped.values()) {
            fullList.addAll(dayList);
        }
        binding.weekRv.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.weekRv.setAdapter(
                new ForecastDailyAdapter(fullList, pos -> viewModel.moveDate(pos))
        );
    }

    /**
     * summary 영역 (현재 시각과 가장 가까운 예보 아이템)
     * 텍스트·아이콘·배경 업데이트
     */
    private void updateSummaryWeather(List<ForecastResponse.ForecastItem> list) {
        String now = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date());
        ForecastResponse.ForecastItem near = null;
        long minDiff = Long.MAX_VALUE;

        for (ForecastResponse.ForecastItem item : list) {
            try {
                String t = item.dtTxt.split(" ")[1].substring(0, 5);
                Date ft = new SimpleDateFormat("HH:mm", Locale.getDefault()).parse(t);
                Date nt = new SimpleDateFormat("HH:mm", Locale.getDefault()).parse(now);
                long diff = Math.abs(ft.getTime() - nt.getTime());
                if (diff < minDiff) {
                    minDiff = diff;
                    near = item;
                }
            } catch (ParseException e) {
                Log.e("TimeParse", e.getMessage());
            }
        }

        if (near != null && !near.weather.isEmpty()) {
            binding.tempTv.setText(Math.round(near.main.temp) + "°C");
            binding.descriptionTv.setText(near.weather.get(0).description);
            Glide.with(this)
                    .load("https://openweathermap.org/img/wn/"
                            + near.weather.get(0).icon + "@2x.png")
                    .into(binding.iconIv);
            binding.humidityTv.setText("습도: " + near.main.humidity + "%");
            binding.feelsLikeTv.setText("체감: " + near.main.feelsLike + "°C");

            int bgRes = getBgRes(near);

            binding.weatherBackgroundV.setAlpha(0.2f);
            binding.weatherBackgroundV.setBackgroundResource(bgRes);
        }
    }

    /**
     * 가장 가까운 날씨 아이템을 받아 백그라운드 변경
     * @param near
     * @return
     */
    private static int getBgRes(ForecastResponse.ForecastItem near) {
        int bgRes;
        String desc = near.weather.get(0).description;
        if (desc.contains("맑음")) bgRes = R.drawable.ic_sunny_bg;
        else if (desc.contains("구름") ||
                desc.contains("흐림")) bgRes = R.drawable.ic_wind_bg;
        else if (desc.contains("비") ||
                desc.contains("소나기") ||
                desc.contains("우박")) bgRes = R.drawable.ic_rain_bg;
        else if (desc.contains("눈") ||
                desc.contains("진눈깨비")) bgRes = R.drawable.ic_snow_bg;
        else if (desc.contains("번개") ||
                desc.contains("천둥")) bgRes = R.drawable.ic_lightning_bg;
        else bgRes = 0;
        return bgRes;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}