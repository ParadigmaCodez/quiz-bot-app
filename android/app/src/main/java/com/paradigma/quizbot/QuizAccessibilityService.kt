package com.paradigma.quizbot

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class QuizAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return

        // Membaca elemen teks yang ada di layar kuis
        findQuizElements(rootNode)
    }

    private fun findQuizElements(node: AccessibilityNodeInfo?) {
        if (node == null) return

        // Mengambil teks dari node yang sedang ditampilkan di layar
        if (node.text != null && node.text.isNotEmpty()) {
            val content = node.text.toString()
            // Di sini teks soal/pilihan jawaban ditangkap untuk dikirimkan ke Gemini API
        }

        for (i in 0 until node.childCount) {
            findQuizElements(node.getChild(i))
        }
    }

    override fun onInterrupt() {}
}

