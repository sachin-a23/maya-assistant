package com.maya.assistant

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val speechRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        val enableAccessibilityBtn = findViewById<Button>(R.id.enableAccessibilityBtn)
        val startListeningBtn = findViewById<Button>(R.id.startListeningBtn)

        // This just opens Android's own Accessibility settings screen.
        // YOU have to manually flip the switch there — apps are not
        // allowed to enable this permission for themselves.
        enableAccessibilityBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        startListeningBtn.setOnClickListener {
            launchSpeechRecognizer()
        }

        statusText.text = if (MayaAccessibilityService.isRunning) {
            "Maya accessibility service: ON"
        } else {
            "Maya accessibility service: OFF (tap button below)"
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

            // TODO: send `spokenText` to your AI backend (e.g. Claude API)
            // to decide what action to take, then call into
            // MayaAccessibilityService to perform it (open an app,
            // tap a button, type text, etc.)
        }
    }
}
