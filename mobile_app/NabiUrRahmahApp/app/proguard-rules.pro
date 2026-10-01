# kotlinx.serialization and Navigation's type-safe routes ship their own R8
# rules. Keep this file for app-specific rules only.

# Keep line numbers so crash reports from release builds are readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# jsoup: optional regex engine and annotations it can use but we don't ship.
-dontwarn com.google.re2j.**
-dontwarn org.jspecify.annotations.**

# The YouTube player talks to its WebView through @JavascriptInterface methods;
# keep them so the shrinker can't remove or rename what the page calls.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
