package com.antithefttracker.agent

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object ApiService {
    private const val TAG = "ApiService"
    private val client = OkHttpClient()
    private const val FCM_URL = "https://45329aa2c129.ngrok-free.app/api/v1/fcm/token"

    fun updateFcmToken(deviceId: String, token: String) {
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("token", token)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(FCM_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to send FCM token: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.i(TAG, "FCM token updated successfully: ${response.code}")
            }
        })
    }
}
