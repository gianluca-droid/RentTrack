# ─── App Models ──────────────────────────────────────────────────────────────
-keep class com.renttrack.app.data.model.** { *; }
-keep class com.renttrack.app.data.repository.** { *; }
-keep class com.renttrack.app.billing.** { *; }
-keep class com.renttrack.app.ui.** { *; }
-keep class com.renttrack.app.viewmodel.** { *; }

# ─── SLF4J (usato internamente da Ktor/Supabase) ─────────────────────────────
-dontwarn org.slf4j.**

# ─── Kotlin Serialization ─────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.renttrack.app.**$$serializer { *; }

# ─── Ktor (Supabase uses Ktor) ────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**

# ─── Supabase ─────────────────────────────────────────────────────────────────
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# ─── OkHttp / Okio ───────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ─── Coil (image loading) ─────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ─── Google Play Billing ──────────────────────────────────────────────────────
-keep class com.android.billingclient.** { *; }
-keep interface com.android.billingclient.** { *; }

# ─── Room ─────────────────────────────────────────────────────────────────────
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# ─── App (catch-all: nessuna classe interna viene rimossa) ────────────────────
-keep class com.renttrack.app.** { *; }
-keep class com.gianlucadelfini.renttrack.** { *; }

# ─── WorkManager ──────────────────────────────────────────────────────────────
-keep class androidx.work.** { *; }

# ─── Security / Preferences ───────────────────────────────────────────────────
-keep class androidx.security.crypto.** { *; }

# ─── Credential Manager / Google Identity ─────────────────────────────────────
-keep class androidx.credentials.** { *; }
-keep class com.google.android.gms.auth.api.identity.** { *; }

# ─── Jetpack Compose ──────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ─── Coroutines ───────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ─── Reflection (Kotlin) ──────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes Exceptions
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }
