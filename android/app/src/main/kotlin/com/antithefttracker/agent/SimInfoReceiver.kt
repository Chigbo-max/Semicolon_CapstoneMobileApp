package com.antithefttracker.agent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager


class SimInfoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val phoneNumber = telephonyManager.line1Number
        val simIccid = telephonyManager.simSerialNumber
        val carrierName = telephonyManager.networkOperatorName
    }
}