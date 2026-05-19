# ─── App: solo le classi accedute via reflection ─────────────────────────────
# I modelli dati (JSON @Serializable): R8 non può tracciarli staticamente
-keep class com.renttrack.app.data.model.** { *; }
# ViewModels: istanziati via factory (reflection)
-keep class com.renttrack.app.viewmodel.** { *; }
# Billing: callback e interfacce Play
-keep class com.renttrack.app.billing.** { *; }
# Config e prefs: accesso per nome
-keep class com.renttrack.app.AppConfig { *; }
-keep class com.renttrack.app.SecurePrefs { *; }
-keep class com.renttrack.app.PropertyManager { *; }
# Widget e Worker: registrati via XML/manifest
-keep class com.renttrack.app.widget.** { *; }
-keep class com.renttrack.app.notifications.** { *; }

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

# (catch-all rimosso — R8 ottimizza liberamente le classi non in lista)

# ─── WorkManager ──────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# ─── Security / Preferences ───────────────────────────────────────────────────
-keep class androidx.security.crypto.** { *; }

# ─── Credential Manager / Google Identity ─────────────────────────────────────
-keep class androidx.credentials.** { *; }
-keep class com.google.android.gms.auth.api.identity.** { *; }

# Compose non necessita -keep: le classi sono referenziate direttamente nel codice
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
