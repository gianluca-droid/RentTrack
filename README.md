# RentTrack 🏠

App Android per la gestione degli affitti — lato proprietario.

---

## 🐛 Bug noti da verificare (post-aggiornamento 15/05/2026)

> Questi problemi erano presenti prima dell'aggiornamento del 15/05/2026.
> Alcuni potrebbero essere già risolti — verificare su build installata.

| # | Bug | Descrizione | File coinvolto | Stato |
|---|---|---|---|---|
| B1 | **Modifiche avviso non salvate** | Modificando un avviso di pagamento (periodo, importo, scadenza) le modifiche non persistevano su Supabase dopo il salvataggio. Fix applicato: `EditCedolinoDialog` ora include il campo importo + `onSave` passa `total` aggiornato. | `RentNoticesScreen.kt` → `EditCedolinoDialog` | ⚠️ Da verificare su device |
| B2 | **Avviso eliminato ancora visibile nel Report** | Dopo aver eliminato un cedolino, il Report mostrava ancora i dati. Fix `refresh()` applicato. Causa residua: se il payment collegato NON aveva `cedolino_id` valorizzato (es. inserito manualmente), il DELETE `payments?cedolino_id=eq.$id` non lo raggiunge → il payment sopravvive nel DB. | `SupabaseRentRepository.kt` → `deleteCedolino()` riga 307 | ⚠️ Parzialmente fixato — vedere B7 |
| B3 | **Totali Dashboard non aggiornati** | La Dashboard legge da `_payments` e `_expenses` che vengono ricaricati da `refresh()`. Se il payment non viene eliminato dal DB (vedi B2/B7), `refresh()` lo rilegge e lo mostra ancora. Il problema è a monte (DB), non nel refresh. | `DashboardScreen.kt` riga 34-38, `SupabaseRentViewModel.kt` `refresh()` | ❓ Causa a monte (vedi B7) |
| B4 | **N+1 query cedolini** | Per ogni cedolino viene fatta una query separata a `cedolino_items`. Con molti avvisi l'app è lenta al caricamento della schermata Avvisi. | `SupabaseRentViewModel.kt` → `refresh()` | ❓ Performance da misurare |
| B5 | **Versione app statica** | La schermata Impostazioni mostra `1.0.0 (build 1)` hardcoded invece di leggere `BuildConfig.VERSION_NAME`. | `SettingsScreen.kt` riga 108 | 🟢 Fix banale (5 min) |
| B6 | **Pull-to-refresh assente** | Impossibile forzare refresh manuale dei dati senza uscire e rientrare nella schermata. | Tutte le schermate con `LazyColumn` | ❓ Da implementare |
| B7 | **Payment orfano dopo eliminazione storico/cedolino** | 🔴 **BUG CONFERMATO (15/05/2026)**. Eliminar uno storico inquilino (`deleteTenantHistory`) o un cedolino pagato NON elimina i `SPayment` associati: (A) `deleteTenantHistory` non ha logica di cascade sui payment; (B) `deleteCedolino` elimina solo `payments?cedolino_id=eq.$id` — se il payment era stato registrato manualmente (senza `cedolino_id`) sopravvive. Il payment orfano persiste nel DB e viene riletto da `refresh()` → appare in Home e Report. **Fix necessario**: in `deleteCedolino()` aggiungere seconda DELETE `payments?unit_id=eq.$unitId&cedolino_id=is.null&date>=...` oppure accettare il campo `condominioId` e fare DELETE per `condominio_id` + `cedolino_id`. | `SupabaseRentRepository.kt` riga 304-312 (`deleteCedolino`), riga 367-370 (`deleteTenantHistory`) | 🔴 **Da fixare — causa radice confermata** |

### Legenda
- ⚠️ **Da verificare su device** — fix nel codice, ma non confermato su build installata
- ❓ **Da indagare** — comportamento segnalato, causa non confermata
- 🟢 **Fix banale** — pochi minuti di lavoro, non ancora applicato
- ✅ **Risolto e verificato** — confermato funzionante su build installata

---

## Target
Piccoli proprietari privati con 2–5 appartamenti in affitto.

---

## ✅ Funzionalità implementate

