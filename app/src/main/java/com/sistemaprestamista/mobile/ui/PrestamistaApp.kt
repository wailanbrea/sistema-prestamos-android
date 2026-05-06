package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sistemaprestamista.mobile.printing.PrintSettingsStore
import com.sistemaprestamista.mobile.ui.components.LoadingSplash
import com.sistemaprestamista.mobile.ui.navigation.AppDestination
import com.sistemaprestamista.mobile.ui.navigation.AppRoutes

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
        onLoadClientDetail = viewModel::loadClientDetail,
        onLoadLoanDetail = viewModel::loadLoanDetail,
        onLoadPaymentDetail = viewModel::loadPaymentDetail,
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
    onLoadClientDetail: (Long) -> Unit,
    onLoadLoanDetail: (Long) -> Unit,
    onLoadPaymentDetail: (Long) -> Unit,
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
                    LaunchedEffect(clientId) {
                        if (clientId != null) {
                            onLoadClientDetail(clientId)
                        }
                    }
                    ClientDetailScreen(
                        detail = state.selectedClientDetail?.takeIf { it.summary.id == clientId },
                        isLoading = state.isDetailLoading,
                        fallbackClient = state.collectorClients.firstOrNull { it.id == clientId },
                        onOpenLoan = { loanId ->
                            navController.navigate(AppRoutes.loanDetail(loanId))
                        },
                        onOpenInstallment = { installmentId ->
                            navController.navigate(AppRoutes.installmentDetail(installmentId))
                        },
                    )
                }
                composable(AppRoutes.LoanDetail) { backStackEntry ->
                    val loanId = backStackEntry.arguments?.getString("loanId")?.toLongOrNull()
                    LaunchedEffect(loanId) {
                        if (loanId != null) {
                            onLoadLoanDetail(loanId)
                        }
                    }
                    LoanDetailScreen(
                        detail = state.selectedLoanDetail?.takeIf { it.summary.id == loanId },
                        isLoading = state.isDetailLoading,
                        fallbackLoan = state.collectorLoans.firstOrNull { it.id == loanId },
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
                    LaunchedEffect(state.lastPaymentReceipt?.id) {
                        val paymentId = state.lastPaymentReceipt?.id
                        if (paymentId != null) {
                            onLoadPaymentDetail(paymentId)
                        }
                    }
                    ReceiptDetailScreen(
                        receipt = state.selectedPaymentDetail ?: state.lastPaymentReceipt,
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
            AppRoutes.LoanDetail, AppRoutes.InstallmentDetail, AppRoutes.ReceiptDetail -> AppDestination.Collections
            else -> AppDestination.Home
        }
}

private fun currentTitle(route: String?, fallback: AppDestination): String {
    return when (route) {
        AppRoutes.ClientDetail -> "Detalle cliente"
        AppRoutes.LoanDetail -> "Detalle préstamo"
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


