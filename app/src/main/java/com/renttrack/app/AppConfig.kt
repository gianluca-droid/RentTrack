package com.renttrack.app

/**
 * Configurazione centralizzata Supabase.
 *
 * NOTA: SUPABASE_ANON_KEY è un JWT con ruolo "anon" — by design pubblico nel client
 * (Supabase architecture). NON è un secret server-side. La sicurezza reale è garantita
 * dalle RLS policy a livello database.
 *
 * Il service_role key NON è mai presente nel codice Android.
 */
object AppConfig {
    const val SUPABASE_URL = "https://zjqrtuposdrimzjoydgh.supabase.co"
    const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
        "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpqcXJ0dXBvc2RyaW16am95ZGdoIiwi" +
        "cm9sZSI6ImFub24iLCJpYXQiOjE3NzgzMzE5MDMsImV4cCI6MjA5MzkwNzkwM30." +
        "dLvc0pPrfIXiBGJSDTnRtNU6FRppFPSr86pLwHD35j4"
}
