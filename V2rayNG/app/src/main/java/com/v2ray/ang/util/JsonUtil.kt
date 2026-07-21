package com.v2ray.ang.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object JsonUtil {
    private var gson = Gson()

    fun toJson(src: Any?): String {
        return gson.toJson(src)
    }

    fun <T> fromJson(src: String, cls: Class<T>): T {
        return gson.fromJson(src, cls)
    }

    fun toJsonPretty(src: Any?): String? {
        if (src == null)
            return null
        val gsonPre = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter( // custom serializer is needed here since JSON by default parse number as Double, core will fail to start
                object : TypeToken<Double>() {}.type,
                JsonSerializer { src: Double?, _: Type?, _: JsonSerializationContext? ->
                    JsonPrimitive(
                        src?.toInt()
                    )
                }
            )
            .create()
        return gsonPre.toJson(src)
    }

    /**
     * Normalizes an object received from URI query parameters.
     *
     * Some subscription generators encode XHTTP `extra` twice, leaving a value
     * such as `%7B%22xmux%22...%7D` after the regular query decoding pass.
     * Decode only values that look like a percent-encoded JSON object and keep
     * the number of passes bounded to avoid changing ordinary JSON strings.
     */
    fun normalizeObjectString(src: String?): String? {
        var value = src?.trim()?.removePrefix("\uFEFF")?.takeIf { it.isNotEmpty() }
            ?: return null

        repeat(2) {
            if (!looksLikeEncodedObject(value)) return@repeat
            val decoded = Utils.urlDecode(value).trim().removePrefix("\uFEFF")
            if (decoded == value) return@repeat
            value = decoded
        }
        return value
    }

    fun parseString(src: String?): JsonObject? {
        val normalized = normalizeObjectString(src) ?: return null
        return try {
            val element = JsonParser.parseString(normalized)
            when {
                element.isJsonObject -> element.asJsonObject
                element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                    val nested = normalizeObjectString(element.asString) ?: return null
                    JsonParser.parseString(nested).takeIf { it.isJsonObject }?.asJsonObject
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun looksLikeEncodedObject(value: String): Boolean {
        return value.startsWith("%7B", ignoreCase = true) ||
            value.startsWith("%257B", ignoreCase = true)
    }
}