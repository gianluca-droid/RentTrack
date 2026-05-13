package com.renttrack.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renttrack.app.ui.theme.*
import com.renttrack.app.viewmodel.AuthState
import com.renttrack.app.viewmodel.AuthViewModel

@Composable
fun ResetPasswordScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    var password     by remember { mutableStateOf("") }
    var confirmPw    by remember { mutableStateOf("") }
    var showPw       by remember { mutableStateOf(false) }
    var showConfirmPw by remember { mutableStateOf(false) }
    val confirmFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val isLoading  = authState is AuthState.Loading
    val errorMsg   = (authState as? AuthState.Error)?.message
    val isSuccess  = authState is AuthState.PasswordResetDone

    val passwordsMatch = password == confirmPw
    val isValid = password.length >= 6 && confirmPw.isNotEmpty() && passwordsMatch

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            kotlinx.coroutines.delay(1500)
            onSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050810), DarkBg, Color(0xFF0D1B2A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icona
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(Cyan400, Purple500))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LockReset,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                "Crea nuova password",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                "Scegli una password sicura di almeno 6 caratteri",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            // Card form
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Campo nuova password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Nuova password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = TextMuted) },
                        trailingIcon = {
                            IconButton(onClick = { showPw = !showPw }) {
                                Icon(
                                    if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    null, tint = TextMuted
                                )
                            }
                        },
                        visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isLoading && !isSuccess,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { confirmFocus.requestFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = resetFieldColors()
                    )

                    // Conferma password
                    OutlinedTextField(
                        value = confirmPw,
                        onValueChange = { confirmPw = it },
                        label = { Text("Conferma password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = TextMuted) },
                        trailingIcon = {
                            if (confirmPw.isNotEmpty()) {
                                Icon(
                                    if (passwordsMatch) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                    null,
                                    tint = if (passwordsMatch) Green400 else Red400
                                )
                            } else {
                                IconButton(onClick = { showConfirmPw = !showConfirmPw }) {
                                    Icon(
                                        if (showConfirmPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        null, tint = TextMuted
                                    )
                                }
                            }
                        },
                        visualTransformation = if (showConfirmPw) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isLoading && !isSuccess,
                        isError = confirmPw.isNotEmpty() && !passwordsMatch,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (isValid) viewModel.resetPassword(password)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(confirmFocus),
                        shape = RoundedCornerShape(14.dp),
                        colors = resetFieldColors()
                    )

                    // Errore
                    AnimatedVisibility(
                        visible = errorMsg != null,
                        enter = fadeIn() + expandVertically(),
                        exit  = fadeOut() + shrinkVertically()
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
                                Icon(Icons.Filled.Warning, null, tint = Red400, modifier = Modifier.size(16.dp))
                                Text(errorMsg, color = Red400, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Successo
                    AnimatedVisibility(
                        visible = isSuccess,
                        enter = fadeIn() + expandVertically(),
                        exit  = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Green400.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Green400, modifier = Modifier.size(16.dp))
                            Text(
                                "Password aggiornata! Reindirizzamento…",
                                color = Green400,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Pulsante conferma
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.resetPassword(password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = isValid && !isLoading && !isSuccess,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Cyan400,
                            contentColor = Color.Black,
                            disabledContainerColor = Cyan400.copy(alpha = 0.3f),
                            disabledContentColor = Color.Black.copy(alpha = 0.4f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.LockReset, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Aggiorna password", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun resetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor    = Cyan400,
    unfocusedBorderColor  = Color(0xFF2D3748),
    focusedLabelColor     = Cyan400,
    unfocusedLabelColor   = TextMuted,
    cursorColor           = Cyan400,
    focusedTextColor      = TextPrimary,
    unfocusedTextColor    = TextPrimary,
    focusedContainerColor    = DarkSurfaceVariant.copy(alpha = 0.5f),
    unfocusedContainerColor  = DarkSurfaceVariant.copy(alpha = 0.3f)
)
