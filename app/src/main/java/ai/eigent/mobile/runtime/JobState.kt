package ai.eigent.mobile.runtime

import org.json.JSONObject

/** Durable state for a long-running mobile job. */
data class JobState(
    val id: String,
    val kind: String,
    val status: String,
    val startedAt: Long,
    val updatedAt: Long,
    val progress: Int = 0,
    val message: String = "",
    val checkpoint: String = "",
    val error: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("kind", kind)
        put("status", status)
        put("startedAt", startedAt)
        put("updatedAt", updatedAt)
        put("progress", progress)
        put("message", message)
        put("checkpoint", checkpoint)
        put("error", error ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(o: JSONObject) = JobState(
            id = o.getString("id"),
            kind = o.getString("kind"),
            status = o.getString("status"),
            startedAt = o.getLong("startedAt"),
            updatedAt = o.getLong("updatedAt"),
            progress = o.optInt("progress", 0),
            message = o.optString("message", ""),
            checkpoint = o.optString("checkpoint", ""),
            error = if (o.isNull("error")) null else o.optString("error")
        )
    }
}
