package com.wizpizz.wifiauto.hook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
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
    private var lastDisconnectTimeMs = 0L
    private const val COOLDOWN_MS = 5 * 60 * 1000L

    @Volatile
    private var wasDisconnectedByUs = false
    private var scanRunnable: Runnable? = null
    private var scanHandler: Handler? = null
    private const val SCAN_INTERVAL_MS = 30 * 1000L

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

                    // RSSI receiver: disconnect when signal is weak
                    val rssiReceiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context, intent: Intent) {
                            if (intent.action != WifiManager.RSSI_CHANGED_ACTION) return
                            val rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, Int.MIN_VALUE)
                            if (rssi == Int.MIN_VALUE) return

                            Log.d(TAG, "RSSI=$rssi dBm, threshold=$threshold dBm")

                            if (rssi < threshold) {
                                // If we already disconnected, check if Android auto-reconnected to a weak AP
                                if (wasDisconnectedByUs) {
                                    Log.d(TAG, "Still weak after reconnect, disconnecting again")
                                    wifiManager.disconnect()
                                    return
                                }

                                val now = System.currentTimeMillis()
                                if (now - lastDisconnectTimeMs < COOLDOWN_MS) {
                                    Log.d(TAG, "Cooldown active, skipping disconnect")
                                    return
                                }

                                Log.i(TAG, "Signal weak ($rssi < $threshold dBm), disconnecting WiFi")
                                wifiManager.disconnect()
                                lastDisconnectTimeMs = now

                                if (autoReconnect) {
                                    wasDisconnectedByUs = true
                                    startScanPolling(handler, wifiManager, threshold)
                                }
                            }
                        }
                    }
                    context.registerReceiver(rssiReceiver, IntentFilter(WifiManager.RSSI_CHANGED_ACTION))

                    // Scan results receiver: reconnect when signal recovers
                    if (autoReconnect) {
                        val scanReceiver = object : BroadcastReceiver() {
                            override fun onReceive(ctx: Context, intent: Intent) {
                                if (intent.action != WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) return
                                if (!wasDisconnectedByUs) return

                                @Suppress("DEPRECATION")
                                val connInfo = wifiManager.connectionInfo
                                val connected = connInfo?.networkId != -1
                                if (connected) return  // already reconnected, wait for RSSI check

                                @Suppress("DEPRECATION")
                                val results = wifiManager.scanResults ?: return
                                @Suppress("DEPRECATION")
                                val configuredSsids = wifiManager.configuredNetworks
                                    ?.map { it.SSID }?.toSet() ?: return

                                val strongEnough = results.any { sr ->
                                    sr.SSID in configuredSsids && sr.level >= threshold
                                }

                                if (strongEnough) {
                                    Log.i(TAG, "Signal recovered, reconnecting WiFi")
                                    wasDisconnectedByUs = false
                                    stopScanPolling()
                                    wifiManager.reconnect()
                                }
                            }
                        }

                        // Network state receiver: if user manually reconnects, cancel our tracking
                        val networkReceiver = object : BroadcastReceiver() {
                            override fun onReceive(ctx: Context, intent: Intent) {
                                if (intent.action != WifiManager.NETWORK_STATE_CHANGED_ACTION) return
                                if (!wasDisconnectedByUs) return
                                @Suppress("DEPRECATION")
                                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                                if (networkInfo?.isConnected == true) {
                                    Log.d(TAG, "User manually reconnected, cancelling tracking")
                                    wasDisconnectedByUs = false
                                    stopScanPolling()
                                }
                            }
                        }

                        val filter = IntentFilter().apply {
                            addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                        }
                        context.registerReceiver(scanReceiver, filter)
                        context.registerReceiver(networkReceiver, IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION))
                    }

                    Log.d(TAG, "BroadcastReceivers registered")
                }
            }
        }
    }

    private fun startScanPolling(handler: Handler, wifiManager: WifiManager, threshold: Int) {
        stopScanPolling()
        scanHandler = handler
        val runnable = object : Runnable {
            override fun run() {
                if (wasDisconnectedByUs) {
                    Log.d(TAG, "Polling scan for signal recovery...")
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
