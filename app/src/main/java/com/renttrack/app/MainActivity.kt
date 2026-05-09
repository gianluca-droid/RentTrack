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
import com.renttrack.app.viewmodel.AuthViewModel
import com.renttrack.app.viewmodel.AuthViewModelFactory
import com.renttrack.app.viewmodel.AuthState
import com.renttrack.app.viewmodel.ListingsViewModel
import com.renttrack.app.viewmodel.ListingsViewModelFactory
import com.renttrack.app.viewmodel.RentViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val crashPrefs = getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
        val lastCrash = crashPrefs.getString("last_crash", null)
        crashPrefs.edit().remove("last_crash").apply()

        setContent {
            RentTrackTheme {
                if (lastCrash != null) CrashDialog(crashMessage = lastCrash)
                MainApp()
            }
        }
    }
}

@Composable
fun CrashDialog(crashMessage: String) {
    var show by remember { mutableStateOf(true) }
    if (!show) return
    AlertDialog(
        onDismissRequest = { show = false },
        containerColor = androidx.compose.ui.graphics.Color(0xFF1A1A2E),
        title = {
            Text("❌ Crash Rilevato",
                color = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
                fontWeight = FontWeight.Bold)
        },
        text = {
            Text(crashMessage,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
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
    val context             = androidx.compose.ui.platform.LocalContext.current
    val navController       = rememberNavController()
    val navBackStackEntry  by navController.currentBackStackEntryAsState()
    val currentRoute        = navBackStackEntry?.destination?.route
    val isLoading          by viewModel.isLoading.collectAsState()
    val activeCondominioId by viewModel.activeCondominioId.collectAsState()
    val activeCondominio   by viewModel.activeCondominio.collectAsState()
    val pendingCedolini    by viewModel.pendingCedolini.collectAsState()
    var showSwitchPropertyDialog by remember { mutableStateOf(false) }

    // ── Onboarding check ─────────────────────────────────────────────────
    val onboardingShown = remember {
        context.getSharedPreferences("renttrack_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("onboarding_shown", false)
    }

    // ── Loading ──────────────────────────────────────────────────────────
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Cyan400)
                Spacer(Modifier.height(16.dp))
                Text("Caricamento…", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(context))
    val listingsViewModel: ListingsViewModel = viewModel(factory = ListingsViewModelFactory(context))
    val authState by authViewModel.authState.collectAsState()
    val isLoggedIn = authState is AuthState.LoggedIn

    val startDestination = when {
        !onboardingShown            -> Screen.Onboarding.route
        isLoggedIn && activeCondominioId > 0L -> Screen.Dashboard.route
        isLoggedIn                  -> Screen.CondominioSelector.route
        else                        -> Screen.Annunci.route
    }

    val isInSelector   = currentRoute == Screen.CondominioSelector.route
    val isInOnboarding = currentRoute == Screen.Onboarding.route
    val isInLogin      = currentRoute == Screen.Login.route
    val isInAnnunci    = currentRoute == Screen.Annunci.route
    val isInDettaglio  = currentRoute == Screen.DettaglioAnnuncio.route
    val isInCrea       = currentRoute == Screen.CreaAnnuncio.route
    val isInMiei       = currentRoute == Screen.MieiAnnunci.route
    val hideChrome     = isInSelector || isInOnboarding || isInLogin ||
                         isInAnnunci || isInDettaglio || isInCrea || isInMiei

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            if (!hideChrome) {
                val currentScreen = Screen.allScreens.find { it.route == currentRoute } ?: Screen.Dashboard
                TopAppBar(
                    title = {
                        Column {
                            Text(currentScreen.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            activeCondominio?.let {
                                Text("🏠 ${it.nome}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Cyan400)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBg, titleContentColor = TextPrimary
                    ),
                    actions = {
                        // Guida — riapre l'onboarding
                        IconButton(
                            onClick = {
                                navController.navigate(Screen.Onboarding.route) {
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.HelpOutline,
                                contentDescription = "Come funziona",
                                tint = TextMuted
                            )
                        }
                        // Cambia proprietà
                        TextButton(
                            onClick = { showSwitchPropertyDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Business, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Proprietà", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                        // Annunci
                        TextButton(
                            onClick = {
                                navController.navigate(Screen.Annunci.route) {
                                    launchSingleTop = true
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Apartment, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Annunci", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                        // Report
                        if (currentRoute != Screen.Reports.route) {
                            TextButton(
                                onClick = {
                                    navController.navigate(Screen.Reports.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.BarChart, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Report", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        // Logout
                        var showLogoutDialog by remember { mutableStateOf(false) }
                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = TextMuted)
                        }
                        if (showLogoutDialog) {
                            AlertDialog(
                                onDismissRequest = { showLogoutDialog = false },
                                containerColor = DarkSurface,
                                icon = { Icon(Icons.Filled.Logout, null, tint = Red400) },
                                title = { Text("Esci dall'account", color = TextPrimary, fontWeight = FontWeight.Bold) },
                                text = { Text("Vuoi davvero uscire?", color = TextSecondary) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showLogoutDialog = false
                                        authViewModel.signOut()
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }) { Text("Esci", color = Red400, fontWeight = FontWeight.Bold) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showLogoutDialog = false }) {
                                        Text("Annulla", color = TextSecondary)
                                    }
                                }
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!hideChrome) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected   = currentRoute == screen.route
                        val badgeCount = if (screen.route == Screen.Affitti.route) pendingCedolini else 0
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                // Guard: non navigare se già su quel tab o su Report
                                // (Report e i tab sono allo stesso livello dello stack dopo il fix)
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
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
                                selectedIconColor   = Cyan400,
                                selectedTextColor   = Cyan400,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor      = Cyan400.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
    // ── Dialog conferma cambio proprietà ────────────────────────────────────
    if (showSwitchPropertyDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchPropertyDialog = false },
            containerColor = androidx.compose.ui.graphics.Color(0xFF1E1E2E),
            icon = { Icon(Icons.Filled.Business, null, tint = Cyan400) },
            title = {
                Text(
                    "Cambia proprietà",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Vuoi tornare alla schermata di selezione delle proprietà?\n\n" +
                    "La proprietà attuale è: ${activeCondominio?.nome ?: "—"}",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSwitchPropertyDialog = false
                        navController.navigate(Screen.CondominioSelector.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan400,  contentColor = androidx.compose.ui.graphics.Color.Black)
                ) { Text("Cambia", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchPropertyDialog = false }) {
                    Text("Rimani qui", color = TextSecondary)
                }
            }
        )
    }

    NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(paddingValues),
            enterTransition  = { fadeIn() },
            exitTransition   = { fadeOut() }
        ) {
            composable(Screen.Annunci.route) {
                AnnunciScreen(
                    viewModel = listingsViewModel,
                    isLoggedIn = isLoggedIn,
                    onListingClick = { listing ->
                        listingsViewModel.loadPublicListings()
                        navController.currentBackStackEntry
                            ?.savedStateHandle?.set("listing", listing)
                        navController.navigate(Screen.DettaglioAnnuncio.route)
                    },
                    onLoginClick = {
                        navController.navigate(Screen.Login.route)
                    },
                    onCreaAnnuncio = {
                        navController.navigate(Screen.CreaAnnuncio.route)
                    }
                )
            }
            composable(Screen.DettaglioAnnuncio.route) { backStack ->
                val listing = navController.previousBackStackEntry
                    ?.savedStateHandle?.get<com.renttrack.app.data.model.Listing>("listing")
                if (listing != null) {
                    DettaglioAnnuncioScreen(
                        listing = listing,
                        viewModel = listingsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.CreaAnnuncio.route) {
                CreaAnnuncioScreen(
                    viewModel = listingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.MieiAnnunci.route) {
                MieiAnnunciScreen(
                    viewModel = listingsViewModel,
                    onCreaAnnuncio = { navController.navigate(Screen.CreaAnnuncio.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        val dest = if (activeCondominioId > 0L) Screen.Dashboard.route
                                   else Screen.CondominioSelector.route
                        navController.navigate(dest) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.CondominioSelector.route) {
                PropertySelectorScreen(
                    viewModel = viewModel,
                    onCondominioSelected = { condoId ->
                        viewModel.setActiveCondominio(condoId)
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.CondominioSelector.route) { inclusive = true }
                        }
                    },
                    onShowOnboarding = {
                        navController.navigate(Screen.Onboarding.route) {
                            launchSingleTop = true
                        }
                    },
                    onLogout = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Dashboard.route)  {
                DashboardScreen(
                    viewModel = viewModel,
                    listingsViewModel = listingsViewModel,
                    onNavigateToAnnunci = {
                        navController.navigate(Screen.MieiAnnunci.route) { launchSingleTop = true }
                    },
                    onCreaAnnuncio = {
                        navController.navigate(Screen.CreaAnnuncio.route) { launchSingleTop = true }
                    }
                )
            }
            composable(Screen.Tenants.route)    { TenantsScreen(viewModel) }
            composable(Screen.Affitti.route)    { RentNoticesScreen(viewModel) }
            composable(Screen.Expenses.route)   { ExpensesScreen(viewModel) }
            composable(Screen.Documenti.route)  { DocumentiScreen(viewModel) }
            composable(Screen.Reports.route)    { ReportsScreen(viewModel) }
        }
    }
}
