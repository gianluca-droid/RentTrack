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
    data object CondominioSelector : Screen("condominio_selector", "Le mie proprietà", "Seleziona immobile", Icons.Filled.Business, Icons.Outlined.Business)
    data object Dashboard  : Screen("dashboard",  "Dashboard",  "Panoramica generale",      Icons.Filled.Dashboard,    Icons.Outlined.Dashboard)
    data object Tenants      : Screen("units",       "Inquilini",  "Gestione inquilini",       Icons.Filled.Apartment,    Icons.Outlined.Apartment)
    data object Expenses   : Screen("expenses",    "Spese",      "Spese immobile",           Icons.Filled.Receipt,      Icons.Outlined.Receipt)
    data object Payments   : Screen("payments",    "Incassi",   "Affitti incassati",        Icons.Filled.CreditCard,   Icons.Outlined.CreditCard)
    data object Affitti   : Screen("cedolini",    "Affitti",    "Avvisi affitto",           Icons.Filled.Description,  Icons.Outlined.Description)
    data object Documenti  : Screen("documenti",   "Documenti",  "Archivio documenti",       Icons.Filled.Folder,       Icons.Outlined.FolderOpen)
    data object Reports    : Screen("reports",     "Report",     "Statistiche e report",     Icons.Filled.BarChart,     Icons.Outlined.BarChart)
    // ─── Lato Condomino ───────────────────────────────────────────────
    data object ResidentLogin     : Screen("resident_login",     "Accesso Condomino",  "Seleziona il tuo appartamento", Icons.Filled.Person,  Icons.Outlined.Person)
    data object ResidentDashboard : Screen("resident_dashboard", "Area Personale",     "La tua area riservata",         Icons.Filled.Home,    Icons.Outlined.Home)

    companion object {
        val bottomNavItems    by lazy { listOf(Dashboard, Tenants, Expenses, Payments, Documenti) }
        val residentNavItems  by lazy { listOf(ResidentDashboard) }
        val allScreens        by lazy { listOf(CondominioSelector, Dashboard, Tenants, Expenses, Payments, Affitti, Documenti, Reports, ResidentLogin, ResidentDashboard) }
    }
}
