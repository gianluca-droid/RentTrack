package com.renttrack.app

import android.content.Context

object PropertyManager {
    private const val PREFS_NAME    = "condogest_prefs"
    private const val KEY_ACTIVE_CONDO = "active_condominio_id_str"
    private const val NO_CONDO      = ""

    fun getActiveCondominioId(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_CONDO, NO_CONDO) ?: NO_CONDO

    fun setActiveCondominioId(context: Context, id: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACTIVE_CONDO, id).apply()
    }

    fun hasActiveCondominio(context: Context): Boolean =
        getActiveCondominioId(context).isNotBlank()

    fun clearActiveCondominio(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_ACTIVE_CONDO).apply()
    }
}
