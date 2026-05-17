package com.renttrack.app.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import com.renttrack.app.AppConfig

/**
 * Provider del client Supabase per RentTrack.
 * Auth (gotrue) viene aggiunto nella fase successiva con la configurazione corretta.
 */
object AppSupabase {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = AppConfig.SUPABASE_URL,
            supabaseKey = AppConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            // Auth gestita via HTTP REST diretto in AuthViewModel (Supabase /auth/v1/).
            // Il plugin GoTrue del SDK non viene usato per evitare conflitti di versione.
        }
    }
}
