# WeatherJava

하루 + 5일간의 날씨를 확인할 수 있는 Android Java 기반 어플리케이션 및 홈 화면 위젯

## Preview

<table>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/808a546a-17ff-41d0-a260-25ec97ef743e" width="400"/></td>
    <td><img src="https://github.com/user-attachments/assets/f1c8838e-a41e-4dc6-955d-2b087789c1c9" width="400"/></td>
  </tr>
  <tr>
    <td align="center">위젯 화면</td>
    <td align="center">홈 화면</td>
  </tr>
</table>

## 기능

- **현재 위치 기반** 오늘 날씨 및 5일 예보 정보 조회
- **홈 화면 위젯** 지원
    - 1×1: 오늘 날씨
    - 4×1: 5일 예보 리스트
- **AlarmManager** 를 이용한 주기적 자동 업데이트
- **Retrofit2 + Gson** 기반 OpenWeather API 연동
- **MVVM** 아키텍처 (ViewModel, Repository 분리)

## 아키텍처 & 디렉터리 구조

```
weather_java/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/weather/
│   │       │   ├── model/
│   │       │   │   └── ForecastResponse.java        // API 응답 모델
│   │       │   ├── network/
│   │       │   │   └── WeatherApiService.java       // Retrofit 인터페이스
│   │       │   ├── repository/
│   │       │   │   └── WeatherRepository.java       // 데이터 처리 로직
│   │       │   ├── ui/
│   │       │   │   ├── MainActivity.java            // 메인 화면
│   │       │   │   └── WeatherViewModel.java        // ViewModel
│   │       │   └── widget/
│   │       │       ├── WeatherWidget.java          // AppWidgetProvider
│   │       │       ├── AlarmReceiver.java          // 업데이트 알람 수신
│   │       │       ├── AlarmScheduler.java         // 알람 스케줄러
│   │       │       └── WeatherUpdateService.java   // 위젯 업데이트 서비스
│   │       └── res/
│   │           ├── layout/
│   │           │   ├── activity_main.xml
│   │           │   ├── widget_today_weather.xml
│   │           │   └── item_weekend_weather.xml
│   │           └── values/
│   │               └── strings.xml                  // API 키, 앱 이름 등
│   └── build.gradle.kts
├── settings.gradle.kts
└── build.gradle.kts

```
