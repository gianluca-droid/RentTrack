package com.renttrack.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.AuthState
import com.renttrack.app.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isRegister   by remember { mutableStateOf(false) }

    val focusManager      = LocalFocusManager.current
    val passwordFocusReq  = remember { FocusRequester() }

    val isLoading = authState is AuthState.Loading
    val errorMsg  = (authState as? AuthState.Error)?.message

    // Naviga all'app quando il login va a buon fine
    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedIn) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050810),
                        DarkBg,
                        Color(0xFF0D1B2A)
                    )
                )
            )
    ) {
        // ── Decorazione sfondo ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Cyan400.copy(alpha = 0.07f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Purple500.copy(alpha = 0.07f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        // ── Contenuto principale ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // Logo / Icona
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Cyan400, Purple500)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Titolo
            Text(
                text = "RentTrack",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                ),
                color = TextPrimary
            )
            Text(
                text = "Il tuo gestionale immobiliare",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(48.dp))

            // ── Card form ─────────────────────────────────────────────────
            Surface(
                modifier  = Modifier.fillMaxWidth(),
                color     = DarkSurface,
                shape     = RoundedCornerShape(24.dp),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Titolo card
                    Text(
                        text = if (isRegister) "Crea account" else "Accedi",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (isRegister)
                            "Inserisci email e password per registrarti"
                        else
                            "Bentornato! Inserisci le tue credenziali",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(Modifier.height(4.dp))

                    // Campo Email
                    OutlinedTextField(
                        value         = email,
                        onValueChange = { email = it },
                        label         = { Text("Email") },
                        leadingIcon   = {
                            Icon(Icons.Filled.Email, null, tint = TextMuted)
                        },
                        singleLine    = true,
                        enabled       = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFocusReq.requestFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = loginFieldColors()
                    )

                    // Campo Password
                    OutlinedTextField(
                        value         = password,
                        onValueChange = { password = it },
                        label         = { Text("Password") },
                        leadingIcon   = {
                            Icon(Icons.Filled.Lock, null, tint = TextMuted)
                        },
                        trailingIcon  = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    contentDescription = "Mostra password",
                                    tint = TextMuted
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        singleLine    = true,
                        enabled       = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (email.isNotBlank() && password.isNotBlank()) {
                                    if (isRegister) viewModel.signUp(email.trim(), password)
                                    else viewModel.signIn(email.trim(), password)
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusReq),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = loginFieldColors()
                    )

                    // Messaggio errore
                    AnimatedVisibility(
                        visible = errorMsg != null,
                        enter   = fadeIn() + expandVertically(),
                        exit    = fadeOut() + shrinkVertically()
                    ) {
                        if (errorMsg != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Red400.copy(alpha = 0.12f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Warning, null,
                                    tint = Red400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text  = errorMsg,
                                    color = Red400,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Pulsante principale
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (isRegister) viewModel.signUp(email.trim(), password)
                            else            viewModel.signIn(email.trim(), password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Cyan400,
                            contentColor   = Color.Black,
                            disabledContainerColor = Cyan400.copy(alpha = 0.3f),
                            disabledContentColor   = Color.Black.copy(alpha = 0.4f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color    = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                if (isRegister) Icons.Filled.PersonAdd else Icons.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text       = if (isRegister) "Crea account" else "Accedi",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Toggle login / registrazione ──────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text  = if (isRegister) "Hai già un account? " else "Non hai un account? ",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(
                    onClick = {
                        isRegister = !isRegister
                        // resetta l'eventuale errore precedente
                        viewModel.resetError()
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text       = if (isRegister) "Accedi" else "Registrati",
                        color      = Cyan400,
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Footer ────────────────────────────────────────────────────
            Text(
                text      = "I tuoi dati sono al sicuro\ne non vengono condivisi con terzi",
                color     = TextMuted.copy(alpha = 0.6f),
                style     = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Cyan400,
    unfocusedBorderColor = Color(0xFF2D3748),
    focusedLabelColor    = Cyan400,
    unfocusedLabelColor  = TextMuted,
    cursorColor          = Cyan400,
    focusedTextColor     = TextPrimary,
    unfocusedTextColor   = TextPrimary,
    focusedContainerColor   = DarkSurfaceVariant.copy(alpha = 0.5f),
    unfocusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.3f)
)
