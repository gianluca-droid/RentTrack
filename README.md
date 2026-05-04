# RentTrack 🏠

App Android per la gestione degli affitti — lato proprietario e lato inquilino.

## Target
Piccoli proprietari privati con 2-5 appartamenti in affitto.

## ✅ Funzionalità implementate

### Lato Proprietario (Admin)
- [x] Gestione multi-proprietà (isolamento dati per ogni immobile)
- [x] Gestione inquilini con scala/blocco e ricerca testuale
- [x] Spese manutenzione per categoria con grafici
- [x] Pagamenti con filtro metodo e vista "Per Inquilino / Per Mese"
- [x] Avvisi affitto con generazione automatica e quota diretta per inquilino
- [x] Dashboard con riepilogo "Da incassare" cliccabile
- [x] Archivio documenti (PDF, Word, Foto) con destinatari selezionabili
- [x] Badge notifiche in bottom bar (avvisi in attesa, pagamenti pendenti)
- [x] Posizione contabile per inquilino (addebitato / incassato / saldo)
- [x] Registrazione pagamento con scelta metodo (Contanti, Bonifico, Bollettino Postale, RID, Assegno)
- [x] Condivisione avviso affitto via WhatsApp / email
- [x] Duplica avviso per mese successivo

### Lato Inquilino
- [x] Login con selezione proprietà e unità
- [x] Dashboard personale: saldo, cedolini, pagamenti, documenti
- [x] Visualizzazione avvisi affitto inviati
- [x] Storico pagamenti ricevuti con metodo e riferimento

## 🏗️ Architettura
- **Kotlin** + **Jetpack Compose**
- **Room** / SQLite (locale, MVP)
- **MVVM** con StateFlow e Repository pattern
- **Supabase** (pianificato — Fase 2)

## 📦 Package
`com.renttrack.app`

## 🗺️ Roadmap
- [ ] Backend Supabase (auth + sync cloud)
- [ ] Scadenza contratto con notifica automatica
- [ ] Affitto ricorrente mensile automatico
- [ ] Versione inglese (UK / Australia / USA)
- [ ] Google Play Store — Free + Pro €6,99/mese
