package com.renttrack.app.data

import com.renttrack.app.data.model.*
import com.renttrack.app.data.repository.RentRepository
import java.util.Calendar

object SampleData {
    suspend fun populateDatabase(repository: RentRepository): Long {
        fun daysAgo(d: Int): Long { val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -d); return c.timeInMillis }
        fun monthsAgo(m: Int): Long { val c = Calendar.getInstance(); c.add(Calendar.MONTH, -m); return c.timeInMillis }

        // ── Proprietà 1 — appartamento a Milano ─────────────────────
        val prop1 = repository.insertCondominio(
            Condominio(
                nome     = "Via Roma 12 - Milano",
                indirizzo = "Via Roma 12",
                citta    = "Milano",
                cf       = "",
                note     = "Appartamento 3 locali, 2° piano"
            )
        )

        // Ogni "CondoUnit" = un inquilino che affitta in quella proprietà
        val unit1a = repository.insertUnit(
            CondoUnit(condominioId = prop1, number = "Interno 1", floor = 2,
                type = "Appartamento", areaMq = 85.0, millesimi = 1000.0,
                ownerName = "Marco Rossi",
                ownerEmail = "marco.rossi@gmail.com",
                ownerPhone = "333 1234567", scala = "")
        )
        val unit1b = repository.insertUnit(
            CondoUnit(condominioId = prop1, number = "Interno 2", floor = 3,
                type = "Appartamento", areaMq = 65.0, millesimi = 1000.0,
                ownerName = "Giulia Bianchi",
                ownerEmail = "giulia.b@gmail.com",
                ownerPhone = "347 9876543", scala = "")
        )

        // Spese di manutenzione per la proprietà
        listOf(
            Expense(condominioId = prop1, date = daysAgo(90),
                category = "Manutenzione Ordinaria", description = "Riparazione infiltrazione bagno", amount = 320.0),
            Expense(condominioId = prop1, date = daysAgo(45),
                category = "Assicurazione", description = "Polizza annuale appartamento", amount = 450.0, notes = "Rinnovo 2026"),
            Expense(condominioId = prop1, date = daysAgo(10),
                category = "Amministrazione", description = "Quote condominiali 1° trimestre", amount = 280.0)
        ).forEach { repository.insertExpense(it) }

        // Avvisi affitto (cedolini = rate mensili)
        // Marco Rossi — affitto 850€/mese, paga regolarmente
        val months = listOf("Febbraio 2026", "Marzo 2026", "Aprile 2026")
        months.forEachIndexed { i, mese ->
            val paid = i < 2
            val issDate = monthsAgo(3 - i)
            val dueDate = monthsAgo(2 - i)
            val items = listOf(CedolinoItem(cedolinoId = 0, description = "Affitto mensile", amount = 850.0))
            repository.insertCedolinoWithItems(
                Cedolino(unitId = unit1a, period = mese,
                    issueDate = issDate, dueDate = dueDate,
                    total = 850.0, status = if (paid) "Pagato" else "Emesso",
                    paidAmount = if (paid) 850.0 else 0.0,
                    paidDate = if (paid) dueDate else null,
                    sentToResident = true, sentAt = issDate),
                items
            )
            if (paid) {
                repository.insertPayment(
                    Payment(unitId = unit1a, amount = 850.0,
                        date = dueDate, method = "Bonifico",
                        reference = "BON-${2026}-${String.format("%02d", 2 + i)}")
                )
            }
        }

        // Giulia Bianchi — affitto 700€/mese, ha un mese arretrato
        val monthsG = listOf("Marzo 2026", "Aprile 2026")
        monthsG.forEachIndexed { i, mese ->
            val paid = i == 0
            val issDate = monthsAgo(2 - i)
            val dueDate = monthsAgo(1 - i)
            val items = listOf(CedolinoItem(cedolinoId = 0, description = "Affitto mensile", amount = 700.0))
            repository.insertCedolinoWithItems(
                Cedolino(unitId = unit1b, period = mese,
                    issueDate = issDate, dueDate = dueDate,
                    total = 700.0, status = if (paid) "Pagato" else "Scaduto",
                    paidAmount = if (paid) 700.0 else 0.0,
                    paidDate = if (paid) dueDate else null,
                    sentToResident = true, sentAt = issDate),
                items
            )
            if (paid) {
                repository.insertPayment(
                    Payment(unitId = unit1b, amount = 700.0,
                        date = dueDate, method = "Contanti", reference = "")
                )
            }
        }

        // ── Proprietà 2 — monolocale a Torino ────────────────────────
        val prop2 = repository.insertCondominio(
            Condominio(
                nome      = "Viale Garibaldi 8 - Torino",
                indirizzo = "Viale Garibaldi 8",
                citta     = "Torino",
                note      = "Monolocale, 1° piano, arredato"
            )
        )
        val unit2a = repository.insertUnit(
            CondoUnit(condominioId = prop2, number = "Ap. 1", floor = 1,
                type = "Appartamento", areaMq = 38.0, millesimi = 1000.0,
                ownerName = "Luca Esposito",
                ownerEmail = "luca.esposito@outlook.com",
                ownerPhone = "339 5554433", scala = "")
        )

        listOf(
            Expense(condominioId = prop2, date = daysAgo(30),
                category = "Manutenzione Ordinaria", description = "Sostituzione serratura porta", amount = 180.0)
        ).forEach { repository.insertExpense(it) }

        // Luca Esposito — affitto 550€/mese, tutto regolare
        val itemsL = listOf(CedolinoItem(cedolinoId = 0, description = "Affitto mensile", amount = 550.0))
        repository.insertCedolinoWithItems(
            Cedolino(unitId = unit2a, period = "Aprile 2026",
                issueDate = monthsAgo(1), dueDate = monthsAgo(0),
                total = 550.0, status = "Pagato", paidAmount = 550.0,
                paidDate = daysAgo(5),
                sentToResident = true, sentAt = monthsAgo(1)),
            itemsL
        )
        repository.insertPayment(
            Payment(unitId = unit2a, amount = 550.0,
                date = daysAgo(5), method = "Bonifico", reference = "BON-2026-04-LE")
        )

        return prop1
    }
}
