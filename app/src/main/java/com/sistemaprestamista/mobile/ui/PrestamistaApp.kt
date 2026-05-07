package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sistemaprestamista.mobile.data.model.PaymentHistoryFilters
import com.sistemaprestamista.mobile.printing.PrintSettingsStore
import com.sistemaprestamista.mobile.ui.components.LoadingSplash
import com.sistemaprestamista.mobile.ui.navigation.AppDestination
import com.sistemaprestamista.mobile.ui.navigation.AppRoutes

private val AppBackground = Color(0xFFF4F7FB)
private val AppSurface = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val PrimaryContainer = Color(0xFF1A4F8B)
private val PrimaryFixed = Color(0xFFD5E3FF)
private val Secondary = Color(0xFF505F76)
private val SecondaryContainer = Color(0xFFD0E1FB)
private val TextVariant = Color(0xFF424750)
private val NavUnselected = Color(0xFF54647A)

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

    LaunchedEffect(state.successMessage) {
        val message = state.successMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearSuccess()
    }

    if (state.isLoading && !state.isAuthenticated) {
        LoadingSplash()
        return
    }

    if (!state.isAuthenticated) {
        LoginScreen(
            isLoading = state.isLoading,
            hasSavedSession = state.hasSavedSession,
            snackbarHostState = snackbarHostState,
            onLogin = viewModel::login,
            onBiometricLogin = viewModel::unlockSavedSession,
            onForgotPassword = viewModel::requestPasswordReset,
            onResetPassword = viewModel::resetPassword,
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
        onLoadInstallmentDetail = viewModel::loadInstallmentDetail,
        onLoadPaymentDetail = viewModel::loadPaymentDetail,
        onLoadPaymentHistory = viewModel::loadPaymentHistory,
        onLoadMapData = viewModel::loadMapData,
        onSelectMapRoute = viewModel::selectMapRoute,
        onRegisterPayment = viewModel::registerPayment,
        onLoadPendingPayments = viewModel::loadPendingPayments,
        onRetryPendingPayment = viewModel::retryPendingPayment,
        onSyncPendingPayments = viewModel::syncPendingPaymentsNow,
        onDiscardPendingPayment = viewModel::discardPendingPayment,
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
    onLoadInstallmentDetail: (Long) -> Unit,
    onLoadPaymentDetail: (Long) -> Unit,
    onLoadPaymentHistory: (PaymentHistoryFilters) -> Unit,
    onLoadMapData: () -> Unit,
    onSelectMapRoute: (Long) -> Unit,
    onRegisterPayment: (Long, String, String) -> Unit,
    onLoadPendingPayments: () -> Unit,
    onRetryPendingPayment: (String) -> Unit,
    onSyncPendingPayments: () -> Unit,
    onDiscardPendingPayment: (String) -> Unit,
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
        containerColor = AppBackground,
        topBar = {
            AppTopBar(
                title = currentTitle(currentRoute, currentDestination),
                subtitle = if (state.pendingPaymentCount > 0) {
                    "${state.user?.company?.name.orEmpty()} · ${state.pendingPaymentCount} cobros pendientes"
                } else {
                    state.user?.company?.name.orEmpty()
                },
                showBack = !isTopLevelDestination,
                isLoading = state.isLoading,
                onBack = { navController.popBackStack() },
                onRefresh = onRefresh,
                onLogout = onLogout,
            )
        },
        bottomBar = {
            AppBottomBar(
                destinations = destinations,
                currentDestination = currentDestination,
                onNavigate = { destination ->
                    navController.navigateSingleTopTo(destination)
                },
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    androidx.compose.material3.Snackbar(
                        snackbarData = data,
                        containerColor = Color(0xFF2E3035),
                        contentColor = Color(0xFFF0F0F6),
                        actionColor = Color(0xFF4EDEA3),
                        shape = RoundedCornerShape(14.dp),
                    )
                },
            )
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = AppBackground,
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

                composable(AppDestination.Map.route) {
                    MapScreen(
                        clients = state.mapClients,
                        routes = state.collectorRoutes,
                        selectedRouteId = state.selectedMapRouteId,
                        realRoutePoints = state.realRoutePoints,
                        routeWarning = state.routeWarning,
                        isLoading = state.isMapLoading,
                        onRefresh = onLoadMapData,
                        onSelectRoute = onSelectMapRoute,
                        onOpenClient = { clientId ->
                            navController.navigate(AppRoutes.clientDetail(clientId))
                        },
                    )
                }

                composable(AppDestination.Payments.route) {
                    PaymentHistoryScreen(
                        payments = state.paymentHistory,
                        clients = state.collectorClients,
                        loans = state.collectorLoans,
                        filters = state.paymentHistoryFilters,
                        isLoading = state.isPaymentHistoryLoading,
                        onApplyFilters = onLoadPaymentHistory,
                        onOpenReceipt = { paymentId ->
                            onLoadPaymentDetail(paymentId)
                            navController.navigate(AppRoutes.ReceiptDetail)
                        },
                    )
                }

                composable(AppDestination.Profile.route) {
                    ProfileScreen(
                        user = state.user,
                        pendingPaymentCount = state.pendingPaymentCount,
                        onOpenPendingPayments = {
                            onLoadPendingPayments()
                            navController.navigate(AppRoutes.PendingPayments)
                        },
                        onOpenPrintSettings = {
                            navController.navigate(AppRoutes.PrintSettings)
                        },
                        onLogout = onLogout,
                    )
                }

                composable(AppRoutes.PendingPayments) {
                    LaunchedEffect(state.pendingPaymentCount) {
                        onLoadPendingPayments()
                    }

                    PendingPaymentsScreen(
                        payments = state.pendingPayments,
                        isLoading = state.isPendingSyncLoading,
                        onRefresh = onSyncPendingPayments,
                        onRetry = onRetryPendingPayment,
                        onDiscard = onDiscardPendingPayment,
                    )
                }

                composable(AppRoutes.ClientDetail) { backStackEntry ->
                    val clientId = backStackEntry.arguments
                        ?.getString("clientId")
                        ?.toLongOrNull()

                    LaunchedEffect(clientId) {
                        if (clientId != null) {
                            onLoadClientDetail(clientId)
                        }
                    }

                    ClientDetailScreen(
                        detail = state.selectedClientDetail
                            ?.takeIf { it.summary.id == clientId },
                        isLoading = state.isDetailLoading,
                        fallbackClient = state.collectorClients
                            .firstOrNull { it.id == clientId },
                        onOpenLoan = { loanId ->
                            navController.navigate(AppRoutes.loanDetail(loanId))
                        },
                        onOpenInstallment = { installmentId ->
                            navController.navigate(AppRoutes.installmentDetail(installmentId))
                        },
                    )
                }

                composable(AppRoutes.LoanDetail) { backStackEntry ->
                    val loanId = backStackEntry.arguments
                        ?.getString("loanId")
                        ?.toLongOrNull()

                    LaunchedEffect(loanId) {
                        if (loanId != null) {
                            onLoadLoanDetail(loanId)
                        }
                    }

                    LoanDetailScreen(
                        detail = state.selectedLoanDetail
                            ?.takeIf { it.summary.id == loanId },
                        isLoading = state.isDetailLoading,
                        fallbackLoan = state.collectorLoans
                            .firstOrNull { it.id == loanId },
                        onOpenInstallment = { installmentId ->
                            navController.navigate(AppRoutes.installmentDetail(installmentId))
                        },
                    )
                }

                composable(AppRoutes.InstallmentDetail) { backStackEntry ->
                    val installmentId = backStackEntry.arguments
                        ?.getString("installmentId")
                        ?.toLongOrNull()

                    LaunchedEffect(installmentId) {
                        if (installmentId != null) {
                            onLoadInstallmentDetail(installmentId)
                        }
                    }

                    InstallmentDetailScreen(
                        detail = state.selectedInstallmentDetail
                            ?.takeIf { it.summary.id == installmentId },
                        fallbackInstallment = state.collectorInstallments
                            .firstOrNull { it.id == installmentId },
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
                    PrintSettingsScreen(
                        printSettingsStore = printSettingsStore,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    subtitle: String,
    showBack: Boolean,
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    color = PrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = subtitle,
                    color = TextVariant,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            if (showBack) {
                IconButton(
                    onClick = onBack,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Atrás",
                        tint = TextVariant,
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onRefresh,
                enabled = !isLoading,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Actualizar",
                    tint = TextVariant,
                )
            }

            IconButton(
                onClick = onLogout,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = "Salir",
                    tint = TextVariant,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppSurface,
            titleContentColor = Primary,
            actionIconContentColor = TextVariant,
            navigationIconContentColor = TextVariant,
        ),
    )
}

@Composable
private fun AppBottomBar(
    destinations: List<AppDestination>,
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
) {
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = AppSurface,
        tonalElevation = 8.dp,
    ) {
        destinations.forEach { item ->
            val selected = currentDestination == item

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item) },
                icon = {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SecondaryContainer)
                                .padding(horizontal = 18.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = PrimaryContainer,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = NavUnselected,
                        )
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        color = if (selected) PrimaryContainer else NavUnselected,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryContainer,
                    selectedTextColor = PrimaryContainer,
                    unselectedIconColor = NavUnselected,
                    unselectedTextColor = NavUnselected,
                    indicatorColor = Color.Transparent,
                ),
            )
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
            AppRoutes.LoanDetail,
            AppRoutes.InstallmentDetail -> AppDestination.Collections
            AppRoutes.ReceiptDetail -> AppDestination.Payments
            AppRoutes.PrintSettings,
            AppRoutes.PendingPayments -> AppDestination.Profile
            else -> AppDestination.Home
        }
}

private fun currentTitle(
    route: String?,
    fallback: AppDestination,
): String {
    return when (route) {
        AppRoutes.ClientDetail -> "Detalle cliente"
        AppRoutes.LoanDetail -> "Detalle préstamo"
        AppRoutes.InstallmentDetail -> "Detalle cuota"
        AppRoutes.ReceiptDetail -> "Recibo"
        AppRoutes.PrintSettings -> "Impresora"
        AppRoutes.PendingPayments -> "Cobros pendientes"
        else -> fallback.title
    }
}

private fun NavHostController.navigateSingleTopTo(
    destination: AppDestination,
) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }

        launchSingleTop = true
        restoreState = true
    }
}