### Gestione immobili
- [x] Multi-proprietà con isolamento dati per condominio
- [x] Gestione inquilini (ricerca, filtro stato contratto, storico subentranti)
- [x] Cambio inquilino con archiviazione storia locativa
- [x] Spese per categoria con grafico storico entrate/uscite (Canvas nativo)
- [x] Posizione contabile per inquilino (addebitato / incassato / saldo)

### Avvisi e pagamenti
- [x] Avvisi affitto con generazione automatica per tutti gli inquilini
- [x] Quota diretta per singolo inquilino
- [x] Registrazione pagamento con metodo (Contanti, Bonifico, Bollettino, RID, Assegno)
- [x] Duplica avviso per mese successivo
- [x] Condivisione avviso via WhatsApp / Email / PDF nativo

### Dashboard e notifiche
- [x] Dashboard con riepilogo Da incassare, banner morosità >30gg
- [x] Badge notifiche in bottom bar (avvisi in attesa, pagamenti pendenti)
- [x] Widget Home con scadenze imminenti
- [x] Smart Reminder (WorkManager) configurabile per pre-scadenze

### Documenti e report
- [x] Archivio documenti (PDF, Word, Foto) con destinatari selezionabili
- [x] Esportazione CSV avanzata (filtro anno / per proprietà)
- [x] Generazione PDF cedolino nativo (senza librerie esterne)

### Auth e UX
- [x] Login email/password + Google OAuth
- [x] Recupero password via deep link `renttrack://auth/callback`
- [x] Ricerca globale (inquilini, spese, cedolini, documenti)
- [x] Annunci vetrina con foto, candidature e gestione richieste

---

## 🏗️ Architettura

- **Kotlin** + **Jetpack Compose**
- **MVVM** con StateFlow e Repository pattern
- **Supabase** — auth + database cloud (REST API via HttpURLConnection, no SDK)
- **WorkManager** per notifiche background
- **Canvas nativo** per grafici e PDF

**Package**: `com.renttrack.app`  
**Supabase Project**: `zjqrtuposdrimzjoydgh.supabase.co`

---

## 🔐 Sicurezza — Stato attuale (audit 2026-05-13)

### Fix applicati

| Fix | Dove | Data |
|---|---|---|
| Bug prefs `RentCheckWorker` (`auth_prefs` → `renttrack_prefs`) | `RentCheckWorker.kt` | 2026-05-13 |
| `android:allowBackup="false"` | `AndroidManifest.xml` | 2026-05-13 |
| `owner_id` aggiunto all'INSERT `cedolino_items` | `SupabaseRentRepository.kt` | 2026-05-13 |
| `DEFAULT auth.uid()` su `cedolino_items.owner_id` | Supabase DB | 2026-05-13 |
| Policy INSERT `documenti` con `WITH CHECK (auth.uid() = owner_id)` | Supabase RLS | 2026-05-13 |
| GRANT espliciti su tutte le tabelle pubbliche | Supabase DB | 2026-05-13 |
| `GRANT USAGE ON SCHEMA public TO anon, authenticated` | Supabase DB | 2026-05-13 |

### RLS Policy — Stato tabelle

Tutte le tabelle critiche hanno policy `ALL` con `owner_id = auth.uid()` o equivalente.  
Audit completo: vedere `zero_trust_audit.md` nella cartella di sessione.

### ⚠️ Regola obbligatoria per nuove tabelle Supabase

Dal **30 maggio 2026**, ogni nuova tabella creata richiede GRANT espliciti.  
Dal **30 ottobre 2026**, vale per tutti i progetti esistenti.

**Ogni volta che crei una nuova tabella, esegui subito:**

```sql
-- Sostituisci 'nuova_tabella' con il nome reale
GRANT USAGE ON SCHEMA public TO anon, authenticated;  -- se non già eseguito

GRANT SELECT, INSERT, UPDATE, DELETE ON public.nuova_tabella TO authenticated;
-- (opzionale, solo se la tabella deve essere pubblica)
GRANT SELECT ON public.nuova_tabella TO anon;

ALTER TABLE public.nuova_tabella ENABLE ROW LEVEL SECURITY;

CREATE POLICY "owner_all_nuova_tabella" ON public.nuova_tabella
  FOR ALL TO authenticated
  USING (owner_id = auth.uid())
  WITH CHECK (owner_id = auth.uid());
```

