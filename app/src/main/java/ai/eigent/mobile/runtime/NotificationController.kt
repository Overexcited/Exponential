package ai.eigent.mobile.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

class NotificationController(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "eigent_runtime"
        const val NOTIFICATION_ID = 4101
    }

    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Eigent background work",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Long-running local AI and document jobs" }
        )
    }

    fun running(state: JobState): Notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("Eigent is working")
        .setContentText(state.message.ifBlank { state.kind })
        .setProgress(100, state.progress.coerceIn(0, 100), state.progress !in 1..99)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .build()

    fun completed(state: JobState): Notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle("Eigent job completed")
        .setContentText(state.message.ifBlank { "Completed" })
        .setAutoCancel(true)
        .build()
}
