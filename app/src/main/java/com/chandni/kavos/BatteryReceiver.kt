package com.chandni.kavos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class BatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BATTERY_LOW) {
            Toast.makeText(context, "Battery low! Sending location to guardians…", Toast.LENGTH_LONG).show()
            SOSHelper.sendSOS(context, "Battery Low Auto-Alert")
        }
    }
}

