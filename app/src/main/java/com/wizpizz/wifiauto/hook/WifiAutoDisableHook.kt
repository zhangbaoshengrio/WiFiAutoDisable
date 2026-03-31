package com.wizpizz.wifiauto.hook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.util.Log
import com.highcapable.yukihookapi.hook.param.PackageParam

private const val TAG = "WifiAutoDisable"

const val PREF_ENABLED = "wifi_auto_disable_enabled"
const val PREF_THRESHOLD = "wifi_rssi_threshold"
const val DEFAULT_THRESHOLD = -75  // dBm

object WifiAutoDisableHook {

    @Volatile
    private var lastDisableTimeMs = 0L
    private const val COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes between disables

    fun apply(packageParam: PackageParam) {
        packageParam.apply {
            val enabled = prefs.getBoolean(PREF_ENABLED, true)
            if (!enabled) {
                Log.d(TAG, "WiFi auto-disable is turned off, skipping")
                return
            }

            val threshold = prefs.getInt(PREF_THRESHOLD, DEFAULT_THRESHOLD)
            Log.d(TAG, "WifiAutoDisableHook loaded, threshold=$threshold dBm")

            onAppLifecycle {
                onCreate {
                    val context: Context = this
                    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    if (wifiManager == null) {
                        Log.e(TAG, "WifiManager not available")
                        return@onCreate
                    }

                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context, intent: Intent) {
                            if (intent.action != WifiManager.RSSI_CHANGED_ACTION) return
                            val rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, Int.MIN_VALUE)
                            if (rssi == Int.MIN_VALUE) return

                            Log.d(TAG, "RSSI=$rssi dBm, threshold=$threshold dBm")

                            if (rssi < threshold) {
                                val now = System.currentTimeMillis()
                                if (now - lastDisableTimeMs < COOLDOWN_MS) {
                                    Log.d(TAG, "Cooldown active, skipping disable")
                                    return
                                }
                                Log.i(TAG, "Signal weak ($rssi < $threshold dBm), disabling WiFi")
                                @Suppress("DEPRECATION")
                                wifiManager.setWifiEnabled(false)
                                lastDisableTimeMs = now
                            }
                        }
                    }

                    context.registerReceiver(receiver, IntentFilter(WifiManager.RSSI_CHANGED_ACTION))
                    Log.d(TAG, "RSSI BroadcastReceiver registered")
                }
            }
        }
    }
}
