package com.renttrack.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Provider centralizzato per SharedPreferences crittografate (AES-256-GCM).
 *
 * MOTIVAZIONE: i token JWT (access_token, refresh_token) erano salvati in
 * SharedPreferences in chiaro, leggibili su dispositivi rooted o con adb.
 * EncryptedSharedPreferences usa Android Keystore per cifrare chiavi e valori.
 *
 * FILE: "renttrack_secure_prefs" — diverso dal vecchio "renttrack_prefs".
 *   Al primo avvio post-aggiornamento i token non vengono trovati → logout
 *   automatico una volta sola → l'utente effettua di nuovo il login.
 *
 * FALLBACK: se il KeyStore del dispositivo non è disponibile (raro, alcuni
 *   dispositivi con keystore corrotto), torna alle prefs standard per
 *   evitare crash. L'app continua a funzionare, la cifratura è degraded.
 */
object SecurePrefs {

    private const val FILE_NAME = "renttrack_secure_prefs"

    fun get(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback graceful: prefs standard se KeyStore non disponibile
            context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        }
    }
}
