package com.maya.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val speechRequestCode = 101
    private val permissionRequestCode = 202

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        val enableAccessibilityBtn = findViewById<Button>(R.id.enableAccessibilityBtn)
        val startListeningBtn = findViewById<Button>(R.id.startListeningBtn)
        val enableNotificationsBtn = findViewById<Button>(R.id.enableNotificationsBtn)
        val apiKeyInput = findViewById<EditText>(R.id.apiKeyInput)
        val saveApiKeyBtn = findViewById<Button>(R.id.saveApiKeyBtn)

        // Start the background service so TTS + call watching are ready.
        ContextCompat.startForegroundService(this, Intent(this, MayaForegroundService::class.java))

        requestRuntimePermissions()

        enableAccessibilityBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        enableNotificationsBtn.setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        saveApiKeyBtn.setOnClickListener {
            val key = apiKeyInput.text.toString().trim()
            if (key.isNotEmpty()) {
                ClaudeCommandProcessor.saveApiKey(this, key)
                Toast.makeText(this, "API key saved", Toast.LENGTH_SHORT).show()
            }
        }

        startListeningBtn.setOnClickListener {
            launchSpeechRecognizer()
        }

        statusText.text = buildString {
            append(if (MayaAccessibilityService.isRunning) "Accessibility: ON\n" else "Accessibility: OFF\n")
            append(if (ClaudeCommandProcessor.hasApiKey(this@MainActivity)) "Claude API key: saved" else "Claude API key: not set")
        }
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.RECORD_AUDIO)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.READ_PHONE_STATE)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.POST_NOTIFICATIONS)

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), permissionRequestCode)
        }
    }

    private fun launchSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bolo, Maya sun rahi hai...")
        }
        try {
            startActivityForResult(intent, speechRequestCode)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechRequestCode && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: return
            Toast.makeText(this, "Heard: $spokenText", Toast.LENGTH_SHORT).show()

            ClaudeCommandProcessor.handleCommand(this, spokenText) { status ->
                Toast.makeText(this, status, Toast.LENGTH_LONG).show()
            }
        }
    }
}
