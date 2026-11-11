package com.paradigma.quizbot

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkOverlayPermission()

        val btnStart = findViewById<Button>(R.id.btnStartFloating)
        val btnStop = findViewById<Button>(R.id.btnStopFloating)

        btnStart.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                startService(Intent(this, FloatingService::class.java))
                Toast.makeText(this, "Widget Melayang Aktif! 🚀", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                checkOverlayPermission()
            }
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, FloatingService::class.java))
            Toast.makeText(this, "Widget Melayang Dimatikan! ❌", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}
