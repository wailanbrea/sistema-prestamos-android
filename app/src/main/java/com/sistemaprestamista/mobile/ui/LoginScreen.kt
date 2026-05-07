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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val LOGIN_PREFS_NAME = "login_preferences"
private const val KEY_REMEMBER_USER = "remember_user"
private const val KEY_REMEMBERED_EMAIL = "remembered_email"

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

    val biometricAvailable = remember(context, hasSavedSession) {
        hasSavedSession &&
                BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

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

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Sistema Prestamista",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Acceso seguro para operaciones de cobro.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Correo") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(14.dp),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contraseña") },
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
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                )

                Text(
                    text = "Recordar usuario",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    saveRememberedUser()
                    onLogin(email.trim(), password)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Entrar")
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = { showRecoveryDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.LockReset, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Recuperar")
                }

                OutlinedButton(
                    onClick = ::launchBiometricPrompt,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && biometricAvailable,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Huella")
                }
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
