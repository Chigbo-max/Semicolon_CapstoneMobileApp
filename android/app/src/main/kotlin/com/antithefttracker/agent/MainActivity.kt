package com.antithefttracker.agent

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val SIM_INFO_CHANNEL = "com.antithefttracker.agent/siminfo"
    private val DEVICE_INFO_CHANNEL = "com.antithefttracker.agent/deviceinfo"
    private val COMMAND_CHANNEL = "com.antithefttracker.agent/command"
    private lateinit var simInfoChannel: MethodChannel
    private lateinit var deviceInfoChannel: MethodChannel
    private lateinit var commandChannel: MethodChannel

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "command_channel",
                "Command Service",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        promptDeviceAdmin()
        promptBatteryOptimization()
        promptPhoneStatePermission()
        promptLocationPermissions()
        startCommandService()
    }

    private fun startCommandService() {
        val intent = Intent(this, CommandService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Log.d("MainActivity", "CommandService started")
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningServices = am.getRunningServices(Integer.MAX_VALUE)
            val isServiceRunning = runningServices.any { it.service.className == "com.antithefttracker.agent.CommandService" }
            if (!isServiceRunning) {
                Log.e("MainActivity", "CommandService not running after start attempt")
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isServiceRunning) {
                        Log.d("MainActivity", "Retrying to start CommandService")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent)
                        } else {
                            startService(intent)
                        }
                    }
                }, 1000)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting CommandService: ${e.message}", e)
        }
    }

    private fun promptPhoneStatePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_PHONE_STATE), 1)
            }
        }
    }

    private fun promptLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ),
                    2
                )
            }
        }
    }

    private fun promptBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    private fun promptDeviceAdmin() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        Log.d("MainActivity", "Device admin active: ${dpm.isAdminActive(componentName)}")
        if (!dpm.isAdminActive(componentName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device admin to lock the device remotely.")
            startActivity(intent)
            Log.d("MainActivity", "Device admin prompt launched")
        } else {
            Log.d("MainActivity", "Device admin is already active")
            // Test the lock functionality
            try {
                val canLock = dpm.isAdminActive(componentName)
                Log.d("MainActivity", "Can lock device: $canLock")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking lock capability: ${e.message}", e)
            }
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        simInfoChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, SIM_INFO_CHANNEL)
        simInfoChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "getPhoneNumber" -> {
                    try {
                        val telephony = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        val number = telephony.line1Number
                        Log.d("SimInfo", "Phone Number: $number")
                        result.success(number)
                    } catch (e: Exception) {
                        Log.e("SimInfo", "Error getting phone number", e)
                        result.error("PHONE_NUMBER_ERROR", "Failed to get phone number", null)
                    }
                }
                else -> result.notImplemented()
            }
        }

        deviceInfoChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, DEVICE_INFO_CHANNEL)
        deviceInfoChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "getImei" -> {
                    try {
                        val telephony = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        val imei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            telephony.imei
                        } else {
                            @Suppress("DEPRECATION")
                            telephony.deviceId
                        }
                        Log.d("DeviceInfo", "IMEI: $imei")
                        result.success(imei)
                    } catch (e: Exception) {
                        Log.e("DeviceInfo", "Error getting IMEI", e)
                        result.error("IMEI_ERROR", "Failed to get IMEI", null)
                    }
                }
                else -> result.notImplemented()
            }
        }

        commandChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, COMMAND_CHANNEL)
        commandChannel.setMethodCallHandler { call, result ->
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(this, MyDeviceAdminReceiver::class.java)

            if (!dpm.isAdminActive(componentName)) {
                promptDeviceAdmin()
                result.error("ADMIN_NOT_ACTIVE", "Device admin not active", null)
                return@setMethodCallHandler
            }

            try {
                when (call.method) {
                    "lockDevice" -> {
                        try {
                            Log.d("CommandChannel", "Attempting to lock device...")
                            dpm.lockNow()
                            Log.d("CommandChannel", "lockNow() called successfully")
                            
                            val intent = Intent(this, LockActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            Log.d("CommandChannel", "Device locked and LockActivity launched")
                            result.success("Device locked and UI launched")
                        } catch (e: Exception) {
                            Log.e("CommandChannel", "Error locking device: ${e.message}", e)
                            result.error("LOCK_ERROR", "Failed to lock device: ${e.message}", null)
                        }
                    }
                    "wipeDevice" -> {
                        dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE or DevicePolicyManager.WIPE_RESET_PROTECTION_DATA)
                        result.success("Wipe initiated")
                    }
                    else -> result.notImplemented()
                }
            } catch (e: Exception) {
                Log.e("CommandChannel", "Error processing command: ${e.message}", e)
                result.error("COMMAND_ERROR", "Failed: ${e.message}", null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        simInfoChannel.setMethodCallHandler(null)
        deviceInfoChannel.setMethodCallHandler(null)
        commandChannel.setMethodCallHandler(null)
    }
}