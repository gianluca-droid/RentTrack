# ⚠️ PLAY STORE – TODO: AGGIORNAMENTI FUTURI

> Ultimo aggiornamento: 08/05/2026
> Queste impostazioni riflettono la versione ATTUALE dell'app.
> Aggiorna ogni voce quando la funzionalità corrispondente viene rilasciata.

---

## 📋 RIEPILOGO IMPOSTAZIONI ATTUALI → DA AGGIORNARE CON NUOVE FEATURE

| # | Sezione Play Console | Impostazione attuale | Aggiorna quando... |
|---|---|---|---|
| 1 | Contenuti app → **Annunci** | ❌ "No, nessun annuncio" | Integri **AdMob** |
| 2 | Sicurezza dati → **Account** | ❌ "Nessun account" | Integri **Supabase login** |
| 3 | Sicurezza dati → **Informazioni personali** | 0/9 selezionati | Sincronizzi dati inquilini su cloud |
| 4 | Sicurezza dati → **Informazioni finanziarie** | 0/4 selezionati | Sincronizzi pagamenti su cloud |
| 5 | Sicurezza dati → **Foto e video** | 0/2 selezionati | Aggiungi **foto immobili** |
| 6 | Sicurezza dati → **Posizione** | 0/2 selezionati | Aggiungi **mappe/GPS** |
| 7 | Sicurezza dati → **Messaggi** | 0/3 selezionati | Aggiungi **chat** inquilini |
| 8 | Contenuti app → **Classificazione** | PEGI 18 | Verifica se cambia con nuove feature |
| 9 | Monetizza → **Prezzo** | Gratuita | Aggiungi **abbonamento premium** con Supabase |
| 10 | Privacy Policy URL | Google Sites impostato | Aggiorna testo quando aggiungi Supabase/AdMob |

---

# ⚠️ PLAY STORE – AGGIORNARE QUANDO SI INTEGRA SUPABASE

## Sezione: Data Safety → Raccolta dati account

### Stato ATTUALE (versione senza Supabase)
✅ Risposta compilata: "La mia app non consente agli utenti di creare un account"

---

## 🔄 QUANDO AGGIUNGI SUPABASE → Aggiorna così:

### 1. Metodi di creazione account
Vai su: Play Console → App → Sicurezza dei dati → Pratiche di sicurezza

Seleziona:
- [x] Nome utente e password  (email + password Supabase)
- [x] OAuth                   (Google Sign-In via Supabase)

### 2. Nuovi dati da dichiarare nella sezione "Dati raccolti"
| Tipo di dato         | Categoria              | Condiviso con terzi |
|----------------------|------------------------|---------------------|
| Indirizzo email      | Informazioni personali | Supabase (backend)  |
| ID utente (UUID)     | Informazioni personali | Supabase (backend)  |
| Token autenticazione | Credenziali            | Supabase (backend)  |

### 3. Aggiorna anche la Privacy Policy
- Aggiungi sezione su autenticazione Supabase
- Aggiungi Supabase come Responsabile del Trattamento (art. 28 GDPR)
- Aggiorna trasferimenti dati extra-SEE (Supabase/AWS)

---
> Promemoria creato il: 08/05/2025
> File policy: brain\bc3c29c3-...\artifacts\renttrack_privacy_policy.md

---

## 📢 QUANDO AGGIUNGI PUBBLICITÀ (AdMob) → Aggiorna così:

### Play Console → Contenuti app → Annunci
- Cambia da: ☐ "No, la mia app non contiene annunci"
- Cambia a:  ✅ "Sì, la mia app contiene annunci"

### Sicurezza dei dati → aggiungi:
| Dato raccolto       | Categoria | Motivo              |
|---------------------|-----------|---------------------|
| ID dispositivo      | Identificatori | Targeting annunci |
| Dati utilizzo app   | Attività app   | AdMob analytics   |

### Aggiorna anche la Privacy Policy:
- Aggiungi sezione su Google AdMob
- Dichiara raccolta IDFA/GAID per pubblicità
