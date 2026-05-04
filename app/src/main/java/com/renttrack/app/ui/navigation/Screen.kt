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
    data object CondominioSelector : Screen("condominio_selector", "I tuoi Condomini", "Seleziona Proprietà", Icons.Filled.Business, Icons.Outlined.Business)
    data object Dashboard  : Screen("dashboard",  "Dashboard",  "Panoramica generale",      Icons.Filled.Dashboard,    Icons.Outlined.Dashboard)
    data object Tenants      : Screen("Tenants",       "UnitÃ ",      "Gestione condÃ²mini",       Icons.Filled.Apartment,    Icons.Outlined.Apartment)
    data object Expenses   : Screen("expenses",    "Spese",      "Registrazione spese",      Icons.Filled.Receipt,      Icons.Outlined.Receipt)
    data object Payments   : Screen("payments",    "Pagamenti",  "Gestione pagamenti",       Icons.Filled.CreditCard,   Icons.Outlined.CreditCard)
    data object Affitti   : Screen("Affitti",    "Affitti",   "Affitti di pagamento",    Icons.Filled.Description,  Icons.Outlined.Description)
    data object Documenti  : Screen("documenti",   "Documenti",  "Archivio documenti",       Icons.Filled.Folder,       Icons.Outlined.FolderOpen)
    data object Reports    : Screen("reports",     "Report",     "Statistiche e report",     Icons.Filled.BarChart,     Icons.Outlined.BarChart)
    // â”€â”€â”€ Lato Condomino â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    data object ResidentLogin     : Screen("resident_login",     "Accesso Condomino",  "Seleziona il tuo appartamento", Icons.Filled.Person,  Icons.Outlined.Person)
    data object ResidentDashboard : Screen("resident_dashboard", "Area Personale",     "La tua area riservata",         Icons.Filled.Home,    Icons.Outlined.Home)

    companion object {
        val bottomNavItems    by lazy { listOf(Dashboard, Tenants, Expenses, Payments, Documenti) }
        val residentNavItems  by lazy { listOf(ResidentDashboard) }
        val allScreens        by lazy { listOf(CondominioSelector, Dashboard, Tenants, Expenses, Payments, Affitti, Documenti, Reports, ResidentLogin, ResidentDashboard) }
    }
}



