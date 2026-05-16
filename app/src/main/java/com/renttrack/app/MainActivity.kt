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
import com.renttrack.app.ui.components.SubscriptionGate
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.AuthViewModel
import com.renttrack.app.viewmodel.AuthViewModelFactory
import com.renttrack.app.viewmodel.AuthState
import com.renttrack.app.viewmodel.ListingsViewModel
import com.renttrack.app.viewmodel.ListingsViewModelFactory
import com.renttrack.app.viewmodel.SubscriptionViewModel
import com.renttrack.app.viewmodel.SupabaseRentViewModel

class MainActivity : ComponentActivity() {

    // URI del deep link: aggiornato sia da onCreate (app fredda) che da onNewIntent (app calda)
    private val _deepLinkUri = kotlinx.coroutines.flow.MutableStateFlow<android.net.Uri?>(null)
    val deepLinkUri: kotlinx.coroutines.flow.StateFlow<android.net.Uri?> = _deepLinkUri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Deep link ricevuto a freddo (app era chiusa)
        intent?.data?.let { uri ->
            if (uri.scheme == "renttrack") _deepLinkUri.value = uri
        }

        val crashPrefs = getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
        val lastCrash = crashPrefs.getString("last_crash", null)
        crashPrefs.edit().remove("last_crash").apply()

