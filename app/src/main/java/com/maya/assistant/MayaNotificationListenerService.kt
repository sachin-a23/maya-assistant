package com.maya.assistant

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Reads incoming notifications (WhatsApp, SMS, etc.) and speaks a
 * short summary out loud via Maya's foreground service.
 *
 * Like the accessibility service, this ONLY works after the user
 * manually enables it in Settings > Notification access > Maya
 * Assistant. Android never allows an app to enable this for itself.
 */
class MayaNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Don't announce Maya's own persistent "running" notification.
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        if (title.isBlank() && text.isBlank()) return

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        val summary = listOf(appName, title, text).filter { it.isNotBlank() }.joinToString(": ")
        MayaForegroundService.instance?.speak(summary)
    }
}
