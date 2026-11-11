package com.paradigma.quizbot

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.abs
import org.json.JSONObject

class FloatingService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingView = TextView(this).apply {
            text = "🤖 Ketuk untuk Jawab Kuis!"
            setBackgroundColor(Color.parseColor("#EE000000"))
            setTextColor(Color.GREEN)
            setPadding(36, 24, 36, 24)
            textSize = 15f
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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = abs(event.rawX - initialTouchX)
                        val diffY = abs(event.rawY - initialTouchY)
                        // Jika tidak diseret jauh (kurang dari 10px), anggap sebagai KETUKAN JARI!
                        if (diffX < 10 && diffY < 10) {
                            val currentScreenText = QuizAccessibilityService.lastExtractedText
                            if (currentScreenText.isEmpty()) {
                                Toast.makeText(this@FloatingService, "Tidak ada teks kuis terdeteksi di layar!", Toast.LENGTH_SHORT).show()
                            } else {
                                floatingView?.text = "⏳ Gemini berpikir..."
                                fetchGeminiAnswer(currentScreenText)
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager?.addView(floatingView, params)
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
                        floatingView?.text = "💡 Jawaban: $answer"
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        floatingView?.text = "❌ Gagal koneksi AI (${conn.responseCode})"
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    floatingView?.text = "⚠️ Error: ${e.message}"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) windowManager?.removeView(floatingView)
    }
}
