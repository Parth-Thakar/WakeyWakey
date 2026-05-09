package com.z.wakey.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.z.wakey.data.AlarmStorage

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return
        AlarmStorage.get(context).getAll()
            .filter { it.isEnabled }
            .forEach { AlarmScheduler.schedule(context, it) }
    }
}
