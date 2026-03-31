package com.wizpizz.wifiauto.ui.activity

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.factory.prefs
import com.wizpizz.wifiauto.R
import com.wizpizz.wifiauto.hook.DEFAULT_THRESHOLD
import com.wizpizz.wifiauto.hook.PREF_ENABLED
import com.wizpizz.wifiauto.hook.PREF_THRESHOLD
import com.wizpizz.wifiauto.ui.activity.base.BaseActivity

private const val SEEKBAR_MIN_DBM = -90
private const val SEEKBAR_MAX_DBM = -50

@Suppress("DEPRECATION")
class MainActivity : BaseActivity() {

    private val prefs by lazy { prefs() }

    override fun onCreate() {
        setContentView(R.layout.activity_main)

        val statusCard = findViewById<android.view.View>(R.id.main_lin_status)
        val statusText = findViewById<TextView>(R.id.main_text_status)
        val rssiText = findViewById<TextView>(R.id.current_rssi_text)
        val enableSwitch = findViewById<Switch>(R.id.enable_switch)
        val seekBar = findViewById<SeekBar>(R.id.threshold_seek_bar)
        val thresholdText = findViewById<TextView>(R.id.threshold_value_text)

        // Module status
        val activated = YukiHookAPI.Status.isXposedModuleActive
        statusCard.setBackgroundResource(if (activated) R.drawable.bg_green_round else R.drawable.bg_dark_round)
        statusText.text = getString(if (activated) R.string.module_is_activated else R.string.module_not_activated)

        // Current RSSI
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val rssi = wm?.connectionInfo?.rssi ?: Int.MIN_VALUE
        rssiText.text = if (rssi == Int.MIN_VALUE || rssi == 0) getString(R.string.wifi_not_connected)
                        else getString(R.string.current_rssi, rssi)

        // Enable toggle
        enableSwitch.isChecked = prefs.getBoolean(PREF_ENABLED, true)
        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.native().edit { putBoolean(PREF_ENABLED, isChecked) }
        }

        // Threshold SeekBar
        val thresholdDbm = prefs.getInt(PREF_THRESHOLD, DEFAULT_THRESHOLD)
        seekBar.max = SEEKBAR_MAX_DBM - SEEKBAR_MIN_DBM
        seekBar.progress = (thresholdDbm - SEEKBAR_MIN_DBM).coerceIn(0, seekBar.max)
        thresholdText.text = getString(R.string.threshold_value, thresholdDbm)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                thresholdText.text = getString(R.string.threshold_value, SEEKBAR_MIN_DBM + progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                prefs.native().edit { putInt(PREF_THRESHOLD, SEEKBAR_MIN_DBM + sb.progress) }
            }
        })
    }
}
