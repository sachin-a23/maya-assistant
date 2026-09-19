package com.maya.assistant

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * The "AI brain": takes what the user said, asks Claude what to do
 * about it, and carries out the action through the accessibility
 * service.
 *
 * Claude is instructed to reply with ONLY a small JSON object so the
 * app can parse it reliably, e.g.:
 *   {"action": "open_app", "value": "com.whatsapp"}
 *   {"action": "speak", "value": "It's currently 5 PM."}
 */
object ClaudeCommandProcessor {

    private const val PREFS = "maya_prefs"
    private const val KEY_API_KEY = "claude_api_key"
    private const val API_URL = "https://api.anthropic.com/v1/messages"

    // Common apps Claude can map spoken names to. Add more as needed.
    private val KNOWN_APPS = mapOf(
        "whatsapp" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "camera" to "com.android.camera",
        "gmail" to "com.google.android.gm",
        "phone" to "com.android.dialer",
        "messages" to "com.google.android.apps.messaging",
        "settings" to "com.android.settings",
        "maps" to "com.google.android.apps.maps"
    )

    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun saveApiKey(context: Context, key: String) {
        prefs(context).edit().putString(KEY_API_KEY, key).apply()
    }

    fun hasApiKey(context: Context): Boolean =
        !prefs(context).getString(KEY_API_KEY, null).isNullOrBlank()

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Sends the spoken command to Claude and executes whatever it
     * decides to do. `onResult` is called back on the main thread
     * with a human-readable status you can show/log.
     */
    fun handleCommand(context: Context, spokenText: String, onResult: (String) -> Unit) {
        val apiKey = prefs(context).getString(KEY_API_KEY, null)
        if (apiKey.isNullOrBlank()) {
            onResult("No Claude API key saved yet — add it in the app first.")
            return
        }

        val appList = KNOWN_APPS.keys.joinToString(", ")
        val systemPrompt = """
            You control an Android phone through a limited set of actions.
            Reply with ONLY a compact JSON object, no other text, no markdown.
            Choose exactly one of these shapes:
            {"action": "open_app", "value": "<one of: $appList>"}
            {"action": "click", "value": "<visible text of the button/element to tap>"}
            {"action": "type", "value": "<text to type into the focused field>"}
            {"action": "speak", "value": "<what to say back to the user>"}
            If the request is just a question or conversation, use "speak".
            If you don't recognize the app name, use "speak" to say so.
        """.trimIndent()

        val body = JSONObject().apply {
            put("model", "claude-sonnet-4-6")
            put("max_tokens", 300)
            put("system", systemPrompt)
            put("messages", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", spokenText)
                }
            ))
        }

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                postResult(onResult, "Network error talking to Claude: ${e.message}")
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        postResult(onResult, "Claude API error: ${it.code}")
                        return
                    }
                    val json = JSONObject(it.body?.string() ?: "{}")
                    val text = json.optJSONArray("content")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        ?: ""
                    executeAction(context, text, onResult)
                }
            }
        })
    }

    private fun executeAction(context: Context, rawJson: String, onResult: (String) -> Unit) {
        val action = try {
            JSONObject(rawJson.trim())
        } catch (e: Exception) {
            postResult(onResult, "Couldn't understand Claude's response: $rawJson")
            return
        }

        val type = action.optString("action")
        val value = action.optString("value")
        val service = MayaAccessibilityService.instance

        val status = when (type) {
            "open_app" -> {
                val pkg = KNOWN_APPS[value.lowercase()] ?: value
                val opened = service?.openApp(pkg) ?: false
                if (opened) "Opened $value" else "Couldn't open $value — is it installed?"
            }
            "click" -> {
                val clicked = service?.clickNodeByText(value) ?: false
                if (clicked) "Tapped '$value'" else "Couldn't find '$value' on screen"
            }
            "type" -> {
                val typed = service?.typeIntoFocusedField(value) ?: false
                if (typed) "Typed the text" else "No text field is focused right now"
            }
            "speak" -> {
                MayaForegroundService.instance?.speak(value)
                value
            }
            else -> "Unrecognized action from Claude"
        }

        postResult(onResult, status)
    }

    private fun postResult(onResult: (String) -> Unit, message: String) {
        mainHandler.post { onResult(message) }
    }
}
