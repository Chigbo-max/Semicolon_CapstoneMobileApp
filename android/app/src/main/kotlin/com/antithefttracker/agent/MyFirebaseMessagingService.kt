package com.antithefttracker.agent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.content.Intent
import android.util.Log
import android.content.Context
import android.content.SharedPreferences
import com.antithefttracker.agent.ApiService




class MyFirebaseMessagingService : FirebaseMessagingService(){
    override fun onNewToken(token: String){
        super.onNewToken(token)

         val sharedPref = getSharedPreferences("flutter", MODE_PRIVATE)
         val deviceId = sharedPref.getString("device_id", null)

        if (deviceId != null) {
            ApiService.updateFcmToken(deviceId, token)
        } else {
            Log.e("FCM", "Device ID not found in SharedPreferences")
        }
        }

     override fun onMessageReceived(message: RemoteMessage) {
        Log.d("FCM", "Message data: ${message.data}")
        val command = message.data["command"]
        if (command != null) {
            val intent = Intent("com.antithefttracker.agent.COMMAND").apply {
                setPackage(applicationContext.packageName)
                putExtra("command", command)
            }
            try {
                sendBroadcast(intent)
                Log.d("FCM", "Broadcast sent with command: $command")
            } catch (e: Exception) {
                Log.e("FCM", "Error sending broadcast: ${e.message}", e)
            }
        } else {
            Log.w("FCM", "No command in message data")
        }
    
  
    }
}