package com.example.widgettest

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class MyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Log.d("WidgetProvider", "onUpdate 호출됨")
        updateWidget(context)
        scheduleMidnightUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d("WidgetProvider", "onReceive: ${intent.action}")

        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {

            //코인 증가
            val sharedPrefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
            var coinCount = sharedPrefs.getInt("coin_count", 100)
            coinCount += 1
            sharedPrefs.edit().putInt("coin_count", coinCount).apply()
            Log.d("CoinDebug", "증가됨: $coinCount")

            updateWidget(context)
            scheduleMidnightUpdate(context)
        }
    }

    private fun updateWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, MyWidgetProvider::class.java))

        val sharedPrefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
        var coinCount = sharedPrefs.getInt("coin_count", 100)

        val cal = Calendar.getInstance()

        // 날짜 + 시간
        val dateTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        // 요일별 식단
        val dietTexts = mapOf(
            Calendar.MONDAY to "된장찌개, 불고기, 쌀밥",
            Calendar.TUESDAY to "김치찌개, 제육볶음, 쌀밥",
            Calendar.WEDNESDAY to "순두부찌개, 고등어구이, 보리밥",
            Calendar.THURSDAY to "미역국, 닭갈비, 쌀밥",
            Calendar.FRIDAY to "부대찌개, 갈비구이, 잡곡밥",
            Calendar.SATURDAY to "짬뽕라면, 김치전, 쌀밥",
            Calendar.SUNDAY to "떡국, 잡채, 쌀밥"
        )
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val diet = dietTexts[dayOfWeek] ?: "식단 없음"

        for (appWidgetId in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
                setTextViewText(R.id.diet_text, "$dateTimeStr\n$diet")
                setTextViewText(R.id.coin_text, "$coinCount Coins")

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.button, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    fun scheduleMidnightUpdate(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 32)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, MyWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, MyWidgetProvider::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ 여기에 위의 완성된 코드 블록 삽입!!
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (manager.canScheduleExactAlarms()) {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d("AlarmTest", "정확한 알람 예약됨 (Android 12+)")
                } else {
                    Log.w("AlarmTest", "⚠ 정확한 알람 권한 없음 → 알람 예약 생략")
                }
            } else {
                manager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d("AlarmTest", "정확한 알람 예약됨 (Android 12 미만)")
            }
        } catch (e: SecurityException) {
            Log.e("AlarmTest", "알람 예약 실패: ${e.message}")
        }
    }
    //이게 맞나?

}
