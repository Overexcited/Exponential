package ai.eigent.mobile.runtime

import android.content.Context
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONObject

/**
 * Small durable job store used by the first vertical slice.
 * Room can replace the store later without changing the service contract.
 */
class JobEngine(private val context: Context) {
    private val root = File(context.filesDir, "jobs").apply { mkdirs() }
    private val active = AtomicReference<JobState?>(null)

    fun create(kind: String): JobState {
        val now = System.currentTimeMillis()
        return JobState(UUID.randomUUID().toString(), kind, "CREATED", now, now)
            .also { persist(it); active.set(it) }
    }

    fun update(state: JobState): JobState {
        val next = state.copy(updatedAt = System.currentTimeMillis())
        persist(next)
        active.set(next)
        return next
    }

    fun current(): JobState? = active.get() ?: recoverLatest()

    fun recoverLatest(): JobState? {
        val state = root.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { read(File(it, "state.json")) }
            ?.maxByOrNull { it.updatedAt }
        active.set(state)
        return state
    }

    private fun persist(state: JobState) {
        val dir = File(root, state.id).apply { mkdirs() }
        val tmp = File(dir, "state.json.tmp")
        val target = File(dir, "state.json")
        tmp.writeText(state.toJson().toString(2), StandardCharsets.UTF_8)
        if (!tmp.renameTo(target)) {
            target.writeText(tmp.readText(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
            tmp.delete()
        }
    }

    private fun read(file: File): JobState? = try {
        JobState.fromJson(JSONObject(file.readText(StandardCharsets.UTF_8)))
    } catch (_: Throwable) { null }
}
