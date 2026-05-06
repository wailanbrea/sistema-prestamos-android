package com.sistemaprestamista.mobile.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.DashboardSummary
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.data.model.UserProfile
import com.sistemaprestamista.mobile.printing.A4ReceiptPrinter
import com.sistemaprestamista.mobile.printing.BluetoothPrinter
import com.sistemaprestamista.mobile.printing.BluetoothReceiptPrinter
import com.sistemaprestamista.mobile.printing.PrintSettingsStore
import com.sistemaprestamista.mobile.printing.ThermalPaper
import com.sistemaprestamista.mobile.ui.components.EmptyCard
import com.sistemaprestamista.mobile.ui.components.LoadingSplash
import com.sistemaprestamista.mobile.ui.components.MetricCard
import com.sistemaprestamista.mobile.ui.components.StatusPill
import com.sistemaprestamista.mobile.ui.components.rememberCurrency
import com.sistemaprestamista.mobile.ui.navigation.AppDestination
import com.sistemaprestamista.mobile.ui.navigation.AppRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
@Composable
fun PrestamistaApp(
    viewModel: MainViewModel,
    printSettingsStore: PrintSettingsStore,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    if (state.isLoading && !state.isAuthenticated) {
        LoadingSplash()
        return
    }

    if (!state.isAuthenticated) {
        LoginScreen(
            isLoading = state.isLoading,
            snackbarHostState = snackbarHostState,
            onLogin = viewModel::login,
        )
        return
    }

    AuthenticatedShell(
        state = state,
        snackbarHostState = snackbarHostState,
        printSettingsStore = printSettingsStore,
        onRefresh = viewModel::refreshDashboard,
        onRegisterPayment = viewModel::registerPayment,
        onLogout = viewModel::logout,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedShell(
    state: AppUiState,
    snackbarHostState: SnackbarHostState,
    printSettingsStore: PrintSettingsStore,
    onRefresh: () -> Unit,
    onRegisterPayment: (Long, String) -> Unit,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    val destinations = if (state.isCollector) {
        AppDestination.entries
    } else {
        listOf(AppDestination.Home, AppDestination.Profile)
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = currentDestination(navController, destinations)
    val isTopLevelDestination = destinations.any { it.route == currentRoute }
    var navigatedReceiptId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.lastPaymentReceipt?.id) {
        val receiptId = state.lastPaymentReceipt?.id ?: return@LaunchedEffect
        if (navigatedReceiptId != receiptId) {
            navigatedReceiptId = receiptId
            navController.navigate(AppRoutes.ReceiptDetail)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentTitle(currentRoute, currentDestination),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = state.user?.company?.name.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    if (!isTopLevelDestination) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
                        }
                    }
                    IconButton(onClick = onRefresh, enabled = !state.isLoading) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Salir")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                destinations.forEach { item ->
                    NavigationBarItem(
                        selected = currentDestination == item,
                        onClick = { navController.navigateSingleTopTo(item) },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.Home.route,
            ) {
                composable(AppDestination.Home.route) {
                    HomeScreen(
                        state = state,
                        onOpenReceipt = {
                            if (state.lastPaymentReceipt != null) {
                                navController.navigate(AppRoutes.ReceiptDetail)
                            }
                        },
                    )
                }
                composable(AppDestination.Collections.route) {
                    CollectionsScreen(
                        state = state,
                        onRegisterPayment = onRegisterPayment,
                        onOpenInstallment = { installmentId ->
                            navController.navigate(AppRoutes.installmentDetail(installmentId))
                        },
                    )
                }
                composable(AppDestination.Clients.route) {
                    ClientsScreen(
                        clients = state.collectorClients,
                        onOpenClient = { clientId ->
                            navController.navigate(AppRoutes.clientDetail(clientId))
                        },
                    )
                }
                composable(AppDestination.Profile.route) {
                    ProfileScreen(
                        user = state.user,
                        onOpenPrintSettings = { navController.navigate(AppRoutes.PrintSettings) },
                        onLogout = onLogout,
                    )
                }
                composable(AppRoutes.ClientDetail) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId")?.toLongOrNull()
                    ClientDetailScreen(
                        client = state.collectorClients.firstOrNull { it.id == clientId },
                        loans = state.collectorLoans.filter { it.client?.id == clientId },
                        installments = state.collectorInstallments.filter { it.client?.id == clientId },
                        onOpenInstallment = { installmentId ->
                            navController.navigate(AppRoutes.installmentDetail(installmentId))
                        },
                    )
                }
                composable(AppRoutes.InstallmentDetail) { backStackEntry ->
                    val installmentId = backStackEntry.arguments?.getString("installmentId")?.toLongOrNull()
                    InstallmentDetailScreen(
                        installment = state.collectorInstallments.firstOrNull { it.id == installmentId },
                        isLoading = state.isLoading,
                        onRegisterPayment = onRegisterPayment,
                    )
                }
                composable(AppRoutes.ReceiptDetail) {
                    ReceiptDetailScreen(
                        receipt = state.lastPaymentReceipt,
                        printSettingsStore = printSettingsStore,
                    )
                }
                composable(AppRoutes.PrintSettings) {
                    PrintSettingsScreen(printSettingsStore = printSettingsStore)
                }
            }
        }
    }
}

