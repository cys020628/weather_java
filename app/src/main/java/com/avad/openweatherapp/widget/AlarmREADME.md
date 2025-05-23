# 🌦️ 날씨 위젯 시스템 동작 구조 (JobIntentService 기반)

---

## 📦 주요 컴포넌트 요약

| 컴포넌트              | 역할                                               |
|-----------------------|----------------------------------------------------|
| `WeatherWidget`       | 위젯 정의 및 수동 업데이트 트리거                  |
| `AlarmReceiver`       | 위젯 버튼(◀️ ▶️ 🔄) 클릭 이벤트 수신                |
| `AlarmScheduler`      | 주기적 위젯 갱신 예약 (AlarmManager 사용)          |
| `WeatherUpdateService`| 실질적인 위젯 UI 업데이트 및 날씨 데이터 fetch 처리 |
| `JobIntentService`    | 백그라운드에서 안전하게 작업 처리 (⚠️ Deprecated)  |

---

## 🔄 전체 흐름 구조

### 1. 위젯이 처음 추가될 때
- `WeatherWidget.onUpdate()` 호출
- → `WeatherUpdateService.enqueueWork(context, true)` 실행
- → 위젯에 첫 데이터 표시 + SharedPreferences 초기화

---

### 2. 사용자가 위젯에서 버튼을 클릭할 때

#### ◀️ 또는 ▶️ 화살표 클릭 시
- `AlarmReceiver.onReceive()` 호출
    - index 값을 +1 또는 -1
    - SharedPreferences에 저장
    - 👉 `WeatherUpdateService.quickUpdateWidget()` 실행
    - UI만 즉시 빠르게 반영

#### 🔄 새로고침 버튼 클릭 시
- `AlarmReceiver.onReceive()` → `WeatherUpdateService.enqueueWork(context, true)` 호출
    - 서버로부터 최신 날씨 받아옴
    - 로딩바 표시 → 데이터 반영 후 로딩바 숨김

---

### 3. 주기적으로 자동 새로고침 할 때

- `AlarmScheduler.scheduleRepeatingUpdate()` 통해
    - AlarmManager로 `PendingIntent` 예약 (예: 1시간마다)
    - 인텐트 수신 → `AlarmReceiver` → `WeatherUpdateService.enqueueWork(context, true)`

---

## 💡 핵심 클래스 구조

### `WeatherUpdateService extends JobIntentService`

- `onHandleWork(Intent intent)`:
    - refresh 여부 판단
    - `cachedForecastList` 사용해 UI 업데이트
    - 새로고침이면 Retrofit으로 날씨 fetch
    - 결과 캐시로 저장

---

### `quickUpdateWidget(Context context)`

- 빠른 UI 반영 전용
- forecast 인덱스만 바뀔 경우
- `RemoteViews`만 즉시 업데이트

---

## 🧨 문제점 (현재 구조의 한계)

| 문제                           | 설명                                                              |
|--------------------------------|-------------------------------------------------------------------|
| `JobIntentService`는 Deprecated| Android 12+ 이상에서 더 이상 권장되지 않음                        |
| 최신 백그라운드 정책과 충돌 우려 | Android 10 이상에서는 백그라운드 작업 제약 많음                   |
| `WorkManager` 권장             | 안정성과 유연성 측면에서 WorkManager로 대체 추천됨               |

---

## ✅ 결론

- 지금 구조는 성능적으로 최적화가 잘 되어 있고 안정적임
- 하지만 `JobIntentService`는 **향후 Android 버전 업 대응을 위해 WorkManager로 교체 권장**
- 교체 시 `enqueueWork()` → `WorkManager.enqueue()`로 전환 가능

---
