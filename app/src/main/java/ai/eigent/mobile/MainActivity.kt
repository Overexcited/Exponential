package ai.eigent.mobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import ai.eigent.mobile.runtime.RuntimeService

class MainActivity : AppCompatActivity() {
    private val backendPort = 5001
    private lateinit var root: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        startRuntime()
        showLauncher()
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    private fun startRuntime() {
        val intent = Intent(this, RuntimeService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun showLauncher() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        status = TextView(this).apply {
            text = "Starting local Eigent runtime..."
            textSize = 18f
        }
        root.addView(status)
        val open = Button(this).apply {
            text = "Open Eigent"
            isEnabled = false
            setOnClickListener { openEigent() }
        }
        root.addView(open)
        root.addView(Button(this).apply {
            text = "Local Models"
            setOnClickListener { showModels() }
        })
        root.addView(Button(this).apply {
            text = "Runtime Diagnostics"
            setOnClickListener { showDiagnostics() }
        })
        setContentView(root)
        waitForBackend(open)
    }

    private fun waitForBackend(button: Button) {
        val handler = Handler(Looper.getMainLooper())
        val check = object : Runnable {
            override fun run() {
                thread {
                    val ok = try {
                        val c = URL("http://127.0.0.1:$backendPort/health").openConnection() as HttpURLConnection
                        c.connectTimeout = 500; c.readTimeout = 500
                        c.requestMethod = "GET"; c.responseCode in 200..399
                    } catch (_: Throwable) { false }
                    runOnUiThread {
                        if (ok) { status.text = "Eigent local backend is ready"; button.isEnabled = true }
                        else { status.text = "Local runtime starting..."; handler.postDelayed(this, 1000) }
                    }
                }
            }
        }
        handler.post(check)
    }

    private fun openEigent() {
        // The web UI is served locally from the packaged Eigent web build.
        val web = android.webkit.WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            webViewClient = android.webkit.WebViewClient()
            webChromeClient = android.webkit.WebChromeClient()
            loadUrl("file:///android_asset/eigent-web/index.html")
        }
        setContentView(web)
    }

    private data class CatalogModel(val name: String, val description: String, val size: String, val pageUrl: String, val downloadUrl: String, val filename: String)

    private val catalog = listOf(
        CatalogModel("Qwen3 4B Q4_K_M", "Small general-purpose model.", "about 2.5 GB", "https://huggingface.co/ggml-org/Qwen3-4B-GGUF", "https://huggingface.co/ggml-org/Qwen3-4B-GGUF/resolve/main/Qwen3-4B-Q4_K_M.gguf", "Qwen3-4B-Q4_K_M.gguf"),
        CatalogModel("Qwen3 8B Q4_K_M", "Larger model for higher quality when memory permits.", "about 5 GB", "https://huggingface.co/Qwen/Qwen3-8B-GGUF", "https://huggingface.co/Qwen/Qwen3-8B-GGUF/resolve/main/Qwen3-8B-Q4_K_M.gguf", "Qwen3-8B-Q4_K_M.gguf"),
        CatalogModel("Qwen3.5 4B Q4_K_M", "Modern 4B candidate for local agent work.", "about 2.7 GB", "https://huggingface.co/unsloth/Qwen3.5-4B-GGUF", "https://huggingface.co/unsloth/Qwen3.5-4B-GGUF/resolve/main/Qwen3.5-4B-Q4_K_M.gguf", "Qwen3.5-4B-Q4_K_M.gguf")
    )

    private fun showModels() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 32, 24, 24) }
        layout.addView(TextView(this).apply { text = "Local Models"; textSize = 24f })
        layout.addView(TextView(this).apply { text = "Model weights live outside the APK." })
        val dir = File(filesDir, "models").apply { mkdirs() }
        val list = TextView(this).apply { text = listModels(dir) }
        layout.addView(list)
        catalog.forEach { model ->
            layout.addView(TextView(this).apply { text = "${model.name} — ${model.size}\n${model.description}" })
            layout.addView(Button(this).apply {
                text = "Browse"; setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(model.pageUrl))) }
            })
            layout.addView(Button(this).apply {
                text = "Download"; setOnClickListener {
                    isEnabled = false; text = "Downloading..."
                    thread { try { downloadFile(model.downloadUrl, dir, model.filename) } finally { runOnUiThread { list.text = listModels(dir); isEnabled = true; text = "Download" } } }
                }
            })
        }
        val url = EditText(this).apply { hint = "Direct GGUF URL"; singleLine = true }
        layout.addView(url)
        layout.addView(Button(this).apply {
            text = "Download Custom GGUF"; setOnClickListener {
                val u = url.text.toString().trim(); if (u.isEmpty()) return@setOnClickListener
                isEnabled = false; thread { try { downloadFile(u, dir, null) } finally { runOnUiThread { list.text = listModels(dir); isEnabled = true } } }
            }
        })
        layout.addView(Button(this).apply { text = "Back"; setOnClickListener { showLauncher() } })
        setContentView(layout)
    }

    private fun showDiagnostics() {
        val lines = buildString {
            appendLine("Eigent Mobile Runtime Diagnostics")
            appendLine("Process: ${android.os.Process.myPid()}")
            appendLine("API: ${android.os.Build.VERSION.SDK_INT}")
            appendLine("ABI: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("Files: ${filesDir.absolutePath}")
            appendLine("Runtime service: started from Activity")
            appendLine("Important: Activity destruction does not stop RuntimeService.")
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(24, 32, 24, 24)
            addView(TextView(this@MainActivity).apply { text = lines; textSize = 16f })
            addView(Button(this@MainActivity).apply { text = "Back"; setOnClickListener { showLauncher() } })
        })
    }

    private fun listModels(dir: File): String = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".gguf", true) }?.joinToString("\n\n") { "${it.name}\n${it.length() / 1024 / 1024} MB" } ?: "No GGUF models downloaded."

    private fun downloadFile(urlString: String, dir: File, requestedName: String?) {
        val c = URL(urlString).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 30000; c.connect()
        val name = requestedName ?: urlString.substringAfterLast('/').substringBefore('?').ifBlank { "model.gguf" }
        val target = File(dir, name); val part = File(dir, "$name.part")
        c.inputStream.use { input -> FileOutputStream(part).use { output -> input.copyTo(output) } }
        if (!part.renameTo(target)) throw IllegalStateException("Could not commit downloaded model")
        c.disconnect()
    }
}
