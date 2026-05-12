package com.renttrack.app.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// ─── Auth state ───────────────────────────────────────────────────────────────
sealed class AuthState {
    data object Loading        : AuthState()
    data object LoggedOut      : AuthState()
    data object GoogleLoading  : AuthState()  // attesa Google Sign-In
    data class  LoggedIn(val email: String)  : AuthState()
    data class  Error(val message: String)   : AuthState()
    data class  EmailSent(val email: String) : AuthState()  // dopo registrazione
}

// ─── ViewModel ────────────────────────────────────────────────────────────────
class AuthViewModel(private val prefs: SharedPreferences) : ViewModel() {

    // Web Client ID Google — da impostare dopo la configurazione in Google Cloud Console
    // Formato: "XXXXXXXXXX-xxxxxxxxxxxx.apps.googleusercontent.com"
    companion object {
        const val GOOGLE_WEB_CLIENT_ID = "763544456219-e4lr7lt8nrqdcdgocfkd35nv6fpt5sed.apps.googleusercontent.com"
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val supabaseUrl = "https://zjqrtuposdrimzjoydgh.supabase.co"
    private val anonKey     =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
        "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpqcXJ0dXBvc2RyaW16am95ZGdoIiwi" +
        "cm9sZSI6ImFub24iLCJpYXQiOjE3NzgzMzE5MDMsImV4cCI6MjA5MzkwNzkwM30." +
        "dLvc0pPrfIXiBGJSDTnRtNU6FRppFPSr86pLwHD35j4"

    init { checkSession() }

    // ── Controlla sessione: verifica JWT expiry + refresh automatico ─────────
    fun checkSession() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val token = prefs.getString("auth_token", null)
            val email = prefs.getString("auth_email", null)

            if (token == null || email == null) {
                _authState.value = AuthState.LoggedOut
                return@launch
            }

            // Token presente ma non scaduto → sessione valida
            if (!isTokenExpired(token)) {
                _authState.value = AuthState.LoggedIn(email)
                return@launch
            }

            // Token scaduto → prova refresh
            val refreshToken = prefs.getString("refresh_token", null)
            if (refreshToken != null) {
                val refreshed = tryRefreshToken(refreshToken)
                if (refreshed) {
                    val updatedEmail = prefs.getString("auth_email", email) ?: email
                    _authState.value = AuthState.LoggedIn(updatedEmail)
                    return@launch
                }
            }

            // Nessun refresh possibile → forza logout
            clearSession()
            _authState.value = AuthState.LoggedOut
        }
    }

    /** Restituisce true se il JWT è scaduto (o non decodificabile). */
    private fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return true
            val padded = parts[1].let { it.padEnd((it.length + 3) / 4 * 4, '=') }
            val payload = String(Base64.decode(padded, Base64.URL_SAFE))
            val exp = JSONObject(payload).optLong("exp", 0L)
            val nowSeconds = System.currentTimeMillis() / 1000
            exp == 0L || nowSeconds >= (exp - 60) // 60s di margine
        } catch (e: Exception) { true }
    }

    /** Chiama /auth/v1/token?grant_type=refresh_token e aggiorna le prefs. */
    private suspend fun tryRefreshToken(refreshToken: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val body = """{"refresh_token":"$refreshToken"}"""
                val conn = URL("$supabaseUrl/auth/v1/token?grant_type=refresh_token")
                    .openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("apikey", anonKey)
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream).use { it.write(body) }
                val code = conn.responseCode
                if (code in 200..299) {
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    conn.disconnect()
                    val newAccess  = json.optString("access_token")
                    val newRefresh = json.optString("refresh_token")
                    if (newAccess.isNotBlank()) {
                        prefs.edit()
                            .putString("auth_token", newAccess)
                            .apply { if (newRefresh.isNotBlank()) putString("refresh_token", newRefresh) }
                            .apply()
                        true
                    } else false
                } else { conn.disconnect(); false }
            } catch (e: Exception) { false }
        }

    /** Rimuove tutti i dati di sessione da SharedPreferences. */
    private fun clearSession() {
        prefs.edit()
            .remove("auth_token")
            .remove("refresh_token")
            .remove("auth_email")
            .remove("auth_user_id")
            .apply()
    }

    // ── Login con email + password ────────────────────────────────────────────
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    callSupabaseAuth(
                        endpoint  = "$supabaseUrl/auth/v1/token?grant_type=password",
                        body      = """{"email":"$email","password":"$password"}"""
                    )
                }
                if (result.has("access_token")) {
                    val token     = result.getString("access_token")
                    val userObj   = result.optJSONObject("user")
                    val userEmail = userObj?.optString("email") ?: email
                    val userId    = userObj?.optString("id") ?: ""
                    val refreshToken = result.optString("refresh_token")
                    prefs.edit()
                        .putString("auth_token", token)
                        .putString("auth_email", userEmail)
                        .putString("auth_user_id", userId)
                        .apply { if (refreshToken.isNotBlank()) putString("refresh_token", refreshToken) }
                        .apply()
                    _authState.value = AuthState.LoggedIn(userEmail)
                } else {
                    val errDesc = result.optString("error_description", "")
                    val msg = when {
                        errDesc.contains("not confirmed", ignoreCase = true) ->
                            "Conferma prima la tua email (controlla la casella di posta)"
                        errDesc.contains("Invalid login", ignoreCase = true) ->
                            "Email o password errata"
                        errDesc.isNotEmpty() -> errDesc
                        else -> "Accesso non riuscito"
                    }
                    _authState.value = AuthState.Error(msg)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Errore di rete: ${e.message}")
            }
        }
    }

    // ── Registrazione con email + password ────────────────────────────────────
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    callSupabaseAuth(
                        endpoint = "$supabaseUrl/auth/v1/signup",
                        body     = """{"email":"$email","password":"$password"}"""
                    )
                }
                when {
                    // Email confirmation DISABILITATA → access_token diretto
                    result.has("access_token") -> {
                        val token     = result.getString("access_token")
                        val userObj   = result.optJSONObject("user")
                        val userEmail = userObj?.optString("email") ?: email
                        val userId    = userObj?.optString("id") ?: ""
                        val refreshToken = result.optString("refresh_token")
                        prefs.edit()
                            .putString("auth_token", token)
                            .putString("auth_email", userEmail)
                            .putString("auth_user_id", userId)
                            .apply { if (refreshToken.isNotBlank()) putString("refresh_token", refreshToken) }
                            .apply()
                        _authState.value = AuthState.LoggedIn(userEmail)
                    }
                    // Email confirmation ABILITATA → mostra messaggio conferma
                    result.has("id") -> {
                        _authState.value = AuthState.EmailSent(email)
                    }
                    else -> {
                        val msg = result.optString("error_description",
                            result.optString("msg", "Registrazione fallita"))
                        _authState.value = AuthState.Error(msg)
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Errore di rete: ${e.message}")
            }
        }
    }

    // ── Google Sign-In ─────────────────────────────────────────────────────────
    fun signInWithGoogle(context: Context) {
        if (GOOGLE_WEB_CLIENT_ID.isBlank()) {
            _authState.value = AuthState.Error("Google Sign-In non ancora configurato. Aggiungi il Web Client ID.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.GoogleLoading
            try {
                // 1. Richiedi credenziale Google tramite Credential Manager
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)  // mostra tutti gli account Google
                    .setServerClientId(GOOGLE_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                val googleIdToken = GoogleIdTokenCredential
                    .createFrom(credential.data)
                    .idToken

                // 2. Scambia il Google ID token con Supabase
                val supabaseResult = withContext(Dispatchers.IO) {
                    callSupabaseAuth(
                        endpoint = "$supabaseUrl/auth/v1/token?grant_type=id_token",
                        body = """{"provider":"google","id_token":"$googleIdToken"}"""
                    )
                }

                if (supabaseResult.has("access_token")) {
                    val token     = supabaseResult.getString("access_token")
                    val userObj   = supabaseResult.optJSONObject("user")
                    val userEmail = userObj?.optString("email") ?: "utente Google"
                    val userId    = userObj?.optString("id") ?: ""
                    val refreshToken = supabaseResult.optString("refresh_token")
                    prefs.edit()
                        .putString("auth_token", token)
                        .putString("auth_email", userEmail)
                        .putString("auth_user_id", userId)
                        .apply { if (refreshToken.isNotBlank()) putString("refresh_token", refreshToken) }
                        .apply()
                    _authState.value = AuthState.LoggedIn(userEmail)
                } else {
                    val msg = supabaseResult.optString("error_description",
                        supabaseResult.optString("msg", "Accesso con Google non riuscito"))
                    _authState.value = AuthState.Error(msg)
                }
            } catch (e: GetCredentialException) {
                _authState.value = AuthState.Error("Accesso con Google annullato")
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Errore Google Sign-In: ${e.message}")
            }
        }
    }

    // ── Logout: cancella tutta la sessione e blocca nuove richieste dati ──────
    fun signOut() {
        clearSession()
        _authState.value = AuthState.LoggedOut
    }

    // ── Reset errore (es. quando si passa da login a registrazione) ───────────
    fun resetError() {
        if (_authState.value is AuthState.Error || _authState.value is AuthState.EmailSent) {
            _authState.value = AuthState.LoggedOut
        }
    }

    // ── Helper HTTP (Supabase REST) ───────────────────────────────────────────
    private fun callSupabaseAuth(endpoint: String, body: String): JSONObject {
        val url  = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream).use { it.write(body) }
            val response = conn.inputStream.bufferedReader().readText()
            JSONObject(response)
        } catch (e: Exception) {
            try {
                val errResponse = conn.errorStream?.bufferedReader()?.readText() ?: "{}"
                JSONObject(errResponse)
            } catch (_: Exception) {
                JSONObject("""{"error_description":"${e.message}"}""")
            }
        } finally {
            conn.disconnect()
        }
    }
}

// ─── Factory ─────────────────────────────────────────────────────────────────
class AuthViewModelFactory(private val context: Context) :
    androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val prefs = context.getSharedPreferences("renttrack_prefs", Context.MODE_PRIVATE)
        @Suppress("UNCHECKED_CAST")
        return AuthViewModel(prefs) as T
    }
}