@Composable
private fun currentDestination(
    navController: NavHostController,
    destinations: List<AppDestination>,
): AppDestination {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    return destinations.firstOrNull { it.route == route }
        ?: when (route) {
            AppRoutes.ClientDetail -> AppDestination.Clients
            AppRoutes.InstallmentDetail, AppRoutes.ReceiptDetail -> AppDestination.Collections
            else -> AppDestination.Home
        }
}

private fun currentTitle(route: String?, fallback: AppDestination): String {
    return when (route) {
        AppRoutes.ClientDetail -> "Detalle cliente"
        AppRoutes.InstallmentDetail -> "Detalle cuota"
        AppRoutes.ReceiptDetail -> "Recibo"
        AppRoutes.PrintSettings -> "Impresora"
        else -> fallback.title
    }
}

private fun NavHostController.navigateSingleTopTo(destination: AppDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun LoginScreen(
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    onLogin: (String, String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
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
                onValueChange = { email = it },
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
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onLogin(email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Entrar")
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: AppUiState,
    onOpenReceipt: () -> Unit,
) {
    val dashboard = state.dashboard

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            WelcomeCard(state)
        }
        if (dashboard != null) {
            item { FinanceOverview(dashboard) }
        }
        item { CollectorOverview(state) }
        item { LastReceiptCard(state, onOpenReceipt) }
    }
}

@Composable
private fun WelcomeCard(state: AppUiState) {
    val user = state.user ?: return

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Hola, ${user.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = user.roles.joinToString().ifBlank { "Sin rol asignado" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun FinanceOverview(summary: DashboardSummary) {
    val currency = rememberCurrency()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Capital", currency.format(summary.capitalPrestado), Modifier.weight(1f))
            MetricCard("Cobros hoy", currency.format(summary.cobrosHoy), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Ganancia", currency.format(summary.gananciaNeta), Modifier.weight(1f))
            MetricCard("En mora", summary.prestamosMora.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun CollectorOverview(state: AppUiState) {
    val summary = state.collectorSummary ?: return
    val currency = rememberCurrency()

    Card(shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Ruta de cobro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Clientes", summary.assignedClients.toString(), Modifier.weight(1f), compact = true)
                MetricCard("Cuotas", summary.pendingInstallments.toString(), Modifier.weight(1f), compact = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Atrasos", summary.lateLoans.toString(), Modifier.weight(1f), compact = true)
                MetricCard("Hoy", currency.format(summary.collectedToday), Modifier.weight(1f), compact = true)
            }
        }
    }
}

@Composable
private fun CollectionsScreen(
    state: AppUiState,
    onRegisterPayment: (Long, String) -> Unit,
    onOpenInstallment: (Long) -> Unit,
) {
    val installments = state.collectorInstallments

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (installments.isEmpty()) {
            item { EmptyCard("No hay cuotas pendientes para este cobrador.") }
        } else {
            items(installments, key = { it.id }) { installment ->
                InstallmentCard(
                    installment = installment,
                    isLoading = state.isLoading,
                    onRegisterPayment = onRegisterPayment,
                    onOpenInstallment = onOpenInstallment,
                )
            }
        }
    }
}

@Composable
private fun InstallmentCard(
    installment: InstallmentSummary,
    isLoading: Boolean,
    onRegisterPayment: (Long, String) -> Unit,
    onOpenInstallment: (Long) -> Unit,
) {
    var amount by remember(installment.id) {
        mutableStateOf("%.2f".format(Locale.US, installment.pendingAmount))
    }
    val currency = rememberCurrency()
    val isLate = installment.daysLate > 0

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLate) Color(0xFFFFF1F0) else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = installment.client?.fullName ?: "Cliente sin nombre",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${installment.loanNumber} · cuota ${installment.installmentNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusPill(if (isLate) "${installment.daysLate} días" else installment.status)
            }
            Text(
                text = "Vence ${installment.dueDate ?: "-"} · pendiente ${currency.format(installment.pendingAmount)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Monto a cobrar") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
            )
            Button(
                onClick = { onRegisterPayment(installment.loanId, amount) },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Registrar cobro")
            }
            OutlinedButton(
                onClick = { onOpenInstallment(installment.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Ver detalle")
            }
        }
    }
}

@Composable
private fun ClientsScreen(
    clients: List<ClientSummary>,
    onOpenClient: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (clients.isEmpty()) {
            item { EmptyCard("No hay clientes asignados para este cobrador.") }
        } else {
            items(clients, key = { it.id }) { client ->
                ClientCard(client, onOpenClient)
            }
        }
    }
}

@Composable
private fun ClientCard(
    client: ClientSummary,
    onOpenClient: (Long) -> Unit,
) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(client.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = listOfNotNull(client.phone, client.identification).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!client.address.isNullOrBlank()) {
                Text(client.address, style = MaterialTheme.typography.bodyMedium)
            }
            StatusPill("${client.status} · riesgo ${client.riskLevel}")
            OutlinedButton(
                onClick = { onOpenClient(client.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Ver expediente")
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    user: UserProfile?,
    onOpenPrintSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    if (user == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(user.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(user.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(user.company.name, style = MaterialTheme.typography.bodyLarge)
                Text(user.roles.joinToString().ifBlank { "Sin rol asignado" })
            }
        }
        FilledTonalButton(
            onClick = onOpenPrintSettings,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Configurar impresora")
        }
        FilledTonalButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Cerrar sesión")
        }
    }
}

@Composable
private fun LastReceiptCard(
    state: AppUiState,
    onOpenReceipt: () -> Unit,
) {
    val receipt = state.lastPaymentReceipt ?: return
    val currency = rememberCurrency()

    Card(shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("Último recibo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(receipt.receiptNumber, style = MaterialTheme.typography.bodyLarge)
            Text(receipt.client?.fullName ?: "Cliente no disponible")
            Text("Monto ${currency.format(receipt.amount)} · balance ${currency.format(receipt.newBalance)}")
            OutlinedButton(
                onClick = onOpenReceipt,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Ver recibo")
            }
        }
    }
}

@Composable
private fun ClientDetailScreen(
    client: ClientSummary?,
    loans: List<LoanSummary>,
    installments: List<InstallmentSummary>,
    onOpenInstallment: (Long) -> Unit,
) {
    if (client == null) {
        EmptyCard("No se encontró el cliente seleccionado.")
        return
    }

    val currency = rememberCurrency()
    val activeBalance = loans.sumOf { it.remainingBalance }
    val pendingAmount = installments.sumOf { it.pendingAmount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(client.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(listOfNotNull(client.phone, client.identification).joinToString(" · "))
                    if (!client.address.isNullOrBlank()) {
                        Text(client.address)
                    }
                    StatusPill("${client.status} · riesgo ${client.riskLevel}")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Balance", currency.format(activeBalance), Modifier.weight(1f))
                MetricCard("Pendiente", currency.format(pendingAmount), Modifier.weight(1f))
            }
        }
        item {
            Text("Préstamos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (loans.isEmpty()) {
            item { EmptyCard("Este cliente no tiene préstamos asignados al cobrador.") }
        } else {
            items(loans, key = { it.id }) { loan ->
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(loan.loanNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Cuota ${currency.format(loan.installmentAmount)} · balance ${currency.format(loan.remainingBalance)}")
                        StatusPill("${loan.paymentFrequency} · ${loan.status}")
                    }
                }
            }
        }
        item {
            Text("Cuotas pendientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (installments.isEmpty()) {
            item { EmptyCard("No hay cuotas pendientes para este cliente.") }
        } else {
            items(installments, key = { it.id }) { installment ->
                CompactInstallmentCard(installment, onOpenInstallment)
            }
        }
    }
}

@Composable
private fun CompactInstallmentCard(
    installment: InstallmentSummary,
    onOpenInstallment: (Long) -> Unit,
) {
    val currency = rememberCurrency()

    Card(shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("${installment.loanNumber} · cuota ${installment.installmentNumber}", fontWeight = FontWeight.SemiBold)
            Text("Vence ${installment.dueDate ?: "-"} · pendiente ${currency.format(installment.pendingAmount)}")
            StatusPill(if (installment.daysLate > 0) "${installment.daysLate} días atraso" else installment.status)
            OutlinedButton(
                onClick = { onOpenInstallment(installment.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Ver cuota")
            }
        }
    }
}

@Composable
private fun InstallmentDetailScreen(
    installment: InstallmentSummary?,
    isLoading: Boolean,
    onRegisterPayment: (Long, String) -> Unit,
) {
    if (installment == null) {
        EmptyCard("No se encontró la cuota seleccionada.")
        return
    }

    var amount by remember(installment.id) {
        mutableStateOf("%.2f".format(Locale.US, installment.pendingAmount))
    }
    val currency = rememberCurrency()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(installment.client?.fullName ?: "Cliente sin nombre", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${installment.loanNumber} · cuota ${installment.installmentNumber}")
                StatusPill(if (installment.daysLate > 0) "${installment.daysLate} días atraso" else installment.status)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Capital", currency.format(installment.principalAmount), Modifier.weight(1f))
            MetricCard("Interés", currency.format(installment.interestAmount), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Mora", currency.format(installment.lateFee), Modifier.weight(1f))
            MetricCard("Pendiente", currency.format(installment.pendingAmount), Modifier.weight(1f))
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Monto a cobrar") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                )
                Button(
                    onClick = { onRegisterPayment(installment.loanId, amount) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Registrar cobro")
                }
            }
        }
    }
}

@Composable
private fun ReceiptDetailScreen(
    receipt: PaymentReceipt?,
    printSettingsStore: PrintSettingsStore,
) {
    if (receipt == null) {
        EmptyCard("Todavía no hay un recibo generado en esta sesión.")
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bluetoothPrinter = remember(context) { BluetoothReceiptPrinter(context.applicationContext) }
    var selectedPaper by remember { mutableStateOf(printSettingsStore.thermalPaper()) }
    var printers by remember { mutableStateOf<List<BluetoothPrinter>>(emptyList()) }
    var selectedPrinter by remember { mutableStateOf(printSettingsStore.selectedPrinter()) }
    var printMessage by remember { mutableStateOf<String?>(null) }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            printers = bluetoothPrinter.pairedPrinters()
            selectedPrinter = printers.firstOrNull()
        } else {
            printMessage = "Permiso Bluetooth denegado."
        }
    }
    val currency = rememberCurrency()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Recibo de pago", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(receipt.receiptNumber, style = MaterialTheme.typography.bodyLarge)
                Text(receipt.client?.fullName ?: "Cliente no disponible")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Monto", currency.format(receipt.amount), Modifier.weight(1f))
            MetricCard("Balance", currency.format(receipt.newBalance), Modifier.weight(1f))
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Aplicación del pago", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Capital: ${currency.format(receipt.principalPaid)}")
                Text("Interés: ${currency.format(receipt.interestPaid)}")
                Text("Mora: ${currency.format(receipt.lateFeePaid)}")
                Text("Balance anterior: ${currency.format(receipt.previousBalance)}")
                Text("Método: ${receipt.paymentMethod}")
                Text("Estado: ${receipt.status}")
            }
        }
        FilledTonalButton(
            onClick = { A4ReceiptPrinter(context).printReceipt(receipt) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Imprimir A4")
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Impresora térmica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThermalPaper.entries.forEachIndexed { index, paper ->
                        SegmentedButton(
                            selected = selectedPaper == paper,
                            onClick = {
                                selectedPaper = paper
                                printSettingsStore.saveThermalPaper(paper)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, ThermalPaper.entries.size),
                        ) {
                            Text(paper.label)
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !bluetoothPrinter.hasConnectPermission()) {
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            printers = bluetoothPrinter.pairedPrinters()
                            selectedPrinter = printers.firstOrNull()
                            if (printers.isEmpty()) {
                                printMessage = "No hay impresoras Bluetooth vinculadas."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Buscar vinculadas")
                }
                printers.forEach { printer ->
                    OutlinedButton(
                        onClick = {
                            selectedPrinter = printer
                            printSettingsStore.savePrinter(printer, selectedPaper)
                            printMessage = "Impresora predeterminada: ${printer.name}."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (selectedPrinter?.address == printer.address) "✓ ${printer.name}" else printer.name)
                    }
                }
                Button(
                    onClick = {
                        val printer = selectedPrinter
                        if (printer == null) {
                            printMessage = "Selecciona una impresora vinculada."
                            return@Button
                        }

                        scope.launch {
                            printMessage = "Imprimiendo..."
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    bluetoothPrinter.printReceipt(printer.address, receipt, selectedPaper)
                                }
                            }.onSuccess {
                                printMessage = "Recibo enviado a ${printer.name}."
                            }.onFailure {
                                printMessage = it.message ?: "No se pudo imprimir el recibo."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Imprimir ${selectedPaper.label}")
                }
                Text(
                    text = selectedPrinter?.let { "Predeterminada: ${it.name} · ${selectedPaper.label}" }
                        ?: "Sin impresora predeterminada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (printMessage != null) {
                    Text(
                        text = printMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrintSettingsScreen(printSettingsStore: PrintSettingsStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bluetoothPrinter = remember(context) { BluetoothReceiptPrinter(context.applicationContext) }
    var selectedPaper by remember { mutableStateOf(printSettingsStore.thermalPaper()) }
    var printers by remember { mutableStateOf<List<BluetoothPrinter>>(emptyList()) }
    var selectedPrinter by remember { mutableStateOf(printSettingsStore.selectedPrinter()) }
    var message by remember { mutableStateOf<String?>(null) }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            printers = bluetoothPrinter.pairedPrinters()
            selectedPrinter = selectedPrinter ?: printers.firstOrNull()
        } else {
            message = "Permiso Bluetooth denegado."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Configuración de impresión", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "Selecciona la impresora predeterminada y el tamaño térmico para recibos. Los documentos importantes se imprimen en A4 desde la pantalla del recibo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Papel térmico por defecto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThermalPaper.entries.forEachIndexed { index, paper ->
                        SegmentedButton(
                            selected = selectedPaper == paper,
                            onClick = {
                                selectedPaper = paper
                                printSettingsStore.saveThermalPaper(paper)
                                selectedPrinter?.let { printSettingsStore.savePrinter(it, paper) }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, ThermalPaper.entries.size),
                        ) {
                            Text(paper.label)
                        }
                    }
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Impresora Bluetooth", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = selectedPrinter?.let { "Actual: ${it.name}" } ?: "No hay impresora predeterminada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !bluetoothPrinter.hasConnectPermission()) {
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            printers = bluetoothPrinter.pairedPrinters()
                            selectedPrinter = selectedPrinter ?: printers.firstOrNull()
                            if (printers.isEmpty()) {
                                message = "No hay impresoras Bluetooth vinculadas."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Buscar impresoras vinculadas")
                }
                printers.forEach { printer ->
                    OutlinedButton(
                        onClick = {
                            selectedPrinter = printer
                            printSettingsStore.savePrinter(printer, selectedPaper)
                            message = "Guardada: ${printer.name} · ${selectedPaper.label}."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (selectedPrinter?.address == printer.address) "✓ ${printer.name}" else printer.name)
                    }
                }
                OutlinedButton(
                    onClick = {
                        selectedPrinter = null
                        printSettingsStore.clearPrinter()
                        message = "Impresora predeterminada eliminada."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Quitar predeterminada")
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Prueba", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = {
                        val printer = selectedPrinter
                        if (printer == null) {
                            message = "Selecciona una impresora antes de probar."
                            return@Button
                        }

                        scope.launch {
                            message = "Enviando prueba..."
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    bluetoothPrinter.printReceipt(
                                        printerAddress = printer.address,
                                        receipt = sampleReceipt(),
                                        paper = selectedPaper,
                                    )
                                }
                            }.onSuccess {
                                message = "Prueba enviada a ${printer.name}."
                            }.onFailure {
                                message = it.message ?: "No se pudo imprimir la prueba."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Imprimir prueba ${selectedPaper.label}")
                }
                if (message != null) {
                    Text(
                        text = message.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun sampleReceipt(): PaymentReceipt {
    return PaymentReceipt(
        id = 0,
        receiptNumber = "PRUEBA",
        loanId = 0,
        loanNumber = "PRE-PRUEBA",
        client = ClientSummary(
            id = 0,
            fullName = "Cliente de prueba",
            identification = null,
            phone = null,
            address = null,
            status = "active",
            riskLevel = "low",
        ),
        paymentDate = "2026-05-06",
        amount = 100.0,
        principalPaid = 80.0,
        interestPaid = 20.0,
        lateFeePaid = 0.0,
        previousBalance = 500.0,
        newBalance = 420.0,
        paymentMethod = "cash",
        status = "valid",
    )
}




