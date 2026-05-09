package com.renttrack.app.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.renttrack.app.data.model.Inquiry
import com.renttrack.app.data.model.Listing
import com.renttrack.app.data.model.ListingPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

// ─── State ────────────────────────────────────────────────────────────────────
sealed class ListingsUiState {
    data object Loading : ListingsUiState()
    data class Success(val listings: List<Listing>) : ListingsUiState()
    data class Error(val message: String) : ListingsUiState()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────
class ListingsViewModel(
    private val prefs: SharedPreferences,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val baseUrl = "https://zjqrtuposdrimzjoydgh.supabase.co"
    private val anonKey =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
        "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpqcXJ0dXBvc2RyaW16am95ZGdoIiwi" +
        "cm9sZSI6ImFub24iLCJpYXQiOjE3NzgzMzE5MDMsImV4cCI6MjA5MzkwNzkwM30." +
        "dLvc0pPrfIXiBGJSDTnRtNU6FRppFPSr86pLwHD35j4"

    private val authToken: String? get() = prefs.getString("auth_token", null)

    // Stato feed pubblico
    private val _publicState = MutableStateFlow<ListingsUiState>(ListingsUiState.Loading)
    val publicState: StateFlow<ListingsUiState> = _publicState.asStateFlow()

    // Stato annunci del proprietario
    private val _myListings = MutableStateFlow<List<Listing>>(emptyList())
    val myListings: StateFlow<List<Listing>> = _myListings.asStateFlow()

    private val _myListingsLoading = MutableStateFlow(false)
    val myListingsLoading: StateFlow<Boolean> = _myListingsLoading.asStateFlow()

    // Richieste ricevute (inquiries) per il proprietario
    private val _myInquiries = MutableStateFlow<List<Inquiry>>(emptyList())
    val myInquiries: StateFlow<List<Inquiry>> = _myInquiries.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    init { loadPublicListings() }

    // ── Feed pubblico ─────────────────────────────────────────────────────────
    fun loadPublicListings() {
        viewModelScope.launch {
            _publicState.value = ListingsUiState.Loading
            try {
                val list = withContext(Dispatchers.IO) {
                    val listingsJson = httpGet(
                        "$baseUrl/rest/v1/listings?is_active=eq.true&order=created_at.desc",
                        anonKey
                    )
                    val listings = parseListings(JSONArray(listingsJson))
                    // Carica le foto separatamente
                    listings.map { l ->
                        val photosJson = httpGet(
                            "$baseUrl/rest/v1/listing_photos?listing_id=eq.${l.id}&order=display_order.asc",
                            anonKey
                        )
                        val photos = parsePhotos(JSONArray(photosJson), l.id)
                        l.copy(photos = photos)
                    }
                }
                _publicState.value = ListingsUiState.Success(list)
            } catch (e: Exception) {
                _publicState.value = ListingsUiState.Error("Caricamento fallito: ${e.message}")
            }
        }
    }

    // ── Annunci del proprietario ──────────────────────────────────────────────
    fun loadMyListings() {
        viewModelScope.launch {
            _myListingsLoading.value = true
            try {
                val token = authToken ?: return@launch
                val list = withContext(Dispatchers.IO) {
                    val listingsJson = httpGet(
                        "$baseUrl/rest/v1/listings?order=created_at.desc",
                        token
                    )
                    val listings = parseListings(JSONArray(listingsJson))
                    listings.map { l ->
                        val photosJson = httpGet(
                            "$baseUrl/rest/v1/listing_photos?listing_id=eq.${l.id}&order=display_order.asc",
                            token
                        )
                        val photos = parsePhotos(JSONArray(photosJson), l.id)
                        l.copy(photos = photos)
                    }
                }
                _myListings.value = list
            } catch (e: Exception) {
                _toast.value = "Errore caricamento annunci: ${e.message}"
            } finally {
                _myListingsLoading.value = false
            }
        }
    }

    // ── Crea annuncio ─────────────────────────────────────────────────────────
    fun createListing(
        title: String, city: String, zone: String,
        priceMonthly: Double, sqm: Int?, rooms: Int?, bathrooms: Int?,
        floor: String, furnished: Boolean, availableFrom: String,
        description: String, contactType: String,
        contactPhone: String, contactEmail: String, contactWhatsapp: String,
        photoUris: List<Uri>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val token = authToken ?: throw Exception("Non autenticato")
                val listingId = withContext(Dispatchers.IO) {
                    postListing(
                        token, title, city, zone, priceMonthly, sqm, rooms, bathrooms,
                        floor, furnished, availableFrom, description, contactType,
                        contactPhone, contactEmail, contactWhatsapp
                    )
                }
                if (listingId != null && photoUris.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        photoUris.forEachIndexed { index, uri ->
                            val url = uploadPhoto(token, uri, listingId)
                            if (url != null) savePhotoRecord(token, listingId, url, index == 0, index)
                        }
                    }
                }
                loadMyListings()
                loadPublicListings()
                _toast.value = "Annuncio pubblicato! ✅"
                onSuccess()
            } catch (e: Exception) {
                _toast.value = "Errore: ${e.message}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    // ── Elimina annuncio ──────────────────────────────────────────────────────
    fun deleteListing(listingId: String) {
        viewModelScope.launch {
            try {
                val token = authToken ?: return@launch
                withContext(Dispatchers.IO) {
                    httpPost("$baseUrl/rest/v1/listings?id=eq.$listingId", token, "", method = "DELETE")
                }
                loadMyListings(); loadPublicListings()
                _toast.value = "Annuncio eliminato"
            } catch (e: Exception) { _toast.value = "Errore eliminazione: ${e.message}" }
        }
    }

    // ── Attiva / disattiva annuncio ───────────────────────────────────────────
    fun toggleActive(listingId: String, current: Boolean) {
        viewModelScope.launch {
            try {
                val token = authToken ?: return@launch
                withContext(Dispatchers.IO) {
                    httpPost(
                        "$baseUrl/rest/v1/listings?id=eq.$listingId", token,
                        """{"is_active":${!current}}""", method = "PATCH"
                    )
                }
                loadMyListings(); loadPublicListings()
            } catch (e: Exception) { _toast.value = "Errore toggle: ${e.message}" }
        }
    }

    // ── Libero / Occupato ─────────────────────────────────────────────────────
    fun toggleAvailable(listingId: String, current: Boolean) {
        viewModelScope.launch {
            try {
                val token = authToken ?: return@launch
                withContext(Dispatchers.IO) {
                    httpPost(
                        "$baseUrl/rest/v1/listings?id=eq.$listingId", token,
                        """{"is_available":${!current}}""", method = "PATCH"
                    )
                }
                loadMyListings(); loadPublicListings()
                _toast.value = if (!current) "✅ Immobile segnato come libero" else "🔒 Immobile segnato come occupato"
            } catch (e: Exception) { _toast.value = "Errore: ${e.message}" }
        }
    }

    // ── Invia richiesta (cercatore) ───────────────────────────────────────────
    fun submitInquiry(
        listingId: String, name: String, phone: String,
        email: String, message: String, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                withContext(Dispatchers.IO) {
                    val body = JSONObject().apply {
                        put("listing_id", listingId)
                        put("seeker_name", name)
                        put("seeker_phone", phone)
                        put("seeker_email", email)
                        put("message", message)
                    }.toString()
                    httpPost(
                        "$baseUrl/rest/v1/inquiries", anonKey, body,
                        extraHeaders = mapOf("Prefer" to "return=minimal")
                    )
                }
                _toast.value = "Richiesta inviata! Il proprietario ti contatterà 👍"
                onSuccess()
            } catch (e: Exception) {
                _toast.value = "Errore invio: ${e.message}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun clearToast() { _toast.value = null }

    // ── Helper HTTP GET robusto ──────────────────────────────────────────
    private fun httpGet(endpoint: String, token: String): String {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.setRequestProperty("apikey", anonKey)
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.connectTimeout = 10_000
        conn.readTimeout    = 10_000
        return try {
            val code = conn.responseCode
            if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                throw Exception("HTTP $code: $err")
            }
        } finally {
            conn.disconnect()
        }
    }

    // ── Helper HTTP POST/PATCH/DELETE robusto ──────────────────────────────
    private fun httpPost(
        endpoint: String, token: String, body: String,
        method: String = "POST", extraHeaders: Map<String,String> = emptyMap()
    ): String {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("apikey", anonKey)
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 10_000
        conn.readTimeout    = 15_000
        extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        if (body.isNotEmpty()) {
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream).use { it.write(body) }
        }
        return try {
            val code = conn.responseCode
            if (code in 200..299) {
                conn.inputStream?.bufferedReader()?.readText() ?: ""
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                throw Exception("Errore $code: $err")
            }
        } finally {
            conn.disconnect()
        }
    }

