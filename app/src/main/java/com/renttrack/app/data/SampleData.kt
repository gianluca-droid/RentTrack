package com.renttrack.app.data

import com.renttrack.app.data.model.*
import com.renttrack.app.data.repository.RentRepository
import java.util.Calendar

object SampleData {
    suspend fun populateDatabase(repository: RentRepository): Long {
        fun daysAgo(d: Int): Long { val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -d); return c.timeInMillis }

        // â”€â”€ Condominio 1 (dati completi) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        val condoId1 = repository.insertCondominio(
            Condominio(nome = "Condominio Via Roma 12", indirizzo = "Via Roma 12", citta = "Milano", cf = "97654321000")
        )

        val units1 = listOf(
            // Scala A
            CondoUnit(condominioId = condoId1, number = "1", floor = 0, type = "Locale",       areaMq = 45.0,  millesimi = 55.0,  ownerName = "Mario Bianchi",    ownerEmail = "mario.bianchi@email.it",  ownerPhone = "333 1001001", scala = "A"),
            CondoUnit(condominioId = condoId1, number = "2", floor = 1, type = "Appartamento", areaMq = 85.0,  millesimi = 125.0, ownerName = "Lucia Verdi",      ownerEmail = "lucia.verdi@email.it",    ownerPhone = "339 2002002", scala = "A"),
            CondoUnit(condominioId = condoId1, number = "3", floor = 2, type = "Appartamento", areaMq = 70.0,  millesimi = 105.0, ownerName = "Giuseppe Russo",   ownerEmail = "g.russo@email.it",        ownerPhone = "347 3003003", scala = "A"),
            CondoUnit(condominioId = condoId1, number = "4", floor = 3, type = "Appartamento", areaMq = 95.0,  millesimi = 140.0, ownerName = "Anna Ferrari",     ownerEmail = "anna.ferrari@email.it",   ownerPhone = "328 4004004", scala = "A"),
            // Scala B
            CondoUnit(condominioId = condoId1, number = "5", floor = 0, type = "Box",          areaMq = 18.0,  millesimi = 30.0,  ownerName = "Paolo Esposito",   ownerEmail = "p.esposito@email.it",     ownerPhone = "366 5005005", scala = "B"),
            CondoUnit(condominioId = condoId1, number = "6", floor = 1, type = "Appartamento", areaMq = 80.0,  millesimi = 120.0, ownerName = "Francesca Romano", ownerEmail = "f.romano@email.it",       ownerPhone = "345 6006006", scala = "B"),
            CondoUnit(condominioId = condoId1, number = "7", floor = 2, type = "Appartamento", areaMq = 110.0, millesimi = 160.0, ownerName = "Marco Colombo",    ownerEmail = "m.colombo@email.it",      ownerPhone = "320 7007007", scala = "B"),
            CondoUnit(condominioId = condoId1, number = "8", floor = 3, type = "Appartamento", areaMq = 75.0,  millesimi = 110.0, ownerName = "Sara Ricci",       ownerEmail = "sara.ricci@email.it",     ownerPhone = "351 8008008", scala = "B")
        )
        val unitIds1 = units1.map { repository.insertUnit(it) }

        listOf(
            Expense(condominioId = condoId1, date = daysAgo(150), category = "Pulizia",                    description = "Pulizia scale e androni - Gennaio", amount = 380.0,  notes = "Fatt. 012/2026"),
            Expense(condominioId = condoId1, date = daysAgo(120), category = "Manutenzione Ordinaria",     description = "Riparazione portone ingresso",      amount = 450.0,  notes = "Fatt. 045/2026"),
            Expense(condominioId = condoId1, date = daysAgo(110), category = "Acqua",                      description = "Bolletta acqua I trimestre",         amount = 1250.0),
            Expense(condominioId = condoId1, date = daysAgo(80),  category = "Ascensore",                  description = "Manutenzione ordinaria ascensore",   amount = 320.0),
            Expense(condominioId = condoId1, date = daysAgo(70),  category = "Assicurazione",              description = "Premio assicurazione annuale",        amount = 2800.0, notes = "Polizza n. 456789"),
            Expense(condominioId = condoId1, date = daysAgo(45),  category = "Riscaldamento",              description = "Manutenzione caldaia centralizzata", amount = 680.0),
            Expense(condominioId = condoId1, date = daysAgo(20),  category = "Amministrazione",            description = "Compenso amministratore II trimestre", amount = 1500.0),
            Expense(condominioId = condoId1, date = daysAgo(5),   category = "Manutenzione Straordinaria", description = "Rifacimento impermeabilizzazione terrazzo", amount = 4500.0, notes = "Delibera assemblea 15/04")
        ).forEach { repository.insertExpense(it) }

        listOf(
            Payment(unitId = unitIds1[0], amount = 350.0, date = daysAgo(100), method = "Bonifico", reference = "BON-2026-001"),
            Payment(unitId = unitIds1[1], amount = 520.0, date = daysAgo(95),  method = "Contanti",  reference = "CED-2026-012"),
            Payment(unitId = unitIds1[2], amount = 430.0, date = daysAgo(90),  method = "Bonifico", reference = "BON-2026-003"),
            Payment(unitId = unitIds1[3], amount = 580.0, date = daysAgo(85),  method = "Bonifico", reference = "BON-2026-004"),
            Payment(unitId = unitIds1[7], amount = 750.0, date = daysAgo(20),  method = "Contanti",  reference = "CED-2026-033")
        ).forEach { repository.insertPayment(it) }

        for (i in 0..3) {
            val items = listOf(
                CedolinoItem(cedolinoId = 0, description = "Pulizia scale",      amount = 380.0  * units1[i].millesimi / 1000),
                CedolinoItem(cedolinoId = 0, description = "Manutenzione ord.",  amount = 450.0  * units1[i].millesimi / 1000),
                CedolinoItem(cedolinoId = 0, description = "Acqua",              amount = 1250.0 * units1[i].millesimi / 1000),
                CedolinoItem(cedolinoId = 0, description = "Ascensore",          amount = 320.0  * units1[i].millesimi / 1000)
            )
            repository.insertCedolinoWithItems(
                Cedolino(unitId = unitIds1[i], period = "I Trimestre 2026",
                    issueDate = daysAgo(100), dueDate = daysAgo(70),
                    total = items.sumOf { it.amount },
                    status = if (i < 2) "Pagato" else "Emesso",
                    paidAmount = if (i < 2) items.sumOf { it.amount } else 0.0,
                    paidDate = if (i < 2) daysAgo(90) else null),
                items
            )
        }

        // â”€â”€ Condominio 2 (dati minimali) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        val condoId2 = repository.insertCondominio(
            Condominio(nome = "Condominio Viale Europa 5", indirizzo = "Viale Europa 5", citta = "Roma")
        )
        listOf(
            CondoUnit(condominioId = condoId2, number = "A", floor = 1, type = "Appartamento", areaMq = 90.0, millesimi = 300.0, ownerName = "Roberto Neri",    ownerEmail = "r.neri@email.it"),
            CondoUnit(condominioId = condoId2, number = "B", floor = 1, type = "Appartamento", areaMq = 75.0, millesimi = 250.0, ownerName = "Carla Blu",       ownerEmail = "c.blu@email.it"),
            CondoUnit(condominioId = condoId2, number = "C", floor = 2, type = "Appartamento", areaMq = 100.0,millesimi = 350.0, ownerName = "Gianni Gialli",   ownerEmail = "g.gialli@email.it"),
            CondoUnit(condominioId = condoId2, number = "1", floor = 0, type = "Box",           areaMq = 15.0, millesimi = 100.0, ownerName = "Roberto Neri")
        ).forEach { repository.insertUnit(it) }

        listOf(
            Expense(condominioId = condoId2, date = daysAgo(60), category = "Pulizia",       description = "Pulizia scale bimestrale", amount = 280.0),
            Expense(condominioId = condoId2, date = daysAgo(30), category = "Illuminazione", description = "Sostituzione lampade",      amount = 120.0)
        ).forEach { repository.insertExpense(it) }

        return condoId1
    }
}


