package com.renttrack.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.FocusDirection
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
    val focusManager = LocalFocusManager.current

    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    // Reagisce ai cambiamenti di stato
    LaunchedEffect(authState) {
        when (val s = authState) {
            is AuthState.LoggedIn -> onLoginSuccess()
            is AuthState.Error    -> errorMsg = s.message
            is AuthState.EmailSent -> {
                errorMsg = null
                // mostrato sotto come card informativa
            }
            else -> {}
        }
    }

    val isLoading   = authState is AuthState.Loading
    val emailSent   = authState is AuthState.EmailSent
    val sentToEmail = (authState as? AuthState.EmailSent)?.email ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DarkBg, Color(0xFF0D1B2E), DarkBg)
                )
            )
    ) {
        // ── Sfondo decorativo ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-60).dp, y = (-60).dp)
                .clip(CircleShape)
                .background(Cyan400.copy(alpha = 0.04f))
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .clip(CircleShape)
                .background(Purple400.copy(alpha = 0.05f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // ── Logo ─────────────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Cyan400.copy(alpha = 0.12f))
            ) {
                Icon(
                    Icons.Filled.HomeWork,
                    contentDescription = null,
                    tint = Cyan400,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "RentTrack",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = TextPrimary
            )

            Text(
                if (isRegisterMode) "Crea il tuo account" else "Bentornato",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(40.dp))

            // ── Card form ────────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = DarkSurface,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // Email
                    Text(
                        "Email",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMsg = null },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("nome@email.com", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, null, tint = TextMuted)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Cyan400,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = Cyan400,
                            focusedContainerColor   = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // Password
                    Text(
                        "Password",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("••••••••", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, null, tint = TextMuted)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    contentDescription = "Mostra/Nascondi",
                                    tint = TextMuted
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (isRegisterMode) viewModel.signUp(email, password)
                                else viewModel.signIn(email, password)
                            }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Cyan400,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = Cyan400,
                            focusedContainerColor   = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant
                        )
                    )

                    // Errore
                    AnimatedVisibility(visible = errorMsg != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Red400.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.ErrorOutline, null,
                                    tint = Red400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    errorMsg ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Red400
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Pulsante principale
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            errorMsg = null
                            if (isRegisterMode) viewModel.signUp(email, password)
                            else viewModel.signIn(email, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled  = !isLoading && email.isNotBlank() && password.length >= 6,
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = Cyan400,
                            contentColor   = Color.Black
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color    = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (isRegisterMode) "Crea account" else "Accedi",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Switch login/registra
                    TextButton(
                        onClick = { isRegisterMode = !isRegisterMode; errorMsg = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isRegisterMode)
                                "Hai già un account? Accedi"
                            else
                                "Non hai un account? Registrati",
                            style = MaterialTheme.typography.bodySmall,
                            color = Cyan400
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Card conferma email inviata ───────────────────────────────────
            AnimatedVisibility(
                visible = emailSent,
                enter = fadeIn() + expandVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Green400.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            tint = Green400,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Email di conferma inviata!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Green400
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Controlla la casella di posta di\n$sentToEmail\ne clicca il link per attivare l'account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isRegisterMode = false
                                viewModel.checkSession()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Green400,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Vai al login", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Divisore ─────────────────────────────────────────────────────
            if (!emailSent) {
                Spacer(Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = DarkSurfaceVariant)
                    Text("  oppure  ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = DarkSurfaceVariant)
                }
                Spacer(Modifier.height(20.dp))

                // ── Google Sign-In ────────────────────────────────────────────
                OutlinedButton(
                    onClick = { /* TODO: Google Sign-In */ },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
                ) {
                    Text(
                        "G",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
                        ),
                        color = Color(0xFF4285F4)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Continua con Google",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "Continuando, accetti i Termini di Servizio\ne la Privacy Policy di RentTrack",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
