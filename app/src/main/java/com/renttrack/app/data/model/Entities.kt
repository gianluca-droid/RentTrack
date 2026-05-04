package com.renttrack.app.data.model

import androidx.room.*

// ─── Condominio ──────────────────────────────────────────────────────
@Entity(tableName = "condomini")
data class Condominio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val indirizzo: String,
    val citta: String,
    val cf: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Unità Condominiale ─────────────────────────────────────────────
@Entity(
    tableName = "units",
    foreignKeys = [ForeignKey(
        entity = Condominio::class,
        parentColumns = ["id"], childColumns = ["condominioId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("condominioId")]
)
data class CondoUnit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val condominioId: Long,
    val number: String,
    val floor: Int,
    val type: String,
    val areaMq: Double,
    val millesimi: Double,
    val ownerName: String,
    val ownerEmail: String = "",
    val ownerPhone: String = "",
    val scala: String = ""    // es. "A", "B", "1", "2" — vuoto = nessuna scala
)

// ─── Spesa Condominiale ─────────────────────────────────────────────
@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(
        entity = Condominio::class,
        parentColumns = ["id"], childColumns = ["condominioId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("condominioId")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val condominioId: Long,
    val date: Long,
    val category: String,
    val description: String,
    val amount: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Pagamento ──────────────────────────────────────────────────────
@Entity(
    tableName = "payments",
    foreignKeys = [ForeignKey(
        entity = CondoUnit::class,
        parentColumns = ["id"], childColumns = ["unitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("unitId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long,
    val amount: Double,
    val date: Long,
    val method: String,
    val reference: String = "",
    val cedolinoId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Cedolino ───────────────────────────────────────────────────────
@Entity(
    tableName = "cedolini",
    foreignKeys = [ForeignKey(
        entity = CondoUnit::class,
        parentColumns = ["id"], childColumns = ["unitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("unitId")]
)
data class Cedolino(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long,
    val period: String,
    val issueDate: Long,
    val dueDate: Long,
    val total: Double,
    val status: String,
    val paidAmount: Double = 0.0,
    val paidDate: Long? = null,
    val sentToResident: Boolean = false,  // true = inviato al condomino
    val sentAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Voce Cedolino ──────────────────────────────────────────────────
@Entity(
    tableName = "cedolino_items",
    foreignKeys = [ForeignKey(
        entity = Cedolino::class,
        parentColumns = ["id"], childColumns = ["cedolinoId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cedolinoId")]
)
data class CedolinoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cedolinoId: Long,
    val description: String,
    val amount: Double
)

// ─── Documento ───────────────────────────────────────────────────────
@Entity(
    tableName = "documents",
    foreignKeys = [ForeignKey(
        entity = Condominio::class,
        parentColumns = ["id"], childColumns = ["condominioId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("condominioId")]
)
data class Documento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val condominioId: Long,
    val titolo: String,
    val categoria: String,
    val fileType: String = "PDF",   // PDF, Word, Foto, Altro
    val filePath: String,
    val fileName: String,
    val fileSize: Long = 0L,
    val dataInserimento: Long = System.currentTimeMillis(),
    val note: String = "",
    // ─── Nuovi campi v6 ───────────────────────────────────────────────
    /** Sintesi/riassunto del documento leggibile dal condomino prima di aprire il file */
    val sommario: String = "",
    /** "Tutti" = visibile a tutto il condominio | "Singoli" = solo alle unità in destinatariUnitIds */
    val visibilita: String = "Tutti",
    /** ID unità destinatarie separati da virgola, es. "1,3,7". Usato solo se visibilita="Singoli" */
    val destinatariUnitIds: String = ""
)

// ─── Relazioni ──────────────────────────────────────────────────────
data class CedolinoWithItems(
    @Embedded val cedolino: Cedolino,
    @Relation(parentColumn = "id", entityColumn = "cedolinoId")
    val items: List<CedolinoItem>
)

data class UnitWithPayments(
    @Embedded val unit: CondoUnit,
    @Relation(parentColumn = "id", entityColumn = "unitId")
    val payments: List<Payment>
)

data class UnitWithCedolini(
    @Embedded val unit: CondoUnit,
    @Relation(parentColumn = "id", entityColumn = "unitId")
    val cedolini: List<Cedolino>
)

// ─── Costanti ────────────────────────────────────────────────────────
object ExpenseCategories {
    val categories = listOf(
        "Manutenzione Ordinaria" to "🔧", "Manutenzione Straordinaria" to "🏗️",
        "Pulizia" to "🧹", "Ascensore" to "🛗", "Illuminazione" to "💡",
        "Acqua" to "💧", "Riscaldamento" to "🔥", "Assicurazione" to "🛡️",
        "Amministrazione" to "📋", "Giardinaggio" to "🌿", "Altro" to "📦"
    )
    fun getIcon(category: String) = categories.find { it.first == category }?.second ?: "📦"
}

object PaymentMethods {
    val methods = listOf(
        "Contanti",
        "Bonifico",
        "Bollettino Postale",
        "RID / Addebito diretto",
        "Assegno"
    )
}
object CedolinoStatuses { val statuses = listOf("Emesso", "Pagato", "Scaduto", "Parziale") }
object UnitTypes { val types = listOf("Appartamento", "Locale", "Box", "Negozio", "Ufficio") }

object DocumentCategories {
    data class DocCategory(val name: String, val icon: String, val colorHex: String)
    val categories = listOf(
        DocCategory("Verbali di assemblea",  "📋", "#6C63FF"),
        DocCategory("Bilanci e preventivi",  "💰", "#00C896"),
        DocCategory("Ordini del giorno",     "📄", "#FF9F43"),
        DocCategory("Preventivi fornitori",  "🔧", "#54A0FF"),
        DocCategory("Contratti",             "📜", "#FF6B9D"),
        DocCategory("Pratiche condominiali", "🏛️", "#A29BFE"),
        DocCategory("Segnalazioni danni",    "⚠️", "#FF6B6B"),
        DocCategory("Avanzamento lavori",    "🏗️", "#F7B731"),
        DocCategory("Altro",                "📁", "#636E72")
    )
    val names = categories.map { it.name }
    fun getIcon(categoria: String) = categories.find { it.name == categoria }?.icon ?: "📁"
    fun getColorHex(categoria: String) = categories.find { it.name == categoria }?.colorHex ?: "#636E72"
}

object FileTypes {
    data class FileType(val name: String, val icon: String, val colorHex: String, val mimeTypes: List<String>)
    val supported = listOf(
        FileType("PDF",  "📄", "#FF6B6B", listOf("application/pdf")),
        FileType("Word", "📝", "#54A0FF", listOf(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )),
        FileType("Foto", "📷", "#00C896", listOf("image/jpeg", "image/png", "image/heic", "image/webp"))
    )
    val allMimeTypes: Array<String> get() = supported.flatMap { it.mimeTypes }.toTypedArray()
    fun getIcon(fileType: String) = supported.find { it.name == fileType }?.icon ?: "📎"
    fun getColorHex(fileType: String) = supported.find { it.name == fileType }?.colorHex ?: "#636E72"
    fun fromMimeType(mimeType: String): String = when {
        mimeType == "application/pdf" -> "PDF"
        mimeType.contains("word") || mimeType.contains("openxmlformats") -> "Word"
        mimeType.startsWith("image/") -> "Foto"
        else -> "Altro"
    }
}
