package com.wizpizz.wifiauto.hook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Handler
import android.util.Log
import com.highcapable.yukihookapi.hook.param.PackageParam

private const val TAG = "WifiAutoDisable"

const val PREF_ENABLED = "wifi_auto_disable_enabled"
const val PREF_THRESHOLD = "wifi_rssi_threshold"
const val PREF_AUTO_RECONNECT = "wifi_auto_reconnect_enabled"
const val DEFAULT_THRESHOLD = -75  // dBm

object WifiAutoDisableHook {

    @Volatile
    private var lastDisableTimeMs = 0L
    private const val COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes between disables
    private const val SCAN_INTERVAL_MS = 30 * 1000L // scan every 30 seconds

    @Volatile
    private var wasDisabledByUs = false
    private var scanRunnable: Runnable? = null
    private var scanHandler: Handler? = null

    fun apply(packageParam: PackageParam) {
        packageParam.apply {
            val enabled = prefs.getBoolean(PREF_ENABLED, true)
            if (!enabled) {
                Log.d(TAG, "WiFi auto-disable is turned off, skipping")
                return
            }

            val threshold = prefs.getInt(PREF_THRESHOLD, DEFAULT_THRESHOLD)
            val autoReconnect = prefs.getBoolean(PREF_AUTO_RECONNECT, false)
            Log.d(TAG, "WifiAutoDisableHook loaded, threshold=$threshold dBm, autoReconnect=$autoReconnect")

            onAppLifecycle {
                onCreate {
                    val context: Context = this
                    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    if (wifiManager == null) {
                        Log.e(TAG, "WifiManager not available")
                        return@onCreate
                    }

                    val handler = Handler(context.mainLooper)

                    // RSSI receiver: disable WiFi when signal is weak
                    val rssiReceiver = object : BroadcastReceiver() {
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
                                if (autoReconnect) {
                                    wasDisabledByUs = true
                                    startScanPolling(handler, wifiManager)
                                }
                            }
                        }
                    }
                    context.registerReceiver(rssiReceiver, IntentFilter(WifiManager.RSSI_CHANGED_ACTION))

                    // Scan results receiver: re-enable WiFi when signal recovers
                    if (autoReconnect) {
                        val scanReceiver = object : BroadcastReceiver() {
                            override fun onReceive(ctx: Context, intent: Intent) {
                                if (intent.action != WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) return
                                if (!wasDisabledByUs) return
                                if (wifiManager.isWifiEnabled) {
                                    // WiFi was turned on by someone else
                                    wasDisabledByUs = false
                                    stopScanPolling()
                                    return
                                }
                                @Suppress("DEPRECATION")
                                val results = wifiManager.scanResults ?: return
                                @Suppress("DEPRECATION")
                                val configuredSsids = wifiManager.configuredNetworks
                                    ?.map { it.SSID }
                                    ?.toSet() ?: return

                                val strongEnough = results.any { sr ->
                                    sr.SSID in configuredSsids && sr.level >= threshold
                                }

                                if (strongEnough) {
                                    Log.i(TAG, "Signal recovered, re-enabling WiFi")
                                    wasDisabledByUs = false
                                    stopScanPolling()
                                    @Suppress("DEPRECATION")
                                    wifiManager.setWifiEnabled(true)
                                }
                            }
                        }
                        context.registerReceiver(scanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                    }

                    Log.d(TAG, "BroadcastReceivers registered")
                }
            }
        }
    }

    private fun startScanPolling(handler: Handler, wifiManager: WifiManager) {
        stopScanPolling()
        scanHandler = handler
        val runnable = object : Runnable {
            override fun run() {
                if (wasDisabledByUs) {
                    Log.d(TAG, "Polling scan for WiFi recovery...")
                    @Suppress("DEPRECATION")
                    wifiManager.startScan()
                    handler.postDelayed(this, SCAN_INTERVAL_MS)
                }
            }
        }
        scanRunnable = runnable
        handler.postDelayed(runnable, SCAN_INTERVAL_MS)
    }

    private fun stopScanPolling() {
        val r = scanRunnable ?: return
        scanHandler?.removeCallbacks(r)
        scanRunnable = null
        scanHandler = null
    }
}
