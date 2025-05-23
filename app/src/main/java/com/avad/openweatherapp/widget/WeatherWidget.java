package com.avad.openweatherapp.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public class WeatherWidget extends AppWidgetProvider {

    // 추가 및 업데이트
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 최초 실행 및 강제 리프래시
        WeatherUpdateService.enqueueWork(context,true);
    }

    // 첫실행시만
    @Override
    public void onEnabled(Context context) {
        // 1시간 마다 자동 갱신
        AlarmScheduler.scheduleRepeatingUpdate(context);
    }

    // 위젯이 제거될 경우
    @Override
    public void onDisabled(Context context) {
        // 알람 해제
        AlarmScheduler.cancelAlarm(context);
    }
}