# tina release rules. Room, Ktor, Koin and Compose ship consumer rules; these cover what they do not.

# kotlinx.serialization: keep serializers and the @Serializable classes' generated companions
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.tina.app.**$$serializer { *; }
-keepclassmembers class com.tina.app.** { *** Companion; }
-keepclasseswithmembers class com.tina.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Room entities and DAOs are reached by generated code; keep their shape
-keep class com.tina.app.data.** { *; }

# Glance widgets and receivers are looked up by name from the manifest
-keep class com.tina.app.today.** { *; }
-keep class com.tina.app.capture.CaptureWidget** { *; }
-keep class com.tina.app.capture.*TileService { *; }
-keep class com.tina.app.notifications.*Receiver { *; }

# the rich text editor reflects on its own state classes
-keep class com.mohamedrejeb.richeditor.** { *; }
-dontwarn com.mohamedrejeb.richeditor.**

# Koin (no reflection on our side, but keep the module definitions readable in stack traces)
-keepnames class com.tina.app.di.**

# Ktor / OkHttp / coroutines noise
-dontwarn org.slf4j.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn kotlinx.coroutines.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# readable crash reports from Play vitals
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
