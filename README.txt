LockApp - تطبيق قفل (Kiosk) بالباسورد  |  الباسورد: 12345678

التركيب:
1) في Android Studio: New Project > Empty Views Activity
   Package name: com.example.lockapp  |  Min SDK: 28 أو أعلى
2) انسخ الملفات دي فوق اللي في مشروعك (ونفس المسارات):
   app/src/main/AndroidManifest.xml
   app/src/main/res/xml/device_admin.xml
   app/src/main/java/com/example/lockapp/AdminReceiver.kt
   app/src/main/java/com/example/lockapp/MainActivity.kt
3) امسح activity_main.xml لو اتعمل (مش محتاجينه).
4) تأكد إن dependency بتاعة appcompat موجودة (بتتضاف تلقائي).

التشغيل على جهاز (Device Owner):
- جهاز من غير أي حساب جوجل، وUSB debugging شغال
- adb install app-debug.apk
- adb shell dpm set-device-owner com.example.lockapp/.AdminReceiver
- افتح التطبيق: هيتقفل، ويطلب الباسورد

فك الـ Device Owner (للتجربة):
- بعد ما تفتح بالباسورد، من adb:
  adb shell dpm remove-active-admin com.example.lockapp/.AdminReceiver
  (لو مشتغلش لأنه Device Owner، اعمل Factory Reset أو جرّب على Emulator)

ملاحظات:
- بعد الفتح بالباسورد التطبيق بيخرج وبيشيل نفسه كـ Launcher.
  لو فتحت أيقونته تاني بيتقفل من جديد.
- لتغيير الباسورد: احسب SHA-256 لـ salt_lockapp_<الباسورد الجديد>
  وحطه في meta-data lock_password_hash داخل الـ Manifest.
