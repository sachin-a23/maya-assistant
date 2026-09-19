package com.maya.assistant

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * Keeps Maya alive in the background: hosts the text-to-speech engine
 * (so any part of the app can make Maya "speak"), and watches for
 * incoming calls to announce them.
 *
 * Runs as a foreground service so Android does not kill it — this
 * shows a persistent notification the whole time it's active, which
 * is required by Android and lets the user know Maya is running.
 */
class MayaForegroundService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val CHANNEL_ID = "maya_foreground_channel"
        const val NOTIFICATION_ID = 1001

        var instance: MayaForegroundService? = null
            private set
    }

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var telephonyManager: TelephonyManager? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        tts = TextToSpeech(this, this)
        startForegroundWithNotification()
        watchCallState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY: if Android kills this service under memory
        // pressure, it will try to restart it automatically.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            ttsReady = true
        }
    }

    /** Makes Maya speak a line out loud. Safe to call from anywhere. */
    fun speak(text: String) {
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Maya Assistant", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Maya is running")
            .setContentText("Listening for notifications and calls")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun watchCallState() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return // permission not granted yet; MainActivity asks for it
        }

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        @Suppress("DEPRECATION")
        telephonyManager?.listen(object : PhoneStateListener() {
            @Suppress("DEPRECATION")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        val caller = phoneNumber?.takeIf { it.isNotBlank() } ?: "unknown number"
                        speak("Incoming call from $caller")
                    }
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        instance = null
    }
}
