package com.renttrack.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val subtitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Login               : Screen("login",              "Accedi",           "",                     Icons.Filled.Lock,         Icons.Filled.Lock)
    data object Onboarding          : Screen("onboarding",          "Benvenuto",        "",                     Icons.Filled.Home,         Icons.Filled.Home)
    data object Annunci             : Screen("annunci",             "Annunci",          "Trova affitto",         Icons.Filled.Search,       Icons.Filled.Search)
    data object DettaglioAnnuncio   : Screen("dettaglio_annuncio",  "Annuncio",         "",                     Icons.Filled.Home,         Icons.Filled.Home)
    data object CreaAnnuncio        : Screen("crea_annuncio",       "Pubblica",         "",                     Icons.Filled.Add,          Icons.Filled.Add)
    data object MieiAnnunci         : Screen("miei_annunci",        "I miei annunci",   "",                     Icons.Filled.List,         Icons.Filled.List)
    data object Richieste           : Screen("richieste",            "Richieste",        "",                     Icons.Filled.MarkEmailUnread, Icons.Filled.MarkEmailUnread)
    data object CondominioSelector : Screen("condominio_selector", "Le mie proprietà", "Seleziona immobile",    Icons.Filled.Business,     Icons.Outlined.Business)
    data object Dashboard           : Screen("dashboard",           "Home",             "Panoramica affitti",    Icons.Filled.Home,         Icons.Outlined.Home)
    data object Tenants             : Screen("units",               "Inquilini",        "Gestione inquilini",    Icons.Filled.People,       Icons.Outlined.PeopleOutline)
    data object Affitti             : Screen("cedolini",            "Affitti",          "Avvisi e pagamenti",    Icons.Filled.EuroSymbol,   Icons.Outlined.EuroSymbol)
    data object Expenses            : Screen("expenses",            "Spese",            "Spese immobile",        Icons.Filled.Receipt,      Icons.Outlined.Receipt)
    data object Documenti           : Screen("documenti",           "Archivio",         "Documenti e contratti", Icons.Filled.Folder,       Icons.Outlined.FolderOpen)
    data object Reports             : Screen("reports",             "Report",           "Statistiche e report",  Icons.Filled.BarChart,     Icons.Outlined.BarChart)

    companion object {
        /** 5 tab nella bottom bar — Pagamenti rimosso, Affitti al suo posto */
        val bottomNavItems by lazy { listOf(Dashboard, Tenants, Affitti, Expenses, Documenti) }
        val allScreens     by lazy { listOf(Onboarding, CondominioSelector, Dashboard, Tenants, Affitti, Expenses, Documenti, Reports) }
    }
}
