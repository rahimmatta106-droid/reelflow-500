# ReelFlow 500 — minimal ProGuard rules (minify is off; keep for release builds).
-keep class com.reelflow.platform.** { *; }
-keep class com.facebook.** { *; }
-dontwarn com.facebook.**
