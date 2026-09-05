package ai.eigent.mobile.runtime

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import kotlin.concurrent.thread

/**
 * Owns the local runtime independently of the Activity.
 * The Activity is UI only; closing/minimising it must not tear down inference.
 */
class RuntimeService : Service() {
    private lateinit var jobs: JobEngine
    private lateinit var notifications: NotificationController
    private var pythonThread: Thread? = null
    private var llamaThread: Thread? = null
    private var llamaProcess: Process? = null
    @Volatile private var stopping = false

    override fun onCreate() {
        super.onCreate()
        jobs = JobEngine(this)
        notifications = NotificationController(this)
        val recovered = jobs.recoverLatest()
        val state = recovered ?: jobs.create("runtime")
        promote(state)
        startLocalRuntimes()
    }

    private fun promote(state: JobState) {
        ServiceCompat.startForeground(
            this,
            NotificationController.NOTIFICATION_ID,
            notifications.running(state),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun startLocalRuntimes() {
        if (!Python.isStarted()) Python.start(AndroidPlatform(this))
        val python = Python.getInstance()
        val module = python.getModule("mobile_bootstrap")
        pythonThread = thread(start = true, name = "eigent-python") {
            try { module.callAttr("start_backend", 5001) }
            catch (t: Throwable) { if (!stopping) updateFailure("Python backend: ${t.message}") }
        }

        val exe = File(applicationInfo.nativeLibraryDir, "libllama-server.so")
        if (!exe.exists()) {
            updateFailure("llama-server native executable is missing")
            return
        }
        exe.setExecutable(true)
        val models = File(filesDir, "models").apply { mkdirs() }
        llamaThread = thread(start = true, name = "eigent-llama") {
            try {
                llamaProcess = ProcessBuilder(
                    exe.absolutePath,
                    "--host", "127.0.0.1",
                    "--port", "8080",
                    "--models-dir", models.absolutePath
                ).directory(filesDir)
                    .redirectErrorStream(true)
                    .apply { environment()["LD_LIBRARY_PATH"] = applicationInfo.nativeLibraryDir }
                    .start()
                llamaProcess?.inputStream?.bufferedReader()?.useLines { lines ->
                    lines.forEach { line ->
                        if (!stopping) android.util.Log.d("EigentLlama", line)
                    }
                }
            } catch (t: Throwable) { if (!stopping) updateFailure("llama-server: ${t.message}") }
        }
    }

    private fun updateFailure(message: String) {
        val current = jobs.current() ?: return
        jobs.update(current.copy(status = "FAILED", error = message, message = message))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Deliberately do not stop. The service is the runtime owner.
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        stopping = true
        // The Activity never owns this process. Service destruction is an actual runtime stop.
        try { llamaProcess?.destroy() } catch (_: Throwable) {}
        llamaProcess = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
