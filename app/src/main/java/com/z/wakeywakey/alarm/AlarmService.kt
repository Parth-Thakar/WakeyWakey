package com.z.wakeywakey.alarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.z.wakeywakey.AlarmRingActivity
import com.z.wakeywakey.R
import com.z.wakeywakey.data.AlarmStorage

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    companion object {
        const val CHANNEL_ID = "wakey_alarm_channel"
        const val NOTIFICATION_ID = 9001
        const val ACTION_STOP_ALARM = "com.z.wakeywakey.STOP_ALARM"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            stopAlarm()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getIntExtra("alarm_id", -1) ?: -1
        if (alarmId == -1) { stopSelf(); return START_NOT_STICKY }

        val alarm = AlarmStorage.get(applicationContext).getById(alarmId)

        if (alarm != null) {
            AlarmScheduler.rescheduleAfterFired(applicationContext, alarm)

            val ringIntent = Intent(applicationContext, AlarmRingActivity::class.java).apply {
                putExtra("alarm_id", alarmId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val fullScreenPending = PendingIntent.getActivity(
                applicationContext, alarmId, ringIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            startForeground(NOTIFICATION_ID, buildNotification(alarm.label.ifEmpty { "Alarm" }, fullScreenPending))

            val ringtoneUri = if (alarm.ringtonePath.isNotEmpty())
                Uri.parse(alarm.ringtonePath)
            else
                Settings.System.DEFAULT_ALARM_ALERT_URI

            playRingtone(ringtoneUri, alarm.isVibrate)
            startActivity(ringIntent)
            AlarmReceiver.releaseWakeLock()
        } else {
            stopSelf()
        }

        return START_STICKY
    }

    private fun playRingtone(uri: Uri, vibrate: Boolean) {
        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = true
                prepare()
                start()
            }
        }.onFailure {
            runCatching {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(applicationContext, Settings.System.DEFAULT_ALARM_ALERT_URI)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        }

        if (vibrate) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0))
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(label: String, fullScreenIntent: PendingIntent): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.alarm_channel_desc)
            enableVibration(false)
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mediaPlayer?.release()
        vibrator?.cancel()
        super.onDestroy()
    }
}
