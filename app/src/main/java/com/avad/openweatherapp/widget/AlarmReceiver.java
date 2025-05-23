package com.avad.openweatherapp.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * AlarmReceiver
 * - 위젯의 이전/다음/새로고침 버튼 클릭 이벤트 수신
 * - forecast_index 값을 증가/감소/초기화하고
 *   빠른 위젯 업데이트 또는 전체 서비스 재실행 트리거
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // SharedPreferences에서 현재 forecast_index 읽기 (0~4)
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int index = prefs.getInt("forecast_index", 0);
        String action = intent.getAction();

        // NEXT 버튼 클릭: 인덱스 +1 -> 5일치 순환
        if ("com.avad.widget.NEXT".equals(action)) {
            index = (index + 1) % 5;
            prefs.edit().putInt("forecast_index", index).apply();
            // 빠른 UI 업데이트 (서비스 전체 실행 없이)
            WeatherUpdateService.quickUpdateWidget(context);

            // PREV 버튼 클릭: 인덱스 -1 -> 0 미만 시 4로 순환
        } else if ("com.avad.widget.PREV".equals(action)) {
            index = (index - 1 + 5) % 5;
            prefs.edit().putInt("forecast_index", index).apply();
            // 빠른 UI 업데이트
            WeatherUpdateService.quickUpdateWidget(context);

            // REFRESH 버튼 클릭: 전체 서비스 재실행 (새 데이터 fetch)
        } else if ("com.avad.widget.REFRESH".equals(action)) {
            WeatherUpdateService.enqueueWork(context, true);
        }
    }
}
