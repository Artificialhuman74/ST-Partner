# R8 keep rules for release builds. Retrofit, OkHttp, kotlinx.serialization, Firebase, MapLibre and
# Credential Manager ship their own rules; this file only adds what the app itself needs.

# Readable stack traces in Play Console crash reports (the mapping file goes up with the bundle).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
