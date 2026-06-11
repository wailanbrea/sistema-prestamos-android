package com.sistemaprestamista.mobile.ui

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val LOGIN_PREFS_NAME = "login_preferences"
private const val KEY_REMEMBER_USER = "remember_user"
private const val KEY_REMEMBERED_EMAIL = "remembered_email"

private val BrandBlue = Color(0xFF0F4C81)
private val BrandBlueDark = Color(0xFF082F54)
private val BrandGreen = Color(0xFF166534)
private val BrandGreenDark = Color(0xFF14532D)
private val CardWhite = Color(0xFFFFFFFF)
private val FieldBackground = Color(0xFFF8FAFC)
private val TextMain = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val FieldBorder = Color(0xFFE2E8F0)

@Composable
internal fun LoginScreen(
    isLoading: Boolean,
    hasSavedSession: Boolean,
    snackbarHostState: SnackbarHostState,
    onLogin: (String, String) -> Unit,
    onBiometricLogin: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onResetPassword: (String, String, String, String) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val preferences = remember {
        context.getSharedPreferences(LOGIN_PREFS_NAME, Context.MODE_PRIVATE)
    }

    val savedRememberUser = remember {
        preferences.getBoolean(KEY_REMEMBER_USER, false)
    }

    val savedEmail = remember {
        if (savedRememberUser) {
            preferences.getString(KEY_REMEMBERED_EMAIL, "").orEmpty()
        } else {
            ""
        }
    }

    val biometricHardwareReady = remember(context) {
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }
    val biometricEnabled = biometricHardwareReady && hasSavedSession

    var email by remember { mutableStateOf(savedEmail) }
    var password by remember { mutableStateOf("") }
    var rememberUser by remember { mutableStateOf(savedRememberUser) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    fun saveRememberedUser() {
        preferences.edit().apply {
            putBoolean(KEY_REMEMBER_USER, rememberUser)

            if (rememberUser) {
                putString(KEY_REMEMBERED_EMAIL, email.trim())
            } else {
                remove(KEY_REMEMBERED_EMAIL)
            }

            apply()
        }
    }

    fun launchBiometricPrompt() {
        if (activity == null) return

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onBiometricLogin()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        // El mensaje del sistema ya explica el motivo.
                    }
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Entrar con huella")
            .setSubtitle("Confirma tu identidad para abrir la sesión guardada")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(promptInfo)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BrandBlue, BrandBlueDark),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(48.dp))

                // Marca BSP: mismo monograma y verde del ícono de la app.
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BrandGreen, BrandGreenDark),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "BSP",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Sistema Prestamista",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                Text(
                    text = "Acceso seguro para operaciones de cobro",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "Bienvenido",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextMain,
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Correo") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Mail,
                                    contentDescription = null,
                                    tint = TextMuted,
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FieldBackground,
                                unfocusedContainerColor = FieldBackground,
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = FieldBorder,
                                focusedLabelColor = BrandBlue,
                            ),
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Contraseña") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = TextMuted,
                                )
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Outlined.VisibilityOff
                                        } else {
                                            Icons.Outlined.Visibility
                                        },
                                        contentDescription = if (passwordVisible) {
                                            "Ocultar contraseña"
                                        } else {
                                            "Ver contraseña"
                                        },
                                        tint = TextMuted,
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FieldBackground,
                                unfocusedContainerColor = FieldBackground,
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = FieldBorder,
                                focusedLabelColor = BrandBlue,
                            ),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = rememberUser,
                                    onCheckedChange = { checked ->
                                        rememberUser = checked

                                        if (!checked) {
                                            preferences.edit()
                                                .putBoolean(KEY_REMEMBER_USER, false)
                                                .remove(KEY_REMEMBERED_EMAIL)
                                                .apply()
                                        }
                                    },
                                    enabled = !isLoading,
                                    colors = CheckboxDefaults.colors(checkedColor = BrandBlue),
                                )

                                Text(
                                    text = "Recordarme",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                )
                            }

                            TextButton(
                                onClick = { showRecoveryDialog = true },
                                enabled = !isLoading,
                            ) {
                                Text(
                                    text = "¿Olvidaste tu contraseña?",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                )
                            }
                        }

                        Button(
                            onClick = {
                                saveRememberedUser()
                                onLogin(email.trim(), password)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandBlue,
                                contentColor = Color.White,
                            ),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Text(
                                    text = "Iniciar sesión",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                )
                            }
                        }

                        if (biometricHardwareReady) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(FieldBorder),
                                )

                                Text(
                                    text = "o",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(FieldBorder),
                                )
                            }

                            OutlinedButton(
                                onClick = ::launchBiometricPrompt,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                enabled = !isLoading && biometricEnabled,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = BrandBlue,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = "Entrar con huella",
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            if (!biometricEnabled) {
                                Text(
                                    text = "La huella se habilita después de tu primer inicio de sesión.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "BSP · Gestión de préstamos y cobros",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showRecoveryDialog) {
        PasswordRecoveryDialog(
            initialEmail = email,
            isLoading = isLoading,
            onDismiss = { showRecoveryDialog = false },
            onSubmit = { recoveryEmail ->
                onForgotPassword(recoveryEmail.trim())
                showRecoveryDialog = false
            },
            onReset = { recoveryEmail, token, newPassword, confirmation ->
                onResetPassword(recoveryEmail.trim(), token.trim(), newPassword, confirmation)
                showRecoveryDialog = false
            },
        )
    }
}

@Composable
private fun PasswordRecoveryDialog(
    initialEmail: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    onReset: (String, String, String, String) -> Unit,
) {
    var email by remember { mutableStateOf(initialEmail) }
    var token by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var resetMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (resetMode) "Cambiar contraseña" else "Recuperar contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (resetMode) {
                        "Pega el token recibido y define tu nueva contraseña."
                    } else {
                        "Enviaremos las instrucciones al correo registrado."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Correo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )

                if (resetMode) {
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Token") },
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nueva contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )

                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirmar contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                }

                TextButton(
                    onClick = { resetMode = !resetMode },
                    enabled = !isLoading,
                ) {
                    Text(if (resetMode) "Solicitar instrucciones" else "Ya tengo un token")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (resetMode) {
                        onReset(email.trim(), token.trim(), newPassword, confirmation)
                    } else {
                        onSubmit(email.trim())
                    }
                },
                enabled = !isLoading,
            ) {
                Text(if (resetMode) "Cambiar" else "Enviar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
            ) {
                Text("Cancelar")
            }
        },
    )
}
