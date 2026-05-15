package com.renttrack.app.data.repository

import android.content.SharedPreferences
import com.renttrack.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

class SupabaseRentRepository(private val prefs: SharedPreferences) {

    private val baseUrl = "https://zjqrtuposdrimzjoydgh.supabase.co/rest/v1"
    private val anonKey =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
        "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpqcXJ0dXBvc2RyaW16am95ZGdoIiwi" +
        "cm9sZSI6ImFub24iLCJpYXQiOjE3NzgzMzE5MDMsImV4cCI6MjA5MzkwNzkwM30." +
        "dLvc0pPrfIXiBGJSDTnRtNU6FRppFPSr86pLwHD35j4"

    private val token: String get() = prefs.getString("auth_token", "") ?: ""

    val userId: String get() {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return ""
            val padded = parts[1].let { it.padEnd((it.length + 3) / 4 * 4, '=') }
            val payload = String(android.util.Base64.decode(padded, android.util.Base64.URL_SAFE))
            JSONObject(payload).optString("sub")
        } catch (e: Exception) { "" }
    }

    // ── Auth guard ────────────────────────────────────────────────────────────

    /** Restituisce il token corrente o lancia eccezione — impedisce Bearer vuoto. */
    private fun requireToken(): String {
        val t = token
        if (t.isBlank()) throw Exception("Not authenticated — accedi di nuovo")
        return t
    }

    /** Prova a rinnovare l'access_token usando il refresh_token salvato.
     *  Aggiorna SharedPreferences e restituisce true in caso di successo. */
    private fun tryRefreshToken(): Boolean {
        val refreshToken = prefs.getString("refresh_token", null) ?: return false
        return try {
            val body = """{"refresh_token":"$refreshToken"}"""
            val conn = URL("https://zjqrtuposdrimzjoydgh.supabase.co/auth/v1/token?grant_type=refresh_token")
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

    // ── HTTP Helpers ──────────────────────────────────────────────────────────

    private fun get(path: String): String = getInternal(path, requireToken(), retried = false)

    private fun getInternal(path: String, tok: String, retried: Boolean): String {
        val conn = URL("$baseUrl$path").openConnection() as HttpURLConnection
        conn.setRequestProperty("apikey", anonKey)
        conn.setRequestProperty("Authorization", "Bearer $tok")
        conn.connectTimeout = 10_000; conn.readTimeout = 10_000
        return try {
            val code = conn.responseCode
            when {
                code in 200..299 -> conn.inputStream.bufferedReader().readText()
                code == 401 && !retried && tryRefreshToken() ->
                    getInternal(path, requireToken(), retried = true)
                else -> throw Exception("HTTP $code: ${conn.errorStream?.bufferedReader()?.readText()}")
            }
        } finally { conn.disconnect() }
    }

    private fun post(path: String, body: String, method: String = "POST", prefer: String = "return=representation"): String =
        postInternal(path, body, method, prefer, requireToken(), retried = false)

    private fun postInternal(path: String, body: String, method: String, prefer: String, tok: String, retried: Boolean): String {
        val conn = URL("$baseUrl$path").openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("apikey", anonKey)
        conn.setRequestProperty("Authorization", "Bearer $tok")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Prefer", prefer)
        conn.connectTimeout = 10_000; conn.readTimeout = 15_000
        if (body.isNotEmpty()) {
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream).use { it.write(body) }
        }
        return try {
            val code = conn.responseCode
            when {
                code in 200..299 -> conn.inputStream?.bufferedReader()?.readText() ?: ""
                code == 401 && !retried && tryRefreshToken() ->
                    postInternal(path, body, method, prefer, requireToken(), retried = true)
                else -> throw Exception("HTTP $code: ${conn.errorStream?.bufferedReader()?.readText()}")
            }
        } finally { conn.disconnect() }
    }

    // ── Condomini ─────────────────────────────────────────────────────────────

    suspend fun getCondomini(): List<SCondominio> = withContext(Dispatchers.IO) {
        val uid = userId
        val arr = JSONArray(get("/condomini?owner_id=eq.$uid&order=nome.asc"))
        (0 until arr.length()).map { parseCondominio(arr.getJSONObject(it)) }
    }

    suspend fun insertCondominio(c: SCondominio): String = withContext(Dispatchers.IO) {
        val uid = userId
        val body = JSONObject().apply {
            put("owner_id", uid)
            put("nome", c.nome)
            put("indirizzo", c.indirizzo)
            put("citta", c.citta)
            put("note", c.note)
        }.toString()
        val res = JSONArray(post("/condomini", body))
        res.getJSONObject(0).getString("id")
    }

    suspend fun updateCondominio(c: SCondominio) = withContext(Dispatchers.IO) {
        val uid = userId
        val body = JSONObject().apply {
            put("nome", c.nome)
            put("indirizzo", c.indirizzo)
            put("citta", c.citta)
            put("note", c.note)
        }.toString()
        post("/condomini?id=eq.${c.id}&owner_id=eq.$uid", body, "PATCH", "return=minimal")
    }

    suspend fun deleteCondominio(id: String) = withContext(Dispatchers.IO) {
        val uid = userId
        post("/condomini?id=eq.$id&owner_id=eq.$uid", "", "DELETE", "return=minimal")
    }

    suspend fun getCondominioById(id: String): SCondominio? = withContext(Dispatchers.IO) {
        val arr = JSONArray(get("/condomini?id=eq.$id"))
        if (arr.length() > 0) parseCondominio(arr.getJSONObject(0)) else null
    }

    // ── Units ─────────────────────────────────────────────────────────────────

    suspend fun getUnitsByCondominio(condominioId: String): List<SCondoUnit> = withContext(Dispatchers.IO) {
        val arr = JSONArray(get("/units?condominio_id=eq.$condominioId&order=number.asc"))
        (0 until arr.length()).map { parseUnit(arr.getJSONObject(it)) }
    }

    suspend fun getAllUnits(): List<SCondoUnit> = withContext(Dispatchers.IO) {
        val uid = userId
        val arr = JSONArray(get("/units?owner_id=eq.$uid&order=condominio_id.asc,number.asc"))
        (0 until arr.length()).map { parseUnit(arr.getJSONObject(it)) }
    }

    suspend fun insertUnit(u: SCondoUnit): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("owner_id", userId); put("condominio_id", u.condominioId)
            put("number", u.number); put("floor", u.floor)
            put("type", u.type.ifBlank { "Appartamento" })
            put("area_mq", u.areaMq); put("scala", u.scala)
            put("owner_name", u.ownerName)
            put("owner_email", u.ownerEmail); put("owner_phone", u.ownerPhone)
            put("canone_mensile", u.millesimi)   // DB: canone_mensile (NOT NULL)
            put("payment_day", u.paymentDayOfMonth)  // DB: payment_day (NOT NULL)
            u.leaseStartDate?.let { put("lease_start_date", it) }
            u.leaseEndDate?.let { put("lease_end_date", it) }
        }.toString()
        val res = JSONArray(post("/units", body))
        res.getJSONObject(0).getString("id")
    }

    suspend fun updateUnit(u: SCondoUnit) = withContext(Dispatchers.IO) {
        val uid = userId
        val body = JSONObject().apply {
            put("number", u.number); put("floor", u.floor)
            put("type", u.type.ifBlank { "Appartamento" })
            put("area_mq", u.areaMq); put("scala", u.scala)
            put("owner_name", u.ownerName)
            put("owner_email", u.ownerEmail); put("owner_phone", u.ownerPhone)
            put("canone_mensile", u.millesimi)   // DB: canone_mensile (NOT NULL)
            put("payment_day", u.paymentDayOfMonth)  // DB: payment_day (NOT NULL)
            if (u.leaseStartDate != null) put("lease_start_date", u.leaseStartDate) else put("lease_start_date", JSONObject.NULL)
            if (u.leaseEndDate != null) put("lease_end_date", u.leaseEndDate) else put("lease_end_date", JSONObject.NULL)
        }.toString()
        post("/units?id=eq.${u.id}&owner_id=eq.$uid", body, "PATCH", "return=minimal")
    }

    suspend fun deleteUnit(id: String) = withContext(Dispatchers.IO) {
        val uid = userId
        post("/units?id=eq.$id&owner_id=eq.$uid", "", "DELETE", "return=minimal")
    }

    // ── Expenses ──────────────────────────────────────────────────────────────

    suspend fun getExpensesByCondominio(condominioId: String): List<SExpense> = withContext(Dispatchers.IO) {
        val arr = JSONArray(get("/expenses?condominio_id=eq.$condominioId&order=date.desc"))
        (0 until arr.length()).map { parseExpense(arr.getJSONObject(it)) }
    }

    suspend fun insertExpense(e: SExpense): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("owner_id", userId); put("condominio_id", e.condominioId)
            put("category", e.category); put("description", e.description)
            put("amount", e.amount); put("date", e.date); put("notes", e.notes)
        }.toString()
        val res = JSONArray(post("/expenses", body))
        res.getJSONObject(0).getString("id")
    }

    suspend fun updateExpense(e: SExpense) = withContext(Dispatchers.IO) {
        val uid = userId
        val body = JSONObject().apply {
            put("category", e.category); put("description", e.description)
            put("amount", e.amount); put("date", e.date); put("notes", e.notes)
        }.toString()
        post("/expenses?id=eq.${e.id}&owner_id=eq.$uid", body, "PATCH", "return=minimal")
    }

    suspend fun deleteExpense(id: String) = withContext(Dispatchers.IO) {
        val uid = userId
        post("/expenses?id=eq.$id&owner_id=eq.$uid", "", "DELETE", "return=minimal")
    }

    // ── Cedolini ──────────────────────────────────────────────────────────────

    suspend fun getCedoliniByCondominio(condominioId: String): List<SCedolino> = withContext(Dispatchers.IO) {
        val arr = JSONArray(get("/cedolini?condominio_id=eq.$condominioId&order=due_date.desc"))
        (0 until arr.length()).map { parseCedolino(arr.getJSONObject(it)) }
    }

    suspend fun getCedolinoItems(cedolinoId: String): List<SCedolinoItem> = withContext(Dispatchers.IO) {
        val arr = JSONArray(get("/cedolino_items?cedolino_id=eq.$cedolinoId"))
        (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            SCedolinoItem(
                id = o.optString("id"), cedolinoId = cedolinoId,
                description = o.optString("description"), amount = o.optDouble("amount")
            )
        }
    }

    suspend fun insertCedolinoWithItems(c: SCedolino, items: List<SCedolinoItem>): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("owner_id", userId); put("unit_id", c.unitId); put("condominio_id", c.condominioId)
            put("period", c.period); put("issue_date", c.issueDate); put("due_date", c.dueDate)
            put("total", c.total); put("paid_amount", c.paidAmount); put("status", c.status)
            put("sent_to_resident", c.sentToResident)
            c.sentAt?.let { put("sent_at", it) }
            c.paidDate?.let { put("paid_date", it) }
        }.toString()
        val res = JSONArray(post("/cedolini", body))
        val cedId = res.getJSONObject(0).getString("id")
        // Inserisce gli item — owner_id è obbligatorio (NOT NULL, nessun DEFAULT DB)
        items.forEach { item ->
            val itemBody = JSONObject().apply {
                put("cedolino_id", cedId)
                put("owner_id", userId)       // ← fix: richiesto da RLS + vincolo NOT NULL
                put("description", item.description)
                put("amount", item.amount)
            }.toString()
            post("/cedolino_items", itemBody, prefer = "return=minimal")
        }

        cedId
    }

    suspend fun updateCedolino(c: SCedolino) = withContext(Dispatchers.IO) {
        val uid = userId
        val body = JSONObject().apply {
            // Status & payment fields
            put("status", c.status)
            put("paid_amount", c.paidAmount)
            put("sent_to_resident", c.sentToResident)
            if (c.sentAt != null) put("sent_at", c.sentAt) else put("sent_at", JSONObject.NULL)
            if (c.paidDate != null) put("paid_date", c.paidDate) else put("paid_date", JSONObject.NULL)
            // Editable fields (period, amount, dates) — Fix: previously missing from PATCH
            put("period", c.period)
            put("total", c.total)
            put("issue_date", c.issueDate)
            if (c.dueDate != null) put("due_date", c.dueDate) else put("due_date", JSONObject.NULL)
        }.toString()
        post("/cedolini?id=eq.${c.id}&owner_id=eq.$uid", body, "PATCH", "return=minimal")
    }

    /**
     * Elimina un cedolino e i suoi dati correlati.
     *
     * Cascata payments:
     *  a) DELETE esatto: payments con cedolino_id = id  (caso markCedolinoPaidWithPayment)
     *  b) DELETE time-scoped: payments senza cedolino_id registrati nella finestra
     *     temporale del cedolino (±45 giorni dal dueDate) per la stessa unit/condominio.
     *     Gestisce il caso in cui il pagamento era stato inserito manualmente
     *     senza collegarlo al cedolino.
     *     La finestra limita il rischio di eliminare pagamenti di altri mesi.
     *
     * @param id           UUID del cedolino
     * @param unitId       UUID dell'unità (per DELETE time-scoped)
     * @param condominioId UUID del condominio (per DELETE time-scoped)
     * @param dueDateMs    Timestamp scadenza cedolino in ms (per finestra temporale)
     */
    suspend fun deleteCedolino(
        id: String,
        unitId: String = "",
        condominioId: String = "",
        dueDateMs: Long = 0L
    ) = withContext(Dispatchers.IO) {
        val uid = userId
        // 1. DELETE primario: payment esplicitamente collegati (cedolino_id = id)
        post("/payments?cedolino_id=eq.$id&owner_id=eq.$uid", "", "DELETE", "return=minimal")
        // 2. DELETE time-scoped: payment manuali (cedolino_id = null) nella finestra del cedolino
        //    Finestra: dueDate -45 giorni ... dueDate +45 giorni
        //    Elimina solo payment senza cedolino_id della stessa unit, evita di colpire altri mesi
        if (unitId.isNotBlank() && condominioId.isNotBlank() && dueDateMs > 0L) {
            val windowMs = 45L * 24 * 60 * 60 * 1000   // 45 giorni in ms
            val from = dueDateMs - windowMs
            val to   = dueDateMs + windowMs
            post(
                "/payments?unit_id=eq.$unitId&condominio_id=eq.$condominioId" +
                "&cedolino_id=is.null&owner_id=eq.$uid" +
                "&date=gte.$from&date=lte.$to",
                "", "DELETE", "return=minimal"
            )
        }
        // 3. Elimina le voci del cedolino
        post("/cedolino_items?cedolino_id=eq.$id&owner_id=eq.$uid", "", "DELETE", "return=minimal")
        // 4. Elimina il cedolino
        post("/cedolini?id=eq.$id&owner_id=eq.$uid", "", "DELETE", "return=minimal")
    }

    // ── Payments ──────────────────────────────────────────────────────────────

    suspend fun getPaymentsByCondominio(condominioId: String): List<SPayment> = withContext(Dispatchers.IO) {
        val arr = JSONArray(get("/payments?condominio_id=eq.$condominioId&order=date.desc"))
        (0 until arr.length()).map { parsePayment(arr.getJSONObject(it)) }
    }

    suspend fun insertPayment(p: SPayment): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("owner_id", userId); put("unit_id", p.unitId)
            put("condominio_id", p.condominioId)  // nullable in DB, ma necessario per i filtri di lettura
            put("amount", p.amount); put("date", p.date); put("method", p.method)
            put("reference", p.reference); put("notes", p.notes)
            p.cedolinoId?.let { put("cedolino_id", it) }
        }.toString()
        val res = JSONArray(post("/payments", body))
        res.getJSONObject(0).getString("id")
    }

    suspend fun deletePayment(id: String) = withContext(Dispatchers.IO) {
        val uid = userId
        post("/payments?id=eq.$id&owner_id=eq.$uid", "", "DELETE", "return=minimal")
    }

    // ── TenantHistory ─────────────────────────────────────────────────────────

    suspend fun getTenantHistory(condominioId: String): List<STenantHistory> = withContext(Dispatchers.IO) {
        val arr = JSONArray(get("/tenant_history?condominio_id=eq.$condominioId&order=exit_date.desc"))
        (0 until arr.length()).map { parseTenantHistory(arr.getJSONObject(it)) }
    }

    suspend fun getTenantHistoryByUnit(unitId: String): List<STenantHistory> = withContext(Dispatchers.IO) {
        val arr = JSONArray(get("/tenant_history?unit_id=eq.$unitId&order=exit_date.desc"))
        (0 until arr.length()).map { parseTenantHistory(arr.getJSONObject(it)) }
    }

    suspend fun insertTenantHistory(h: STenantHistory) = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("owner_id", userId); put("unit_id", h.unitId); put("condominio_id", h.condominioId)
            // DB usa owner_name/owner_email/owner_phone (non tenant_name etc.)
            put("owner_name", h.tenantName)
            put("owner_email", h.tenantEmail)
            put("owner_phone", h.tenantPhone)
            h.leaseStart?.let { put("lease_start", it) }
            h.leaseEnd?.let { put("lease_end", it) }
            put("monthly_rent", h.monthlyRent)
            put("exit_notes", h.exitNotes)
            // exit_date è nullable nel DB, inviamo solo se non zero
            if (h.exitDate > 0) put("exit_date", h.exitDate)
        }.toString()
        post("/tenant_history", body, prefer = "return=minimal")
    }

    suspend fun deleteTenantHistory(id: String) = withContext(Dispatchers.IO) {
        val uid = userId
        post("/tenant_history?id=eq.$id&owner_id=eq.$uid", "", "DELETE", "return=minimal")
    }

    /**
     * Elimina lo storico inquilino e, opzionalmente, tutti i pagamenti registrati
     * durante il periodo di locazione di quell'inquilino.
     *
     * @param h            Record STenantHistory da eliminare
     * @param deleteOrphanPayments  Se true, elimina i payment dell'unità che non
     *                     hanno cedolino_id (registrati manualmente nel periodo).
     *                     Default false per sicurezza (non cancella dati storici validi).
     */
    suspend fun deleteTenantHistoryWithCascade(
        h: STenantHistory,
        deleteOrphanPayments: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val uid = userId
        if (deleteOrphanPayments && h.unitId.isNotBlank() && h.condominioId.isNotBlank()) {
            // Elimina payment orfani (senza cedolino_id) per questa unità
            post("/payments?unit_id=eq.${h.unitId}&condominio_id=eq.${h.condominioId}&cedolino_id=is.null&owner_id=eq.$uid",
                 "", "DELETE", "return=minimal")
        }
        post("/tenant_history?id=eq.${h.id}&owner_id=eq.$uid", "", "DELETE", "return=minimal")
    }

    // ── JSON Parsers ──────────────────────────────────────────────────────────

    private fun parseCondominio(o: JSONObject) = SCondominio(
        id = o.optString("id"), ownerId = o.optString("owner_id"),
        nome = o.optString("nome"), indirizzo = o.optString("indirizzo"),
        citta = o.optString("citta"), cap = o.optString("cap"),
        provincia = o.optString("provincia"), note = o.optString("note"),
        createdAt = o.optString("created_at")
    )

    private fun parseUnit(o: JSONObject) = SCondoUnit(
        id = o.optString("id"), condominioId = o.optString("condominio_id"),
        ownerId = o.optString("owner_id"), number = o.optString("number"),
        floor = o.optInt("floor", 0), type = o.optString("type").ifBlank { "Appartamento" },
        areaMq = o.optDouble("area_mq", 0.0), scala = o.optString("scala"),
        ownerName = o.optString("owner_name"), ownerEmail = o.optString("owner_email"),
        ownerPhone = o.optString("owner_phone"),
        millesimi = o.optDouble("canone_mensile", 0.0),  // DB: canone_mensile
        leaseStartDate = o.optLong("lease_start_date").takeIf { it > 0 },
        leaseEndDate = o.optLong("lease_end_date").takeIf { it > 0 },
        paymentDayOfMonth = o.optInt("payment_day", 5),  // DB: payment_day
        note = o.optString("note"), createdAt = o.optString("created_at")
    )

    private fun parseExpense(o: JSONObject) = SExpense(
        id = o.optString("id"), condominioId = o.optString("condominio_id"),
        ownerId = o.optString("owner_id"), category = o.optString("category"),
        description = o.optString("description"), amount = o.optDouble("amount"),
        date = o.optLong("date"), notes = o.optString("notes"),
        createdAt = o.optString("created_at")
    )

    private fun parseCedolino(o: JSONObject) = SCedolino(
        id = o.optString("id"), unitId = o.optString("unit_id"),
        condominioId = o.optString("condominio_id"), ownerId = o.optString("owner_id"),
        period = o.optString("period"), issueDate = o.optLong("issue_date"),
        dueDate = o.optLong("due_date"), total = o.optDouble("total"),
        paidAmount = o.optDouble("paid_amount"), status = o.optString("status", "Emesso"),
        sentToResident = o.optBoolean("sent_to_resident"),
        sentAt = o.optLong("sent_at").takeIf { it > 0 },
        paidDate = o.optLong("paid_date").takeIf { it > 0 },
        createdAt = o.optString("created_at")
    )

    private fun parsePayment(o: JSONObject) = SPayment(
        id = o.optString("id"), unitId = o.optString("unit_id"),
        condominioId = o.optString("condominio_id"), ownerId = o.optString("owner_id"),
        amount = o.optDouble("amount"), date = o.optLong("date"),
        method = o.optString("method"), reference = o.optString("reference"),
        notes = o.optString("notes"),
        cedolinoId = o.optString("cedolino_id").takeIf { it.isNotBlank() },
        createdAt = o.optString("created_at")
    )

    private fun parseTenantHistory(o: JSONObject) = STenantHistory(
        id = o.optString("id"), unitId = o.optString("unit_id"),
        condominioId = o.optString("condominio_id"), ownerId = o.optString("owner_id"),
        // DB usa owner_name/owner_email/owner_phone per i dati del tenant
        tenantName = o.optString("owner_name"),
        tenantEmail = o.optString("owner_email"),
        tenantPhone = o.optString("owner_phone"),
        leaseStart = o.optLong("lease_start").takeIf { it > 0 },
        leaseEnd = o.optLong("lease_end").takeIf { it > 0 },
        monthlyRent = o.optDouble("monthly_rent"), exitNotes = o.optString("exit_notes"),
        exitDate = o.optLong("exit_date"), createdAt = o.optString("created_at")
    )

    // ── Documenti ─────────────────────────────────────────────────────────────

    suspend fun getDocumenti(condominioId: String): List<SDocumento> = withContext(Dispatchers.IO) {
        val arr = JSONArray(get("/documenti?condominio_id=eq.$condominioId&order=created_at.desc"))
        (0 until arr.length()).map { parseDocumento(arr.getJSONObject(it)) }
    }

    suspend fun insertDocumento(doc: SDocumento): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("owner_id", userId)
            put("condominio_id", doc.condominioId)
            put("titolo", doc.titolo)
            put("categoria", doc.categoria)
            put("file_path", doc.filePath)
            put("file_name", doc.fileName)
            put("file_size", doc.fileSize)
            put("file_type", doc.fileType)
            put("note", doc.note)
            put("sommario", doc.sommario)
            put("visibilita", doc.visibilita)
            put("destinatari_unit_ids", doc.destinatariUnitIds)
        }.toString()
        val res = JSONArray(post("/documenti", body))
        res.getJSONObject(0).getString("id")
    }

    suspend fun updateDocumento(doc: SDocumento) = withContext(Dispatchers.IO) {
        val uid = userId
        val body = JSONObject().apply {
            put("titolo", doc.titolo)
            put("categoria", doc.categoria)
            put("note", doc.note)
            put("sommario", doc.sommario)
            put("visibilita", doc.visibilita)
            put("destinatari_unit_ids", doc.destinatariUnitIds)
        }.toString()
        post("/documenti?id=eq.${doc.id}&owner_id=eq.$uid", body, "PATCH", "return=minimal")
    }

    suspend fun deleteDocumento(id: String) = withContext(Dispatchers.IO) {
        val uid = userId
        post("/documenti?id=eq.$id&owner_id=eq.$uid", "", "DELETE", "return=minimal")
    }

    /**
     * Carica un file su Supabase Storage nel bucket "documenti".
     * Restituisce il path pubblico del file oppure null in caso di errore.
     */
    suspend fun uploadDocumentoFile(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        condominioId: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val storageBase = "https://zjqrtuposdrimzjoydgh.supabase.co/storage/v1"
            val filePath = "$condominioId/${java.util.UUID.randomUUID()}_$fileName"
            val uploadUrl = "$storageBase/object/documenti/$filePath"

            val conn = java.net.URL(uploadUrl).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", mimeType)
            conn.setRequestProperty("x-upsert", "true")
            conn.connectTimeout = 30_000
            conn.readTimeout    = 60_000
            conn.doOutput = true
            conn.outputStream.use { it.write(fileBytes) }

            if (conn.responseCode in 200..299) {
                filePath   // restituisce il path relativo; per l'URL pubblico: storageBase/object/public/documenti/$filePath
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText()
                android.util.Log.e("Repository", "Upload doc error ${conn.responseCode}: $err")
                null
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Upload doc exception: ${e.message}")
            null
        }
    }

    /** URL pubblico di un file nel bucket "documenti" */
    fun getDocumentoPublicUrl(filePath: String): String =
        "https://zjqrtuposdrimzjoydgh.supabase.co/storage/v1/object/public/documenti/$filePath"

    private fun parseDocumento(o: JSONObject) = SDocumento(
        id                = o.optString("id"),
        condominioId      = o.optString("condominio_id"),
        ownerId           = o.optString("owner_id"),
        titolo            = o.optString("titolo"),
        categoria         = o.optString("categoria"),
        filePath          = o.optString("file_path"),
        fileName          = o.optString("file_name"),
        fileSize          = o.optLong("file_size"),
        fileType          = o.optString("file_type"),
        note              = o.optString("note"),
        sommario          = o.optString("sommario"),
        visibilita        = o.optString("visibilita", "Tutti"),
        destinatariUnitIds = o.optString("destinatari_unit_ids"),
        createdAt         = o.optString("created_at")
    )
}
