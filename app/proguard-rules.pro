# 本项目 release 默认未开启混淆。若开启 minifyEnabled，请保留 Media3 与 Room 的必要类：
-keep class androidx.media3.** { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.media3.**