    // ── Helpers HTTP ──────────────────────────────────────────────────────────
    private fun postListing(
        token: String, title: String, city: String, zone: String,
        price: Double, sqm: Int?, rooms: Int?, bathrooms: Int?,
        floor: String, furnished: Boolean, availableFrom: String,
        description: String, contactType: String,
        contactPhone: String, contactEmail: String, contactWhatsapp: String
    ): String? {
        val userId = getUserIdFromToken(token)
            ?: throw Exception("Impossibile leggere l'ID utente dal token")
        val body = JSONObject().apply {
            put("landlord_id", userId)          // <─ obbligatorio per RLS
            put("title", title); put("city", city); put("zone", zone)
            put("price_monthly", price)
            if (sqm != null) put("sqm", sqm)
            if (rooms != null) put("rooms", rooms)
            if (bathrooms != null) put("bathrooms", bathrooms)
            put("floor", floor); put("furnished", furnished)
            if (availableFrom.isNotBlank()) put("available_from", availableFrom)
            put("description", description); put("contact_type", contactType)
            put("contact_phone", contactPhone); put("contact_email", contactEmail)
            put("contact_whatsapp", contactWhatsapp)
        }.toString()
        val response = httpPost(
            "$baseUrl/rest/v1/listings", token, body,
            extraHeaders = mapOf("Prefer" to "return=representation")
        )
        return JSONArray(response).optJSONObject(0)?.optString("id")
    }

