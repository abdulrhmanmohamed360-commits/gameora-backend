LockApp 2.0 (دمج: قفل Device Owner + إعداد الباسورد من التطبيق + وضع احتياطي)

التشغيل (الوضع القوي - Device Owner):
1) تليفون من غير أي حساب جوجل، USB debugging شغال
2) ابني الـ APK (Android Studio أو GitHub Actions) ثم: adb install app-debug.apk
3) adb shell dpm set-device-owner com.example.lockapp/.AdminReceiver
4) افتح التطبيق، اختر كلمة السر -> يتقفل

الوضع الاحتياطي (من غير adb): فعّل خدمة إمكانية الوصول من داخل التطبيق. أضعف: الطفل لو فصلها أو دخل Safe Mode يخرج.

فتح القفل: ادخل كلمة السر -> بيخرج للشاشة الرئيسية ويشيل القيود.
قفل تاني: افتح أيقونة LockApp.
لو نسيت كلمة السر في وضع Device Owner: adb shell dpm remove-active-admin com.example.lockapp/.AdminReceiver ثم factory reset لو لزم.
