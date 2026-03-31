package com.wizpizz.wifiauto.ui.activity

import android.content.Context
import android.net.wifi.WifiManager
import android.widget.SeekBar
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.factory.prefs
import com.wizpizz.wifiauto.R
import com.wizpizz.wifiauto.databinding.ActivityMainBinding
import com.wizpizz.wifiauto.hook.DEFAULT_THRESHOLD
import com.wizpizz.wifiauto.hook.PREF_ENABLED
import com.wizpizz.wifiauto.hook.PREF_THRESHOLD
import com.wizpizz.wifiauto.ui.activity.base.BaseActivity

// SeekBar range: 0 = -90 dBm, 40 = -50 dBm
private const val SEEKBAR_MIN_DBM = -90
private const val SEEKBAR_MAX_DBM = -50

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val prefs by lazy { prefs() }

    override fun onCreate() {
        refreshModuleStatus()
        refreshCurrentRssi()

        // Enable toggle
        val enabledPref = prefs.getBoolean(PREF_ENABLED, true)
        binding.enableSwitch.isChecked = enabledPref
        binding.enableSwitch.setOnCheckedChangeListener { button, isChecked ->
            if (button.isPressed) {
                prefs.native().edit { putBoolean(PREF_ENABLED, isChecked) }
            }
        }

        // Threshold SeekBar
        val thresholdDbm = prefs.getInt(PREF_THRESHOLD, DEFAULT_THRESHOLD)
        val seekProgress = thresholdDbm - SEEKBAR_MIN_DBM  // e.g. -75 - (-90) = 15
        binding.thresholdSeekBar.max = SEEKBAR_MAX_DBM - SEEKBAR_MIN_DBM  // 40
        binding.thresholdSeekBar.progress = seekProgress.coerceIn(0, SEEKBAR_MAX_DBM - SEEKBAR_MIN_DBM)
        updateThresholdLabel(thresholdDbm)

        binding.thresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val dbm = SEEKBAR_MIN_DBM + progress
                updateThresholdLabel(dbm)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val dbm = SEEKBAR_MIN_DBM + seekBar.progress
                prefs.native().edit { putInt(PREF_THRESHOLD, dbm) }
            }
        })
    }

    private fun updateThresholdLabel(dbm: Int) {
        binding.thresholdValueText.text = getString(R.string.threshold_value, dbm)
    }

    private fun refreshCurrentRssi() {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        @Suppress("DEPRECATION")
        val rssi = wm?.connectionInfo?.rssi ?: Int.MIN_VALUE
        binding.currentRssiText.text = if (rssi == Int.MIN_VALUE || rssi == 0) {
            getString(R.string.wifi_not_connected)
        } else {
            getString(R.string.current_rssi, rssi)
        }
    }

    private fun refreshModuleStatus() {
        val activated = YukiHookAPI.Status.isXposedModuleActive
        binding.mainLinStatus.setBackgroundResource(
            if (activated) R.drawable.bg_green_round else R.drawable.bg_dark_round
        )
        binding.mainTextStatus.text = getString(
            if (activated) R.string.module_is_activated else R.string.module_not_activated
        )
    }
}
