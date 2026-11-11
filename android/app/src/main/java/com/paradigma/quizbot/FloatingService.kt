package com.paradigma.quizbot

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import org.json.JSONObject

class FloatingService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingButton: Button? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingButton = Button(this).apply {
            text = "🤖 Ketuk untuk Jawab Kuis!"
            setBackgroundColor(Color.parseColor("#CC000000"))
            setTextColor(Color.GREEN)
            textSize = 14f
        }

        val layoutParamsType = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            y = 250
        }

        floatingButton?.setOnClickListener {
            floatingButton?.text = "⏳ Gemini berpikir..."
            val currentScreenText = QuizAccessibilityService.lastExtractedText
            
            if (currentScreenText.isEmpty()) {
                Toast.makeText(this@FloatingService, "Tidak ada teks kuis terdeteksi!", Toast.LENGTH_SHORT).show()
                floatingButton?.text = "🤖 Ketuk untuk Jawab Kuis!"
            } else {
                fetchGeminiAnswer(currentScreenText)
            }
        }

        windowManager?.addView(floatingButton, params)
    }

    private fun fetchGeminiAnswer(promptText: String) {
        thread {
            try {
                val apiKey = "AQ." + "Ab8RN6KV44bln9CWre0ilf3MBXsoC46uoDEH7i9zkICTwrmmdA"
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("contents", listOf(
                        JSONObject().apply {
                            put("parts", listOf(
                                JSONObject().apply {
                                    put("text", "Berikut teks dari layar kuis. Berikan jawaban yang tepat dan paling singkat saja:\n\n$promptText")
                                }
                            ))
                        }
                    ))
                }

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val jsonResp = JSONObject(response)
                    val answer = jsonResp.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    Handler(Looper.getMainLooper()).post {
                        floatingButton?.text = "💡 Jawaban: $answer"
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        floatingButton?.text = "❌ Gagal AI (${conn.responseCode})"
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    floatingButton?.text = "⚠️ Error: ${e.message}"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingButton != null) windowManager?.removeView(floatingButton)
    }
}
