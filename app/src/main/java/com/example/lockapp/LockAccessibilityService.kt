package com.example.lockapp

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

/**
 * الوضع الاحتياطي: لو التطبيق مش Device Owner، أي نافذة غير نافذتنا بتتقفل
 * وبنرجّع شاشة القفل. لو Device Owner الخدمة دي مش بتعمل حاجة.
 */
class LockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!Store.isLocked(this)) return

        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isDeviceOwnerApp(packageName)) return

        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName || isKeyboard(pkg)) return

        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    private fun isKeyboard(pkg: String): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == pkg }
    }

    override fun onInterrupt() {}
}