---

## 🗺️ Roadmap

### Funzionalità
- [ ] Report PDF riepilogativo annuale/mensile (per commercialista)
- [ ] Vista calendario scadenze
- [ ] Dettaglio inquilino con storia locativa completa
- [ ] Affitto ricorrente mensile automatico
- [ ] Versione inglese (UK / Australia / USA)

### Produzione
- [ ] Configurazione SMTP reale per email transazionali
- [ ] Storage documenti → Signed URLs (privacy)
- [ ] Test end-to-end con dominio definitivo (`renttrack.it`)
- [ ] Google Play Store — Free + Pro €6,99/mese

---

## 🚀 Post-Produzione — Da fare prima del go-live

### 🔴 Sicurezza — Priorità alta

| # | Task | Dettaglio | Dove |
|---|---|---|---|
| 1 | **Storage documenti → Signed URLs** | Il bucket `documenti` è pubblico: chiunque con il link può scaricare contratti e ricevute. Passare a URL firmati con scadenza (es. 1h) tramite `supabase.storage.from("documenti").createSignedUrl(path, 3600)` | Supabase Storage + `SupabaseRentRepository.kt` |
| 2 | **EncryptedSharedPreferences per JWT** | I token auth sono salvati in SharedPreferences plain. Su device rooted sono leggibili. Migrare a `EncryptedSharedPreferences` (Jetpack Security) | `AuthViewModel.kt`, `ListingsViewModel.kt` |
| 3 | **Centralizzare `anonKey`** | La chiave Supabase è duplicata in 4 file (`SupabaseClient.kt`, `SupabaseRentRepository.kt`, `AuthViewModel.kt`, `ListingsViewModel.kt`). Spostarla in `BuildConfig` o `AppSupabase` unico punto | Refactoring cross-file |

### 🟡 Qualità — Priorità media

| # | Task | Dettaglio |
|---|---|---|
| 4 | **Ownership filter su PATCH/DELETE** | Le query UPDATE/DELETE non aggiungono `&owner_id=eq.$uid` (defense-in-depth). La RLS protegge lato DB, ma il filtro client è best practice | `SupabaseRentRepository.kt` |
| 5 | **SMTP reale notifiche email** | Attualmente le email transazionali usano il servizio demo Supabase (limite 3/ora). Configurare provider SMTP reale (SendGrid / Resend) | Supabase Dashboard → Auth → SMTP |
| 6 | **Aggiornare annunci con city/zona vuota** | Gli annunci creati prima dell'aggiunta del campo città obbligatorio potrebbero avere `city = NULL`. Aggiornare manualmente da Supabase Dashboard o aggiungere migration script | Supabase SQL Editor |
| 7 | **Pagina eliminazione account** | Google Play Data Safety richiede URL dedicato per cancellazione account. Creare pagina su Google Sites (o in-app) con istruzioni: inviare email a gianlucadelfini@yahoo.it con oggetto "Elimina account RentTrack". Attualmente usa URL privacy policy come placeholder. | Google Sites + Play Console → Sicurezza dei dati |

### 🟢 Miglioramenti — Bassa priorità / post-lancio

| # | Task | Dettaglio |
|---|---|---|
| 7 | **Rate limiting su inquiries** | Il form di richiesta annuncio usa `anonKey` senza limite. Considerare captcha o rate limit via Supabase Edge Function per evitare spam | Supabase Edge Functions |
| 8 | **Paginazione ricerca annunci** | La ricerca server-side restituisce tutti i matching results senza limite. Aggiungere `&limit=50` e paginazione per dataset grandi | `ListingsViewModel.searchListings()` |
| 9 | **Signed URLs listing-photos private** | Se in futuro si vuole mostrare foto solo agli utenti loggati (annunci premium), il bucket `listing-photos` dovrà diventare privato | Supabase Storage |
| 10 | **Test Worker notifiche** | Verificare che `RentCheckWorker` invii correttamente le notifiche pre-scadenza dopo il fix del namespace prefs (già fixato, da testare su device reale) | QA manuale |

---

> **Nota**: i fix di sicurezza #1-#3 sono **consigliati prima della pubblicazione su Google Play** se l'app gestisce dati di terzi (contratti, documenti personali degli inquilini).
