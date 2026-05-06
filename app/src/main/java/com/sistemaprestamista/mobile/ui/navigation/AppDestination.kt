package com.sistemaprestamista.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    Home("home", "Inicio", Icons.Outlined.Home),
    Collections("collections", "Cobros", Icons.AutoMirrored.Outlined.ReceiptLong),
    Clients("clients", "Clientes", Icons.Outlined.Groups),
    Payments("payments", "Historial", Icons.Outlined.History),
    Profile("profile", "Perfil", Icons.Outlined.Person),
}

object AppRoutes {
    const val ClientDetail = "clients/{clientId}"
    const val LoanDetail = "loans/{loanId}"
    const val InstallmentDetail = "installments/{installmentId}"
    const val ReceiptDetail = "receipt"
    const val PrintSettings = "settings/printer"

    fun clientDetail(clientId: Long): String = "clients/$clientId"

    fun loanDetail(loanId: Long): String = "loans/$loanId"

    fun installmentDetail(installmentId: Long): String = "installments/$installmentId"
}
