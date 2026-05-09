package com.renttrack.app.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    data object Loading   : AuthState()
    data object LoggedOut : AuthState()
    data class  LoggedIn(val email: String)  : AuthState()
    data class  Error(val message: String)   : AuthState()
    data class  EmailSent(val email: String) : AuthState()  // dopo registrazione
}

// ─── ViewModel ────────────────────────────────────────────────────────────────
class AuthViewModel(private val prefs: SharedPreferences) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val supabaseUrl = "https://zjqrtuposdrimzjoydgh.supabase.co"
    private val anonKey     =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
        "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpqcXJ0dXBvc2RyaW16am95ZGdoIiwi" +
        "cm9sZSI6ImFub24iLCJpYXQiOjE3NzgzMzE5MDMsImV4cCI6MjA5MzkwNzkwM30." +
        "dLvc0pPrfIXiBGJSDTnRtNU6FRppFPSr86pLwHD35j4"

    init { checkSession() }

    // ── Controlla se c'è già una sessione salvata ─────────────────────────────
    fun checkSession() {
        val token = prefs.getString("auth_token", null)
        val email = prefs.getString("auth_email", null)
        if (token != null && email != null) {
            _authState.value = AuthState.LoggedIn(email)
        } else {
            _authState.value = AuthState.LoggedOut
        }
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
                    val userEmail = result.optJSONObject("user")?.optString("email") ?: email
                    prefs.edit()
                        .putString("auth_token", token)
                        .putString("auth_email", userEmail)
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
                        val userEmail = result.optJSONObject("user")?.optString("email") ?: email
                        prefs.edit()
                            .putString("auth_token", token)
                            .putString("auth_email", userEmail)
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

    // ── Logout ────────────────────────────────────────────────────────────────
    fun signOut() {
        prefs.edit().remove("auth_token").remove("auth_email").apply()
        _authState.value = AuthState.LoggedOut
    }

    // ── Reset errore (es. quando si passa da login a registrazione) ───────────
    fun resetError() {
        if (_authState.value is AuthState.Error) {
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
