package io.github.joemk88.showonwatch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import java.util.concurrent.Executors

/**
 * Short-lived foreground service: shrinks and sends one shared item at a time,
 * showing progress in a notification, then stops itself.
 */
class SendService : Service() {

    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.data
        if (uri == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val isVideo = intent?.type?.startsWith("video/") == true
        goForeground(if (isVideo) "Preparing video…" else "Sending image…")

        worker.execute {
            val message = try {
                if (isVideo) WatchSender.sendVideo(applicationContext, uri) { updateStatus(it) }
                else WatchSender.sendImage(applicationContext, uri)
            } catch (e: Exception) {
                "Couldn't send: ${e.message ?: e.javaClass.simpleName}"
            }
            main.post {
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                stopSelf(startId)   // only stops once the newest job has finished
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    private fun goForeground(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Sending to watch", NotificationManager.IMPORTANCE_LOW)
        )
        val n = buildNotification(text)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun updateStatus(text: String) {
        getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .build()

    companion object {
        private const val CHANNEL_ID = "send"
        private const val NOTIFICATION_ID = 1
    }
}
