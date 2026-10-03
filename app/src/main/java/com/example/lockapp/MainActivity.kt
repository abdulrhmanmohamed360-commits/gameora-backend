package com.example.lockapp

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.UserManager
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private var finishing_ = false

    private val isOwner get() = dpm.isDeviceOwnerApp(packageName)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        admin = ComponentName(this, AdminReceiver::class.java)

        // زرار الرجوع مقفول
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
    }

    override fun onResume() {
        super.onResume()
        if (finishing_) return
        if (!Store.hasPassword(this)) {
            showSetup()
        } else {
            // فتح التطبيق = قفل (وبعد الريستارت بيرجع يقفل لأنه الـ Launcher)
            Store.setLocked(this, true)
            enterLockMode()
            showUnlock()
        }
    }

    // ---------- القفل ----------
    private fun enterLockMode() {
        if (!isOwner) return   // الوضع الاحتياطي: الـ Accessibility هو اللي بيمنع الخروج
        dpm.setLockTaskPackages(admin, arrayOf(packageName))

        val filter = IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        dpm.addPersistentPreferredActivity(admin, filter, ComponentName(this, MainActivity::class.java))

        dpm.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
        dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
        dpm.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER)
        dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)

        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) startLockTask()
    }

    private fun leaveLockMode() {
        Store.setLocked(this, false)
        finishing_ = true
        runCatching { stopLockTask() }
        if (isOwner) {
            dpm.clearPackagePersistentPreferredActivities(admin, packageName)
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_ADD_USER)
        }
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    // ---------- الواجهة (خلفية سودا كاملة، مفيش حاجة تتشاف) ----------
    private fun screen(vararg views: android.view.View) {
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            setPadding(64, 64, 64, 64)
            views.forEach { addView(it) }
        })
    }

    private fun text(t: String, size: Float = 18f, color: Int = Color.WHITE) =
        TextView(this).apply { text = t; textSize = size; setTextColor(color); gravity = Gravity.CENTER }

    private fun pinField(hint: String) = EditText(this).apply {
        this.hint = hint
        setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); gravity = Gravity.CENTER
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }

    private fun showSetup() {
        val pin = pinField("اختر كلمة السر (4 أرقام على الأقل)")
        val err = text("", color = Color.RED)
        val btn = Button(this).apply {
            text = "حفظ وتفعيل القفل"
            setOnClickListener {
                if (pin.text.length < 4) { err.text = "قصيرة جداً"; return@setOnClickListener }
                Store.setPassword(this@MainActivity, pin.text.toString())
                onResume()
            }
        }
        val views = mutableListOf(text("إعداد القفل", 26f), pin, btn, err)
        if (!isOwner) {
            views += text("تنبيه: التطبيق مش Device Owner. القفل هيشتغل بالوضع الاحتياطي (أضعف).", 14f, Color.YELLOW)
            views += Button(this).apply {
                text = "تفعيل خدمة إمكانية الوصول"
                setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            }
        }
        screen(*views.toTypedArray())
    }

    private fun showUnlock() {
        val pin = pinField("ادخل كلمة السر")
        val err = text("", color = Color.RED)
        val btn = Button(this).apply { text = "فتح" }

        btn.setOnClickListener {
            val now = System.currentTimeMillis()
            val until = Store.blockedUntil(this)
            if (now < until) { err.text = "استنى ${(until - now) / 1000 + 1} ثانية"; return@setOnClickListener }

            if (Store.check(this, pin.text.toString())) {
                Store.setFailures(this, 0)
                leaveLockMode()
            } else {
                val n = Store.failures(this) + 1
                Store.setFailures(this, n)
                if (n % 5 == 0) Store.setBlockedUntil(this, now + 30_000)
                err.text = "كلمة السر غلط"
                pin.text.clear()
            }
        }
        screen(text("🔒 الجهاز مقفول", 26f), pin, btn, err)
    }
}
