# CondoGest 🏢

App Android per la gestione di condomini — lato amministratore e lato condomino.

---

## ✅ Funzionalità implementate

### Lato Amministratore
- [x] Gestione multi-condominio (isolamento dati e UI per ogni edificio)
- [x] Gestione unità con raggruppamenti liberi (Scala, Corpo, Torre, Nord, Sud...)
- [x] Spese per categoria con grafici e percentuali
- [x] Pagamenti con filtro per scala/blocco e toggle vista "Per Unità / Per Mese"
- [x] Cedolini con generazione massiva proporzionale ai millesimi
- [x] Dashboard con riepilogo "Da Incassare" cliccabile → bottom sheet dettaglio morosi
- [x] Archivio documenti (PDF, Word, Foto) con:
  - Destinatari selezionabili: Tutto il condominio / Unità specifiche
  - Sintesi manuale visibile al condomino prima di aprire il file
  - Badge destinatari sulle card
- [x] Report mensile e archivio storico annuale
- [x] Bottoni con icona + etichetta leggibile (UX ottimizzata)

### Lato Condomino (mock, senza autenticazione reale)
- [x] Login mock: selezione condominio + scala + interno
- [x] Tab Appartamento: dati anagrafici + quota spese millesimale
- [x] Tab Cedolini: cedolini ricevuti con dettaglio voci e stati colorati
- [x] Tab Pagamenti: storico pagamenti effettuati
- [x] Tab Documenti: solo documenti destinati all'unità o a tutti + sintesi visibile

---

## 🗄️ Database

- **Room** locale, versione **6**
- Migrazione 5→6: aggiunta colonne `sommario`, `visibilita`, `destinatariUnitIds` a `documents`

---

## 🚀 Sviluppi futuri — Tecnici

### 1. 🤖 Sintesi automatica documenti con Gemini AI
**Obiettivo**: quando l'admin carica un PDF, l'app lo legge e genera automaticamente la sintesi, che l'admin può rivedere prima di salvare.

**Stack**:
- `com.tom_roush:pdfbox-android` → estrazione testo da PDF
- `com.google.ai.client.generativeai` → Gemini API per il riassunto

**Passi**:
1. Aggiungere le dipendenze al `build.gradle.kts`
2. Creare `GeminiRepository` con funzione `summarizeText(text: String): String`
3. Nel `CondoViewModel`, dopo la copia del file locale, estrarre il testo e chiamare Gemini
4. Pre-compilare il campo `sommario` nel form (modificabile dall'admin)
5. La chiave API va in `local.properties` → letta via `BuildConfig.GEMINI_API_KEY`

**Configurazione chiave API**:
```properties
# local.properties (NON committare su Git)
gemini.api.key=AIzaSyA...
```
```kotlin
// build.gradle.kts
buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties["gemini.api.key"]}\"")
```

**Nota futura (distribuzione)**: spostare la chiamata Gemini su **Supabase Edge Function** per non esporre la chiave nell'APK.

---

### 2. 🔐 Autenticazione reale lato condomino
**Obiettivo**: sostituire il mock login con autenticazione reale.

**Stack suggerito**: Supabase Auth (email/password o magic link)

**Passi**:
1. Creare progetto Supabase
2. Aggiungere `io.github.jan-tennert.supabase:auth-kt` 
3. Associare `auth.uid()` → `unitId` in una tabella `residents`
4. `ResidentLoginScreen` diventa un vero form email/password
5. `CondoViewModel` carica i dati filtrati per `unitId` dall'auth session

---

### 3. 📬 Notifiche push al condomino
**Obiettivo**: notificare il condomino quando l'admin pubblica un nuovo cedolino o documento.

**Stack**: Firebase Cloud Messaging (FCM) o Supabase Realtime

---

### 4. 📄 Export PDF cedolini
**Obiettivo**: generare un PDF del cedolino da inviare/stampare.

**Stack**: `com.itextpdf:itext7-core` o `android.graphics.pdf.PdfDocument` (built-in Android)

---

## 🚀 Sviluppi futuri — Business / Marketing

### 5. 📧 Agente n8n — Lead Generation
**Obiettivo**: trovare automaticamente studi amministrativi italiani e inviare email di presentazione di CondoGest.

**Stack** (tutto gratuito):
| Componente | Strumento | Costo |
|---|---|---|
| Automazione | n8n Cloud (free tier) | €0 |
| Ricerca studi | Apify / Pagine Gialle scraper | €0 |
| Database lead | Google Sheets | €0 |
| Email invio | Gmail SMTP (500/giorno) | €0 |
| Personalizzazione email | Gemini API | €0 |

**Flusso**:
```
Schedule (notte) → Scraping Pagine Gialle "amministratore condominio" + città
→ Filtra già-contattati (Google Sheet) → Gemini genera email personalizzata
→ Gmail invia → Sheet logga (nome, email, città, data, stato risposta)
```

**Note legali**: Le email di studi amministrativi su directory pubbliche (Pagine Gialle, sito web) sono contatti B2B. L'invio è lecito se: mittente identificato, oggetto chiaro, opt-out presente.

---

## 🏗️ Architettura attuale

```
CondoGest/
├── data/
│   ├── model/Entities.kt       # Room entities (v6)
│   ├── dao/Daos.kt             # Query Room
│   ├── database/AppDatabase.kt # DB + migrazioni
│   └── repository/CondoRepository.kt
├── viewmodel/CondoViewModel.kt  # State management
└── ui/
    ├── screens/                 # Una screen per sezione
    ├── components/Components.kt # Componenti riusabili
    ├── theme/                   # Colori dark theme
    └── navigation/Screen.kt    # Route definizioni
```

---

## 📌 Note rapide

- **Branch**: `main`
- **DB name**: `condogest_v3` (nome storico, non cambiarlo)
- **Min SDK**: 26 (Android 8.0)
- **Icone**: usare solo `Icons.Filled.*` del core — per icone extra aggiungere `material-icons-extended`
- **Scala**: campo libero, nessun prefisso automatico — mostrare as-is
- **Mock resident**: bottone "Area Condomino" nel `CondominioSelectorScreen`
