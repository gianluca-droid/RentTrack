package com.renttrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.renttrack.app.ui.navigation.Screen
import com.renttrack.app.ui.screens.*
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.RentViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Leggi eventuale crash precedente
        val crashPrefs = getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
        val lastCrash = crashPrefs.getString("last_crash", null)
        crashPrefs.edit().remove("last_crash").apply()

        setContent {
            RentTrackTheme {
                if (lastCrash != null) {
                    CrashDialog(crashMessage = lastCrash)
                }
                MainApp()
            }
        }
    }
}

@Composable
fun CrashDialog(crashMessage: String) {
    var show by remember { mutableStateOf(true) }
    if (!show) return
    val scrollState = androidx.compose.foundation.rememberScrollState()
    AlertDialog(
        onDismissRequest = { show = false },
        containerColor = androidx.compose.ui.graphics.Color(0xFF1A1A2E),
        title = {
            Text(
                "\u274c Crash Rilevato",
                color = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                crashMessage,
                modifier = Modifier.verticalScroll(scrollState),
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        },
        confirmButton = {
            TextButton(onClick = { show = false }) {
                Text("OK", color = androidx.compose.ui.graphics.Color(0xFF00C896))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: RentViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isLoading by viewModel.isLoading.collectAsState()
    val activeCondominioId by viewModel.activeCondominioId.collectAsState()
    val activeCondominio by viewModel.activeCondominio.collectAsState()
    // Badge counts
    val unsentCedolini by viewModel.unsentCedoliniCount.collectAsState()
    val pendingCedolini by viewModel.pendingCedolini.collectAsState()

    // ── Loading screen ──────────────────────────────────────────────
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Cyan400)
                Spacer(Modifier.height(16.dp))
                Text("Caricamento...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    // ── Dopo il loading: scegli startDestination in base al condominio attivo ──
    val startDestination = if (activeCondominioId > 0L) Screen.Dashboard.route
                           else Screen.CondominioSelector.route

    val isInSelector  = currentRoute == Screen.CondominioSelector.route
    val isInResident  = currentRoute == Screen.ResidentLogin.route || currentRoute == Screen.ResidentDashboard.route
    val currentScreen = Screen.allScreens.find { it.route == currentRoute } ?: Screen.Dashboard

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            if (!isInSelector && !isInResident) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                currentScreen.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            // Nome condominio attivo
                            activeCondominio?.let {
                                Text(
                                    "🏢 ${it.nome}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Cyan400
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBg, titleContentColor = TextPrimary
                    ),
                    actions = {
                        // Cambia condominio
                        TextButton(
                            onClick = {
                                navController.navigate(Screen.CondominioSelector.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Business, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Proprietà", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                        // Reports
                        if (currentRoute != Screen.Reports.route) {
                            TextButton(
                                onClick = { navController.navigate(Screen.Reports.route) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.BarChart, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Report", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isInSelector && !isInResident) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        // Badge count per schermata
                        val badgeCount = when (screen.route) {
                            Screen.Affitti.route -> unsentCedolini
                            Screen.Payments.route -> pendingCedolini
                            else -> 0
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                BadgedBox(badge = {
                                    if (badgeCount > 0) Badge { Text("$badgeCount") }
                                }) {
                                    Icon(
                                        if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Cyan400,
                                selectedTextColor = Cyan400,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = Cyan400.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(Screen.CondominioSelector.route) {
                PropertySelectorScreen(
                    viewModel = viewModel,
                    onCondominioSelected = { condoId ->
                        viewModel.setActiveCondominio(condoId)
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.CondominioSelector.route) { inclusive = true }
                        }
                    },
                    onResidentAccess = {
                        navController.navigate(Screen.ResidentLogin.route)
                    }
                )
            }
            composable(Screen.Dashboard.route)  { DashboardScreen(viewModel) }
            composable(Screen.Tenants.route)      { TenantsScreen(viewModel) }
            composable(Screen.Expenses.route)   { ExpensesScreen(viewModel) }
            composable(Screen.Payments.route)   { PaymentsScreen(viewModel) }
            composable(Screen.Affitti.route)   { RentNoticesScreen(viewModel) }
            composable(Screen.Documenti.route)  { DocumentiScreen(viewModel) }
            composable(Screen.Reports.route)    { ReportsScreen(viewModel) }
            // ─── Lato Condomino ───────────────────────────────────────────────
            composable(Screen.ResidentLogin.route) {
                TenantLoginScreen(
                    viewModel = viewModel,
                    onLogin = {
                        navController.navigate(Screen.ResidentDashboard.route) {
                            popUpTo(Screen.ResidentLogin.route) { inclusive = true }
                        }
                    },
                    onBackToAdmin = {
                        navController.navigate(Screen.CondominioSelector.route) {
                            popUpTo(Screen.ResidentLogin.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.ResidentDashboard.route) {
                TenantDashboardScreen(
                    viewModel = viewModel,
                    onLogout = {
                        navController.navigate(Screen.ResidentLogin.route) {
                            popUpTo(Screen.ResidentDashboard.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
