package com.paradigma.quizbot

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class QuizAccessibilityService : AccessibilityService() {

    companion object {
        var lastExtractedText: String = ""
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return
        val textList = mutableListOf<String>()
        findTextNodes(rootNode, textList)
        if (textList.isNotEmpty()) {
            lastExtractedText = textList.joinToString("\n")
        }
    }

    private fun findTextNodes(node: AccessibilityNodeInfo?, textList: MutableList<String>) {
        if (node == null) return
        if (!node.text.isNullOrEmpty()) {
            textList.add(node.text.toString())
        }
        for (i in 0 until node.childCount) {
            findTextNodes(node.getChild(i), textList)
        }
    }

    override fun onInterrupt() {}
}
