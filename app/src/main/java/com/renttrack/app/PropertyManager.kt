package com.renttrack.app

import android.content.Context

object PropertyManager {
    private const val PREFS_NAME = "condogest_prefs"
    private const val KEY_ACTIVE_CONDO = "active_condominio_id"
    private const val NO_CONDO = -1L

    fun getActiveCondominioId(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_ACTIVE_CONDO, NO_CONDO)

    fun setActiveCondominioId(context: Context, id: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_ACTIVE_CONDO, id).apply()
    }

    fun hasActiveCondominio(context: Context): Boolean =
        getActiveCondominioId(context) != NO_CONDO

    fun clearActiveCondominio(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_ACTIVE_CONDO).apply()
    }
}