        setContent {
            RentTrackTheme {
                if (lastCrash != null) CrashDialog(crashMessage = lastCrash)
                MainApp(activity = this)
            }
        }
    }

    // Deep link ricevuto a caldo (app era già in foreground/background)
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { uri ->
            if (uri.scheme == "renttrack") _deepLinkUri.value = uri
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
fun MainApp(
    viewModel: SupabaseRentViewModel = viewModel(),
    activity: MainActivity? = null
) {
    val context             = androidx.compose.ui.platform.LocalContext.current
    val navController       = rememberNavController()
    val navBackStackEntry  by navController.currentBackStackEntryAsState()
    val currentRoute        = navBackStackEntry?.destination?.route

    // ── AuthViewModel viene creato PRIMA di tutto ─────────────────────────────
    // (non dopo il guard isLoading come prima, che causava race condition)
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(context))
    val listingsViewModel: ListingsViewModel = viewModel(factory = ListingsViewModelFactory(context))
    val subscriptionViewModel: SubscriptionViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val isLoggedIn = authState is AuthState.LoggedIn

    // ── Osserva deep link URI dall'Activity ─────────────────────────────────
    val deepLinkUri by (activity?.deepLinkUri ?: kotlinx.coroutines.flow.MutableStateFlow(null))
        .collectAsState()

    LaunchedEffect(deepLinkUri) {
        val uri = deepLinkUri ?: return@LaunchedEffect
        authViewModel.handleDeepLink(uri)
    }

    // Naviga in base allo stato auth dopo deep link + trigger refresh dati
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.RecoveryReady -> {
                navController.navigate(Screen.ResetPassword.route) {
                    popUpTo(Screen.Login.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
            is AuthState.LoggedIn -> {
                // Trigger refresh dati (ex race condition fix)
                viewModel.refresh()
                // Se siamo su ResetPassword, naviga alla home
                if (currentRoute == Screen.ResetPassword.route) {
                    val dest = if (viewModel.activeCondominioId.value.isNotBlank())
                        Screen.Dashboard.route else Screen.CondominioSelector.route
                    navController.navigate(dest) { popUpTo(0) { inclusive = true } }
                }
            }
            else -> Unit
        }
    }

    val isLoading          by viewModel.isLoading.collectAsState()
    val initialLoadDone    by viewModel.initialLoadDone.collectAsState()
    val activeCondominioId by viewModel.activeCondominioId.collectAsState()
    val activeCondominio   by viewModel.activeCondominio.collectAsState()
    val pendingCedolini    by viewModel.pendingCedolini.collectAsState()
    var showSwitchPropertyDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showLogoutDialog  by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Mostra errori ViewModel via Snackbar ──────────────────────
    val vmError by viewModel.error.collectAsState()
    LaunchedEffect(vmError) {
        val msg = vmError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = "❌ $msg",
            duration = SnackbarDuration.Long
        )
        viewModel.clearError()
    }

    // ── Onboarding check ─────────────────────────────────────────────────
    val onboardingShown = remember {
        context.getSharedPreferences("renttrack_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("onboarding_shown", false)
    }

    // Spinner: copre auth, caricamento dati E il gap tra LoggedIn e primo refresh()
    // isLoggedIn && !initialLoadDone evita il flash di CondominioSelector
    if (authState is AuthState.Loading || isLoading || (isLoggedIn && !initialLoadDone)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Cyan400)
                Spacer(Modifier.height(16.dp))
                Text("Caricamento…", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    val startDestination = when {
        !onboardingShown                         -> Screen.Onboarding.route
        isLoggedIn && activeCondominioId.isNotBlank() -> Screen.Dashboard.route
        isLoggedIn                               -> Screen.CondominioSelector.route
        else                                     -> Screen.Annunci.route
    }

    val isInSelector   = currentRoute == Screen.CondominioSelector.route
    val isInOnboarding = currentRoute == Screen.Onboarding.route
    val isInLogin      = currentRoute == Screen.Login.route
    val isInAnnunci    = currentRoute == Screen.Annunci.route
    val isInDettaglio  = currentRoute == Screen.DettaglioAnnuncio.route
    val isInCrea       = currentRoute == Screen.CreaAnnuncio.route
    val isInMiei       = currentRoute == Screen.MieiAnnunci.route
    val isInReset      = currentRoute == Screen.ResetPassword.route
    val hideChrome     = isInSelector || isInOnboarding || isInLogin ||
                         isInAnnunci || isInDettaglio || isInCrea || isInMiei || isInReset

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            if (!hideChrome) {
                val currentScreen = Screen.allScreens.find { it.route == currentRoute } ?: Screen.Dashboard

                TopAppBar(
                    title = {
                        Column(modifier = Modifier.padding(end = 4.dp)) {
                            // Titolo principale = sezione corrente (es. "Home", "Inquilini")
                            if (activeCondominio != null) {
                                Text(
                                    currentScreen.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (activeCondominio!!.indirizzo.isNotBlank())
                                        "${activeCondominio!!.indirizzo}" +
                                        if (activeCondominio!!.citta.isNotBlank()) ", ${activeCondominio!!.citta}" else ""
                                    else
                                        activeCondominio!!.nome,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    currentScreen.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBg, titleContentColor = TextPrimary
                    ),
                    actions = {
                        // Cerca — icona ricerca globale
                        if (currentRoute != Screen.Search.route) {
                            IconButton(onClick = {
                                navController.navigate(Screen.Search.route) { launchSingleTop = true }
                            }) {
                                Icon(Icons.Filled.Search, "Cerca", tint = TextMuted)
                            }
                        }

                        // Report — icona compatta, visibile solo quando NON si è già su Report
                        if (currentRoute != Screen.Reports.route) {
                            IconButton(onClick = {
                                navController.navigate(Screen.Reports.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            }) {
                                Icon(Icons.Filled.BarChart, "Report", tint = TextMuted)
                            }
                        }

                        // Menu overflow (3 puntini): Proprietà, Guida, Logout
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Filled.MoreVert, "Menu", tint = TextMuted)
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                            // Vedi vetrina pubblica
                                DropdownMenuItem(
                                    text = { Text("Vedi vetrina pubblica", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Filled.Visibility, null, tint = Cyan400) },
                                    onClick = {
                                        showOverflowMenu = false
                                        listingsViewModel.loadPublicListings()
                                        navController.navigate(Screen.Annunci.route) { launchSingleTop = true }
                                    }
                                )
                            // Cambia proprietà
                                DropdownMenuItem(
                                    text = { Text("Cambia proprietà", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Filled.Business, null, tint = Cyan400) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showSwitchPropertyDialog = true
                                    }
                                )
                                // Impostazioni
                                DropdownMenuItem(
                                    text = { Text("Impostazioni", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Filled.Settings, null, tint = TextSecondary) },
                                    onClick = {
                                        showOverflowMenu = false
                                        navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                                    }
                                )
                                // Guida
                                DropdownMenuItem(
                                    text = { Text("Come funziona", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Filled.HelpOutline, null, tint = TextSecondary) },
                                    onClick = {
                                        showOverflowMenu = false
                                        navController.navigate(Screen.Onboarding.route) { launchSingleTop = true }
                                    }
                                )
                                HorizontalDivider(color = TextMuted.copy(alpha = 0.15f))
                                // Logout
                                DropdownMenuItem(
                                    text = { Text("Esci", color = Red400) },
                                    leadingIcon = { Icon(Icons.Filled.Logout, null, tint = Red400) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showLogoutDialog = true
                                    }
                                )
                            }
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
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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

    // ── Dialog conferma logout ───────────────────────────────────────────────
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
                    subscriptionViewModel = subscriptionViewModel,
                    onCreaAnnuncio = { navController.navigate(Screen.CreaAnnuncio.route) },
                    onRichieste = { navController.navigate(Screen.Richieste.route) { launchSingleTop = true } },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Richieste.route) {
                RichiesteScreen(
                    viewModel = listingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        val dest = if (activeCondominioId.isNotBlank()) Screen.Dashboard.route
                                   else Screen.CondominioSelector.route
                        navController.navigate(dest) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onShowHelp = {
                        navController.navigate(Screen.Onboarding.route) { launchSingleTop = true }
                    }
                )
            }
            composable(Screen.Onboarding.route) {
                // Se siamo qui come primo avvio (nessuna schermata precedente),
                // onFinished porta al login. Se invece è stata aperta dalla guida
                // (utente già loggato), popBackStack() torna alla schermata precedente.
                val isFirstLaunch = !isLoggedIn
                OnboardingScreen(
                    onFinished = {
                        if (isFirstLaunch) {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }
            composable(Screen.CondominioSelector.route) {
                PropertySelectorScreen(
                    viewModel = viewModel,
                    subscriptionViewModel = subscriptionViewModel,
                    onCondominioSelected = { condoId ->
                        viewModel.setActiveCondominio(condoId)
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.CondominioSelector.route) { inclusive = true }
                        }
                    },
                    onShowOnboarding = {
                        navController.navigate(Screen.Onboarding.route) { launchSingleTop = true }
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
                        // Card "I miei annunci" → gestione annunci del proprietario
                        navController.navigate(Screen.MieiAnnunci.route) { launchSingleTop = true }
                    },
                    onVediVetrina = {
                        // Refresh feed prima di navigare → dati sempre aggiornati
                        listingsViewModel.loadPublicListings()
                        navController.navigate(Screen.Annunci.route) { launchSingleTop = true }
                    },
                    onCreaAnnuncio = {
                        navController.navigate(Screen.CreaAnnuncio.route) { launchSingleTop = true }
                    }
                )
            }
            composable(Screen.Tenants.route)    {
                TenantsScreen(viewModel = viewModel)
            }
            composable(Screen.Affitti.route)    { RentNoticesScreen(viewModel) }
            composable(Screen.Expenses.route)   { ExpensesScreen(viewModel) }
            composable(Screen.Documenti.route) {
                SubscriptionGate(
                    subscriptionViewModel = subscriptionViewModel,
                    onDismiss = { navController.popBackStack() }
                ) { DocumentiScreen(viewModel) }
            }
            composable(Screen.Reports.route) {
                SubscriptionGate(
                    subscriptionViewModel = subscriptionViewModel,
                    onDismiss = { navController.popBackStack() }
                ) { ReportsScreen(viewModel) }
            }
            composable(Screen.ResetPassword.route) {
                ResetPasswordScreen(
                    viewModel = authViewModel,
                    onSuccess = {
                        val dest = if (viewModel.activeCondominioId.value.isNotBlank())
                            Screen.Dashboard.route else Screen.CondominioSelector.route
                        navController.navigate(dest) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = viewModel,
                    onBack    = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
