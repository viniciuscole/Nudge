package dev.viniciuscole.nudge.data.json

import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import org.json.JSONArray
import org.json.JSONObject

object ReminderJson {

    fun encode(list: List<Reminder>): String {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        return arr.toString()
    }

    fun decode(raw: String?): List<Reminder> {
        if (raw.isNullOrBlank()) return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }

    private fun toJson(r: Reminder): JSONObject = JSONObject()
        .put("id", r.id)
        .put("type", r.type.name)
        .put("label", r.label)
        .put("enabled", r.enabled)
        .put("insistent", r.insistent)
        .put("hour", r.hour)
        .put("minute", r.minute)
        .put("startHour", r.startHour)
        .put("endHour", r.endHour)
        .put("intervalMin", r.intervalMin)

    private fun fromJson(o: JSONObject): Reminder = Reminder(
        id = o.getLong("id"),
        type = ReminderType.valueOf(o.getString("type")),
        label = o.getString("label"),
        enabled = o.optBoolean("enabled", true),
        insistent = o.optBoolean("insistent", true),
        hour = o.optInt("hour", 12),
        minute = o.optInt("minute", 30),
        startHour = o.optInt("startHour", 8),
        endHour = o.optInt("endHour", 22),
        intervalMin = o.optInt("intervalMin", 90),
    )
}
