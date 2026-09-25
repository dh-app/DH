# kotlinx.serialization and Navigation's type-safe routes ship their own R8
# rules. Keep this file for app-specific rules only.

# Keep line numbers so crash reports from release builds are readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
