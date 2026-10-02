package com.example.lockapp

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserManager
import android.text.InputType
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private var unlocked = false   // بيرجع false تلقائي بعد أي ريستارت

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        admin = ComponentName(this, AdminReceiver::class.java)

        if (dpm.isDeviceOwnerApp(packageName)) setupKiosk()
        showUnlockScreen()
    }

    override fun onResume() {
        super.onResume()
        if (!unlocked) lock()
    }

    // ---------- Kiosk ----------
    private fun setupKiosk() {
        dpm.setLockTaskPackages(admin, arrayOf(packageName))

        // التطبيق يبقى الـ Launcher الافتراضي (يفتح بعد الريستارت)
        val filter = IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        dpm.addPersistentPreferredActivity(
            admin, filter, ComponentName(this, MainActivity::class.java)
        )

        dpm.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
        dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)

        // مفيش Home / Recents / قايمة الباور
        dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
    }

    private fun lock() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) startLockTask()
    }

    private fun unlock() {
        unlocked = true
        stopLockTask()
        if (dpm.isDeviceOwnerApp(packageName)) {
            // نشيل كوننا Launcher عشان المستخدم يخرج للشاشة الرئيسية الحقيقية
            dpm.clearPackagePersistentPreferredActivities(admin, packageName)
        }
        finish()
    }

    // ---------- الباسورد (من الـ meta-data) ----------
    private fun expectedHash(): String {
        val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        return ai.metaData?.getString("lock_password_hash") ?: ""
    }

    private fun hash(s: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(("salt_lockapp_$s").toByteArray())
            .joinToString("") { "%02x".format(it) }

    // ---------- الواجهة ----------
    private fun showUnlockScreen() {
        val pin = EditText(this).apply {
            hint = "ادخل الباسورد"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val btn = Button(this).apply { text = "فتح" }
        val err = TextView(this)

        btn.setOnClickListener {
            if (hash(pin.text.toString()) == expectedHash()) {
                unlock()
            } else {
                err.text = "الباسورد غلط"
                pin.text.clear()
            }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 200, 64, 64)
            addView(pin); addView(btn); addView(err)
        })
    }
}
