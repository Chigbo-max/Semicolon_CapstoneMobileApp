package com.antithefttracker.agent

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import android.app.admin.DevicePolicyManager
import android.content.ComponentName

import androidx.core.content.ContextCompat
import org.json.JSONObject
import okhttp3.RequestBody.Companion.toRequestBody


import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator


class CommandService : Service() {
    private lateinit var commandReceiver: BroadcastReceiver

    override fun onCreate() {
        super.onCreate()
        Log.d("CommandService", "CommandService onCreate called")
        
        val notification = NotificationCompat.Builder(this, "command_channel")
            .setContentTitle("Anti-Theft Service")
            .setContentText("Running in background")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        try {
            startForeground(1, notification)
            Log.d("CommandService", "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e("CommandService", "Error starting foreground service: ${e.message}", e)
        }

        commandReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("CommandService", "Broadcast received: ${intent?.action}")
                Log.d("CommandService", "Intent extras: ${intent?.extras}")
                val cmd = intent?.getStringExtra("command") ?: run {
                    Log.e("CommandService", "No command extra in broadcast")
                    return
                }
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AntiTheft:CommandLock")
                wakeLock.acquire(10_000)
                try {
                                    Log.d("CommandService", "Processing command: $cmd")
                Log.d("CommandService", "Command type: ${cmd::class.java.simpleName}")
                when (cmd) {
                        "lock" -> {
                            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                            val componentName = ComponentName(this@CommandService, MyDeviceAdminReceiver::class.java)
                            Log.d("CommandService", "Device admin active: ${dpm.isAdminActive(componentName)}")
                            if (dpm.isAdminActive(componentName)) {
                                try {
                                    // Try to lock the device immediately
                                    Log.d("CommandService", "Executing lockNow()...")
                                    dpm.lockNow()
                                    Log.d("CommandService", "lockNow() executed successfully")
                                    
                                    // Wait a moment for the lock to take effect
                                    Thread.sleep(500)
                                    
                                    // Launch the lock activity
                                    val lockIntent = Intent(this@CommandService, LockActivity::class.java)
                                    lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    startActivity(lockIntent)
                                    Log.d("CommandService", "Device locked and LockActivity launched")
                                    
                                    // Show notification to indicate the device is locked
                                    showLockNotification()
                                    
                                    // Also send a broadcast to the Flutter app to update UI
                                    val flutterIntent = Intent("com.antithefttracker.agent.LOCK_EXECUTED")
                                    sendBroadcast(flutterIntent)
                                    
                                } catch (e: Exception) {
                                    Log.e("CommandService", "Error locking device: ${e.message}", e)
                                    // Try alternative locking method
                                    try {
                                        Log.d("CommandService", "Trying alternative lock method...")
                                        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            if (!keyguardManager.isKeyguardLocked) {
                                                Log.d("CommandService", "Attempting to lock via keyguard...")
                                                // This is a fallback method
                                            }
                                        }
                                    } catch (e2: Exception) {
                                        Log.e("CommandService", "Alternative lock method also failed: ${e2.message}", e2)
                                    }
                                }
                            } else {
                                Log.e("CommandService", "Device admin not active")
                                // Try to prompt for device admin again
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device admin to lock the device remotely.")
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }
                        }
                        "wipe" -> {
                            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                            val componentName = ComponentName(this@CommandService, MyDeviceAdminReceiver::class.java)
                            Log.d("CommandService", "Device admin active: ${dpm.isAdminActive(componentName)}")
                            if (dpm.isAdminActive(componentName)) {
                                dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE or DevicePolicyManager.WIPE_RESET_PROTECTION_DATA)
                                Log.d("CommandService", "Wipe initiated")
                            } else {
                                Log.e("CommandService", "Device admin not active")
                            }
                        }
                       
    "theft_report" -> {
        Log.d("CommandService", "Handling theft report command")

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(2000)
        }

        showTheftNotification()

    }

    else -> {
        Log.w("CommandService", "Unknown command received: $cmd")
    }
                    }
                } catch (e: Exception) {
                    Log.e("CommandService", "Error processing command: ${e.message}", e)
                } finally {
                    wakeLock.release()
                }
            }
        }
        try {
            registerReceiver(commandReceiver, IntentFilter("com.antithefttracker.agent.COMMAND"))
            Log.d("CommandService", "Broadcast receiver registered successfully for: com.antithefttracker.agent.COMMAND")
        } catch (e: Exception) {
            Log.e("CommandService", "Error registering receiver: ${e.message}", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CommandService", "onStartCommand called")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(commandReceiver)
            Log.d("CommandService", "Broadcast receiver unregistered")
        } catch (e: Exception) {
            Log.e("CommandService", "Error unregistering receiver: ${e.message}", e)
        }
        Log.d("CommandService", "Service destroyed")
    }

    private fun showLockNotification() {
        val channelId = "lock_alert"
        val notificationIntent = Intent(this, LockActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Device Locked")
            .setContentText("Your device has been locked remotely.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Lock Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts about device locking"
            }
            manager.createNotificationChannel(channel)
        }

        manager.notify(1002, notification.build())
    }

    private fun showTheftNotification() {
    val channelId = "theft_alert"
    val notificationIntent = Intent(this, CommandService::class.java)
    val pendingIntent = PendingIntent.getService(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

    val notification = NotificationCompat.Builder(this, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("Device Compromised")
        .setContentText("Your device may have been stolen.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(false)
        .setContentIntent(pendingIntent)

    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Theft Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alerts about possible device theft"
        }
        manager.createNotificationChannel(channel)
    }

    manager.notify(1001, notification.build())
}




}