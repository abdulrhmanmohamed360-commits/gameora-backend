package com.example.lockapp

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** تخزين الباسورد (PBKDF2 + salt عشوائي) وحالة القفل. مفيش باسورد مكتوب في الكود. */
object Store {
    private fun p(c: Context) = c.getSharedPreferences("lock", Context.MODE_PRIVATE)

    private fun derive(pw: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pw.toCharArray(), salt, 120_000, 256)).encoded

    fun hasPassword(c: Context) = p(c).contains("h")

    fun setPassword(c: Context, pw: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        p(c).edit()
            .putString("s", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("h", Base64.encodeToString(derive(pw, salt), Base64.NO_WRAP))
            .apply()
    }

    fun check(c: Context, pw: String): Boolean {
        val s = p(c).getString("s", null) ?: return false
        val h = p(c).getString("h", null) ?: return false
        val a = derive(pw, Base64.decode(s, Base64.NO_WRAP))
        val b = Base64.decode(h, Base64.NO_WRAP)
        return java.security.MessageDigest.isEqual(a, b)
    }

    // حالة القفل (بتفضل محفوظة بعد الريستارت)
    fun isLocked(c: Context) = p(c).getBoolean("locked", false)
    fun setLocked(c: Context, v: Boolean) = p(c).edit().putBoolean("locked", v).apply()

    // تأخير بعد محاولات غلط
    fun failures(c: Context) = p(c).getInt("fails", 0)
    fun setFailures(c: Context, n: Int) = p(c).edit().putInt("fails", n).apply()
    fun blockedUntil(c: Context) = p(c).getLong("until", 0L)
    fun setBlockedUntil(c: Context, t: Long) = p(c).edit().putLong("until", t).apply()
}
