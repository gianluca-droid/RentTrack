package com.renttrack.app.data.model

// ─── Modelli Supabase — IDs come String UUID ──────────────────────────────────
// Usati da SupabaseRentRepository in sostituzione ai modelli Room (Long ID)

data class SCondominio(
    val id: String            = "",
    val ownerId: String       = "",
    val nome: String          = "",
    val indirizzo: String     = "",
    val citta: String         = "",
    val cap: String           = "",
    val provincia: String     = "",
    val note: String          = "",
    val createdAt: String     = ""
)

data class SCondoUnit(
    val id: String            = "",
    val condominioId: String  = "",
    val ownerId: String       = "",
    val number: String        = "",
    val floor: Int            = 0,
    val type: String          = "Appartamento",
    val areaMq: Double        = 0.0,
    val scala: String         = "",
    val ownerName: String     = "",
    val ownerEmail: String    = "",
    val ownerPhone: String    = "",
    val millesimi: Double     = 0.0,       // affitto mensile (€)
    val leaseStartDate: Long? = null,
    val leaseEndDate: Long?   = null,
    val paymentDayOfMonth: Int = 5,
    val note: String          = "",
    val createdAt: String     = ""
)

data class SExpense(
    val id: String            = "",
    val condominioId: String  = "",
    val ownerId: String       = "",
    val category: String      = "",
    val description: String   = "",
    val amount: Double        = 0.0,
    val date: Long            = System.currentTimeMillis(),
    val notes: String         = "",
    val createdAt: String     = ""
)

data class SCedolino(
    val id: String            = "",
    val unitId: String        = "",
    val condominioId: String  = "",
    val ownerId: String       = "",
    val period: String        = "",
    val issueDate: Long       = System.currentTimeMillis(),
    val dueDate: Long         = System.currentTimeMillis(),
    val total: Double         = 0.0,
    val paidAmount: Double    = 0.0,
    val status: String        = "Emesso",
    val sentToResident: Boolean = false,
    val sentAt: Long?         = null,
    val paidDate: Long?       = null,
    val createdAt: String     = ""
)

data class SCedolinoItem(
    val id: String            = "",
    val cedolinoId: String    = "",
    val description: String   = "",
    val amount: Double        = 0.0
)

data class SCedolinoWithItems(
    val cedolino: SCedolino,
    val items: List<SCedolinoItem>
)

data class SPayment(
    val id: String            = "",
    val unitId: String        = "",
    val condominioId: String  = "",
    val ownerId: String       = "",
    val amount: Double        = 0.0,
    val date: Long            = System.currentTimeMillis(),
    val method: String        = "Bonifico",
    val reference: String     = "",
    val notes: String         = "",
    val cedolinoId: String?   = null,
    val createdAt: String     = ""
)

data class STenantHistory(
    val id: String            = "",
    val unitId: String        = "",
    val condominioId: String  = "",
    val ownerId: String       = "",
    val tenantName: String    = "",
    val tenantEmail: String   = "",
    val tenantPhone: String   = "",
    val leaseStart: Long?     = null,
    val leaseEnd: Long?       = null,
    val monthlyRent: Double   = 0.0,
    val exitNotes: String     = "",
    val exitDate: Long        = System.currentTimeMillis(),
    val createdAt: String     = ""
)

data class SDocumento(
    val id: String            = "",
    val condominioId: String  = "",
    val ownerId: String       = "",
    val titolo: String        = "",
    val categoria: String     = "",
    val filePath: String      = "",
    val fileName: String      = "",
    val fileSize: Long        = 0L,
    val fileType: String      = "",
    val note: String          = "",
    val sommario: String      = "",
    val visibilita: String    = "Tutti",
    val destinatariUnitIds: String = "",
    val createdAt: String     = ""
)

/** Riepilogo per singola proprietà (Supabase version) */
data class SPropertySummary(
    val unitCount: Int         = 0,
    val totalMonthlyRent: Double = 0.0,
    val totalMorosita: Double  = 0.0,
    val expiringContracts: Int = 0
)
