# RentTrack 🏠

App Android per la gestione degli affitti — lato proprietario.

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
