package com.example.widgettest

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.widgettest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var hasRequestedPermission = false

    private val prefs by lazy {
        getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SharedPreferences에서 코인 수 가져오기 (없으면 100으로 초기화)
        val sharedPrefs = this.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
        var coinCount = sharedPrefs.getInt("coin_count", 0)

        binding.coin.text = coinCount.toString()

        if (!checkExactAlarmPermission()) {
            // ✅ permission_requested 키가 false일 경우에만 안내
            val alreadyRequested = prefs.getBoolean("permission_requested", false)
            if (!alreadyRequested) {
                showExactAlarmDialog()
            }
        } else {
            // 권한 있음 → 바로 알람 등록
            MyWidgetProvider().scheduleMidnightUpdate(this)
        }
    }

    override fun onResume() {
        super.onResume()

        if (hasRequestedPermission) {
            if (checkExactAlarmPermission()) {
                MyWidgetProvider().scheduleMidnightUpdate(this)
            }
            hasRequestedPermission = false
        }
    }

    private fun checkExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun showExactAlarmDialog() {
        AlertDialog.Builder(this)
            .setTitle("정확한 알람 권한 필요")
            .setMessage("매일 자정에 위젯을 자동 업데이트하려면 '정확한 알람' 권한이 필요합니다.\n설정으로 이동하시겠어요?")
            .setPositiveButton("허용하러 가기") { _, _ ->
                requestExactAlarmPermission()
            }
            .setNegativeButton("취소") { _, _ -> }
            .setCancelable(false)
            .show()

        // ✅ 안내 다이얼로그를 보여준 것을 기록
        prefs.edit().putBoolean("permission_requested", true).apply()
    }

    private fun requestExactAlarmPermission() {
        hasRequestedPermission = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}