    /** Decodifica il JWT e restituisce il campo 'sub' (UUID utente) */
    private fun getUserIdFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val padded = parts[1].let { it.padEnd((it.length + 3) / 4 * 4, '=') }
            val payload = String(android.util.Base64.decode(padded, android.util.Base64.URL_SAFE))
            JSONObject(payload).optString("sub").takeIf { it.isNotBlank() }
        } catch (e: Exception) { null }
    }

    private fun uploadPhoto(token: String, uri: Uri, listingId: String): String? {
        return try {
            val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: return null
            val filename = "$listingId/${UUID.randomUUID()}.jpg"
            val conn = URL("$baseUrl/storage/v1/object/listing-photos/$filename")
                .openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "image/jpeg")
            conn.doOutput = true
            conn.outputStream.use { it.write(bytes) }
            if (conn.responseCode in 200..299) {
                "$baseUrl/storage/v1/object/public/listing-photos/$filename"
            } else null
        } catch (e: Exception) { null }
    }

    private fun savePhotoRecord(
        token: String, listingId: String, url: String, isCover: Boolean, order: Int
    ) {
        val body = JSONObject().apply {
            put("listing_id", listingId); put("url", url)
            put("is_cover", isCover); put("display_order", order)
        }.toString()
        httpPost(
            "$baseUrl/rest/v1/listing_photos", token, body,
            extraHeaders = mapOf("Prefer" to "return=minimal")
        )
    }

    // ── Parser JSON → photos ──────────────────────────────────────────────────
    private fun parsePhotos(arr: JSONArray, listingId: String): List<ListingPhoto> =
        (0 until arr.length()).map { j ->
            val p = arr.getJSONObject(j)
            ListingPhoto(
                id = p.optString("id"),
                listingId = listingId,
                url = p.optString("url"),
                isCover = p.optBoolean("is_cover"),
                displayOrder = p.optInt("display_order")
            )
        }.sortedBy { it.displayOrder }

    // ── Parser JSON → Listing (senza foto embed) ──────────────────────────────
    private fun parseListings(arr: JSONArray): List<Listing> {
        val list = mutableListOf<Listing>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val photosArr = o.optJSONArray("listing_photos") ?: JSONArray()
            val photos = (0 until photosArr.length()).map { j ->
                val p = photosArr.getJSONObject(j)
                ListingPhoto(
                    id = p.optString("id"),
                    listingId = o.optString("id"),
                    url = p.optString("url"),
                    isCover = p.optBoolean("is_cover"),
                    displayOrder = p.optInt("display_order")
                )
            }.sortedBy { it.displayOrder }
            list.add(
                Listing(
                    id = o.optString("id"),
                    landlordId = o.optString("landlord_id"),
                    title = o.optString("title"),
                    city = o.optString("city"),
                    zone = o.optString("zone"),
                    priceMonthly = o.optDouble("price_monthly", 0.0),
                    sqm = o.optInt("sqm").takeIf { it > 0 },
                    rooms = o.optInt("rooms").takeIf { it > 0 },
                    bathrooms = o.optInt("bathrooms").takeIf { it > 0 },
                    floor = o.optString("floor"),
                    furnished = o.optBoolean("furnished"),
                    availableFrom = o.optString("available_from"),
                    description = o.optString("description"),
                    contactType = o.optString("contact_type", "direct"),
                    contactPhone = o.optString("contact_phone"),
                    contactEmail = o.optString("contact_email"),
                    contactWhatsapp = o.optString("contact_whatsapp"),
                    isActive = o.optBoolean("is_active", true),
                    isAvailable = o.optBoolean("is_available", true),
                    createdAt = o.optString("created_at"),
                    photos = photos
                )
            )
        }
        return list
    }
}

// ─── Factory ──────────────────────────────────────────────────────────────────
class ListingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val prefs = context.getSharedPreferences("renttrack_prefs", Context.MODE_PRIVATE)
        @Suppress("UNCHECKED_CAST")
        return ListingsViewModel(prefs, context.contentResolver) as T
    }
}
