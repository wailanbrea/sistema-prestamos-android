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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sistemaprestamista.mobile.data.model.PaymentHistoryFilters
import com.sistemaprestamista.mobile.printing.PrintSettingsStore
import com.sistemaprestamista.mobile.tracking.RouteTrackingForegroundService
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
        onStartRouteTracking = viewModel::startRouteTracking,
        onFinishRouteTracking = viewModel::finishRouteTracking,
        onRegisterPayment = viewModel::registerPayment,
        onLoadPendingPayments = viewModel::loadPendingPayments,
        onRetryPendingPayment = viewModel::retryPendingPayment,
        onSyncPendingPayments = viewModel::syncPendingPaymentsNow,
        onDiscardPendingPayment = viewModel::discardPendingPayment,
        onLoadAdminClientDetail = viewModel::loadAdminClientDetail,
        onLoadAdminLoanDetail = viewModel::loadAdminLoanDetail,
        onLoadMoreAdminLoans = viewModel::loadMoreAdminLoans,
        onRegisterAdminPayment = viewModel::registerAdminPayment,
        onGenerateLoanDocument = viewModel::generateLoanDocument,
        onCreateAdminClient = viewModel::createAdminClient,
        onLoadAdminCollectors = viewModel::loadAdminCollectors,
        onCreateAdminLoan = viewModel::createAdminLoan,
        onLoadAdminQuotes = viewModel::loadAdminQuotes,
        onCreateAdminQuote = viewModel::createAdminQuote,
        onLoadAdminQuote = viewModel::loadAdminQuote,
        onDeleteAdminQuote = viewModel::deleteAdminQuote,
        onClearCreationMarkers = viewModel::clearCreationMarkers,
        onUpdateAdminLoan = viewModel::updateAdminLoan,
        onCreateRegistrationLink = viewModel::createClientRegistrationLink,
        onClearRegistrationLink = viewModel::clearRegistrationLink,
        onApproveLoan = viewModel::approveLoan,
        onRejectLoan = viewModel::rejectLoan,
        onCreateExpense = viewModel::createExpense,
        onUpdateAdminClient = viewModel::updateAdminClient,
        onDeleteAdminClient = viewModel::deleteAdminClient,
        onDeleteAdminLoan = viewModel::deleteAdminLoan,
        onCancelPayment = viewModel::cancelPayment,
        onLoadAdminCollectorDetail = viewModel::loadAdminCollectorDetail,
        onCreateAdminCollector = viewModel::createAdminCollector,
        onUpdateAdminCollector = viewModel::updateAdminCollector,
        onPayCollectorCommission = viewModel::payCollectorCommission,
        onStoreAdminCashMovement = viewModel::storeAdminCashMovement,
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
    onStartRouteTracking: (Long, com.sistemaprestamista.mobile.data.model.RoutePoint?) -> Unit,
    onFinishRouteTracking: () -> Unit,
    onRegisterPayment: (Long, String, String, String, Long?) -> Unit,
    onLoadPendingPayments: () -> Unit,
    onRetryPendingPayment: (String) -> Unit,
    onSyncPendingPayments: () -> Unit,
    onDiscardPendingPayment: (String) -> Unit,
    onLoadAdminClientDetail: (Long) -> Unit,
    onLoadAdminLoanDetail: (Long) -> Unit,
    onLoadMoreAdminLoans: () -> Unit,
    onRegisterAdminPayment: (Long, String, String, String, Long?) -> Unit,
    onGenerateLoanDocument: (Long, String) -> Unit,
    onCreateAdminClient: (com.sistemaprestamista.mobile.data.model.NewClientInput) -> Unit,
    onLoadAdminCollectors: () -> Unit,
    onCreateAdminLoan: (com.sistemaprestamista.mobile.data.model.NewLoanInput) -> Unit,
    onLoadAdminQuotes: () -> Unit,
    onCreateAdminQuote: (Long?, Double, Double, String, String, String, Int) -> Unit,
    onLoadAdminQuote: (Long) -> Unit,
    onDeleteAdminQuote: (Long) -> Unit,
    onClearCreationMarkers: () -> Unit,
    onUpdateAdminLoan: (Long, com.sistemaprestamista.mobile.data.model.UpdateLoanInput) -> Unit,
    onCreateRegistrationLink: (String?, String?) -> Unit,
    onClearRegistrationLink: () -> Unit,
    onApproveLoan: (Long) -> Unit,
    onRejectLoan: (Long, String?) -> Unit,
    onCreateExpense: (Long?, String, String, String) -> Unit,
    onUpdateAdminClient: (Long, com.sistemaprestamista.mobile.data.model.UpdateClientInput) -> Unit,
    onDeleteAdminClient: (Long, () -> Unit) -> Unit,
    onDeleteAdminLoan: (Long, () -> Unit) -> Unit,
    onCancelPayment: (Long, String, () -> Unit) -> Unit,
    onLoadAdminCollectorDetail: (Long) -> Unit,
    onCreateAdminCollector: (com.sistemaprestamista.mobile.data.model.NewCollectorInput) -> Unit,
    onUpdateAdminCollector: (Long, com.sistemaprestamista.mobile.data.model.UpdateCollectorInput) -> Unit,
    onPayCollectorCommission: (Long, Long) -> Unit,
    onStoreAdminCashMovement: (com.sistemaprestamista.mobile.data.model.CashMovementInput) -> Unit,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Las vistas se controlan por rol: cada destino declara su predicado isVisible.
    val destinations = AppDestination.entries.filter { it.isVisible(state) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = currentDestination(navController, destinations)
    val isTopLevelDestination = destinations.any { it.route == currentRoute }

    var navigatedReceiptId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.activeRouteSession?.id) {
        if (state.activeRouteSession?.status == "active") {
            ContextCompat.startForegroundService(context, RouteTrackingForegroundService.startIntent(context))
        }
    }

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
                        optimizedRouteClientIds = state.optimizedRouteClientIds,
                        realRoutePoints = state.realRoutePoints,
                        routeWarning = state.routeWarning,
                        activeSession = state.activeRouteSession,
                        isRouteTrackingLoading = state.isRouteTrackingLoading,
                        isLoading = state.isMapLoading,
                        onRefresh = onLoadMapData,
                        onSelectRoute = onSelectMapRoute,
                        onStartTracking = { routeId, origin ->
                            onStartRouteTracking(routeId, origin)
                        },
                        onFinishTracking = {
                            onFinishRouteTracking()
                            context.startService(RouteTrackingForegroundService.stopIntent(context))
                        },
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

                // --- Back-office / administrador ---

                composable(AppDestination.ClientsAdmin.route) {
                    ClientsScreen(
                        clients = state.adminClients,
                        onOpenClient = { clientId ->
                            navController.navigate(AppRoutes.adminClientDetail(clientId))
                        },
                        onCreateClient = if (state.canCreateClients) {
                            { navController.navigate(AppRoutes.AdminClientCreate) }
                        } else {
                            null
                        },
                        onGenerateRegistrationLink = if (state.canCreateClients) onCreateRegistrationLink else null,
                        isGeneratingLink = state.isLinkGenerating,
                        generatedLinkWhatsappUrl = state.lastGeneratedRegistrationLink?.whatsappUrl,
                        generatedLinkFormUrl = state.lastGeneratedRegistrationLink?.formUrl,
                        onDismissGeneratedLink = onClearRegistrationLink,
                    )
                }

                composable(AppRoutes.AdminClientCreate) {
                    // Al confirmar el alta se regresa automáticamente al listado.
                    LaunchedEffect(state.lastCreatedClientId) {
                        if (state.lastCreatedClientId != null) {
                            onClearCreationMarkers()
                            navController.popBackStack()
                        }
                    }

                    ClientCreateScreen(
                        isSaving = state.isClientSaving,
                        onSubmit = onCreateAdminClient,
                    )
                }

                composable(AppDestination.LoansAdmin.route) {
                    AdminLoansScreen(
                        loans = state.adminLoans,
                        onOpenLoan = { loanId ->
                            navController.navigate(AppRoutes.adminLoanDetail(loanId))
                        },
                        hasMore = state.adminLoansHasMore,
                        isLoadingMore = state.isLoadingMoreAdminLoans,
                        onLoadMore = onLoadMoreAdminLoans,
                        onOpenQuotes = if (state.canManageQuotes) {
                            { navController.navigate(AppRoutes.AdminQuotes) }
                        } else {
                            null
                        },
                        onCreateLoan = if (state.canCreateLoans) {
                            { navController.navigate(AppRoutes.AdminLoanCreate) }
                        } else {
                            null
                        },
                    )
                }

                composable(AppRoutes.AdminLoanCreate) {
                    LaunchedEffect(state.lastCreatedLoanId) {
                        val loanId = state.lastCreatedLoanId
                        if (loanId != null) {
                            onClearCreationMarkers()
                            navController.popBackStack()
                            navController.navigate(AppRoutes.adminLoanDetail(loanId))
                        }
                    }

                    LoanCreateScreen(
                        clients = state.adminClients,
                        collectors = state.adminCollectors,
                        isSaving = state.isLoanSaving,
                        onLoadCollectors = onLoadAdminCollectors,
                        onSubmit = onCreateAdminLoan,
                    )
                }

                composable(AppRoutes.AdminQuotes) {
                    LaunchedEffect(Unit) {
                        onLoadAdminQuotes()
                    }

                    QuotesScreen(
                        quotes = state.adminQuotes,
                        isLoading = state.isQuotesLoading,
                        onOpenQuote = { quoteId ->
                            navController.navigate(AppRoutes.adminQuoteDetail(quoteId))
                        },
                        onCreateQuote = {
                            navController.navigate(AppRoutes.AdminQuoteCreate)
                        },
                    )
                }

                composable(AppRoutes.AdminQuoteCreate) {
                    // Tras guardar, se abre directo el detalle con el cronograma calculado.
                    LaunchedEffect(state.lastCreatedQuoteId) {
                        val quoteId = state.lastCreatedQuoteId
                        if (quoteId != null) {
                            onClearCreationMarkers()
                            navController.popBackStack()
                            navController.navigate(AppRoutes.adminQuoteDetail(quoteId))
                        }
                    }

                    QuoteFormScreen(
                        clients = state.adminClients,
                        isSaving = state.isQuoteSaving,
                        onSubmit = onCreateAdminQuote,
                    )
                }

                composable(AppRoutes.AdminQuoteDetail) { backStackEntry ->
                    val quoteId = backStackEntry.arguments?.getString("quoteId")?.toLongOrNull()

                    LaunchedEffect(quoteId) {
                        if (quoteId != null) {
                            onLoadAdminQuote(quoteId)
                        }
                    }

                    // Si se elimina, la cotización desaparece del estado y se vuelve al listado.
                    LaunchedEffect(state.adminQuotes, state.selectedQuote) {
                        if (quoteId != null && state.selectedQuote == null && state.adminQuotes.none { it.id == quoteId } && !state.isDetailLoading) {
                            navController.popBackStack()
                        }
                    }

                    QuoteDetailScreen(
                        quote = state.selectedQuote?.takeIf { it.id == quoteId },
                        isLoading = state.isDetailLoading,
                        isDeleting = state.isQuoteSaving,
                        onDelete = onDeleteAdminQuote,
                    )
                }

                composable(AppDestination.Approvals.route) {
                    ApprovalsScreen(
                        approvals = state.pendingApprovals,
                        isActionLoading = state.isApprovalActionLoading,
                        onApprove = onApproveLoan,
                        onReject = onRejectLoan,
                        onOpenLoan = { loanId ->
                            navController.navigate(AppRoutes.adminLoanDetail(loanId))
                        },
                    )
                }

                composable(AppDestination.Reports.route) {
                    AdminReportsScreen(
                        summary = state.reportSummary,
                        collectors = state.collectorPerformance,
                    )
                }

                composable(AppDestination.Expenses.route) {
                    ExpensesScreen(
                        expenses = state.expenses,
                        categories = state.expenseCategories,
                        isSaving = state.isExpenseSaving,
                        onCreateExpense = onCreateExpense,
                    )
                }

                composable(AppDestination.Cash.route) {
                    CashScreen(
                        summary = state.cashSummary,
                        movements = state.cashMovements,
                        canCreateMovement = state.canManagePortfolio,
                        isSavingMovement = state.isMovementSaving,
                        onCreateMovement = if (state.canManagePortfolio) onStoreAdminCashMovement else null,
                    )
                }

                composable(AppRoutes.AdminClientDetail) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId")?.toLongOrNull()

                    LaunchedEffect(clientId) {
                        if (clientId != null) {
                            onLoadAdminClientDetail(clientId)
                        }
                    }

                    ClientDetailScreen(
                        detail = state.selectedClientDetail?.takeIf { it.summary.id == clientId },
                        isLoading = state.isDetailLoading,
                        fallbackClient = state.adminClients.firstOrNull { it.id == clientId },
                        onOpenLoan = { loanId ->
                            navController.navigate(AppRoutes.adminLoanDetail(loanId))
                        },
                        onOpenInstallment = { },
                        onEdit = if (state.canUpdateClients && clientId != null) {
                            { navController.navigate(AppRoutes.adminClientEdit(clientId)) }
                        } else null,
                        onDelete = if (state.canDeleteClients && clientId != null) {
                            { onDeleteAdminClient(clientId) { navController.popBackStack() } }
                        } else null,
                        isDeletingClient = state.isClientSaving,
                    )
                }

                composable(AppRoutes.AdminClientEdit) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId")?.toLongOrNull()
                    val detail = state.selectedClientDetail?.takeIf { it.summary.id == clientId }

                    LaunchedEffect(state.isClientSaving, state.successMessage) {
                        if (!state.isClientSaving && state.successMessage != null && detail != null) {
                            navController.popBackStack()
                        }
                    }

                    if (detail == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.CircularProgressIndicator()
                        }
                    } else {
                        ClientEditScreen(
                            detail = detail,
                            isSaving = state.isClientSaving,
                            onSubmit = { input -> if (clientId != null) onUpdateAdminClient(clientId, input) },
                        )
                    }
                }

                composable(AppRoutes.AdminLoanDetail) { backStackEntry ->
                    val loanId = backStackEntry.arguments?.getString("loanId")?.toLongOrNull()

                    LaunchedEffect(loanId) {
                        if (loanId != null) {
                            onLoadAdminLoanDetail(loanId)
                        }
                    }

                    LoanDetailScreen(
                        detail = state.selectedLoanDetail?.takeIf { it.summary.id == loanId },
                        isLoading = state.isDetailLoading,
                        fallbackLoan = state.adminLoans.firstOrNull { it.id == loanId },
                        onOpenInstallment = { },
                        // FAB "Registrar pago": solo para back-office con permiso de cobro.
                        onRegisterPayment = if (state.canRegisterAdminPayment) onRegisterAdminPayment else null,
                        isPaymentLoading = state.isLoading,
                        onGenerateDocument = if (state.canGenerateDocuments) onGenerateLoanDocument else null,
                        isDocumentGenerating = state.isDocumentGenerating,
                        onEditLoan = if (state.canEditLoan && loanId != null) {
                            { navController.navigate(AppRoutes.adminLoanEdit(loanId)) }
                        } else {
                            null
                        },
                        onDeleteLoan = if (state.canDeleteLoan && loanId != null) {
                            { onDeleteAdminLoan(loanId) { navController.popBackStack() } }
                        } else null,
                        isDeletingLoan = state.isLoanSaving,
                    )
                }

                composable(AppRoutes.AdminLoanEdit) { backStackEntry ->
                    val loanId = backStackEntry.arguments?.getString("loanId")?.toLongOrNull()
                    val detail = state.selectedLoanDetail?.takeIf { it.summary.id == loanId }

                    // Navigate back after a successful update
                    androidx.compose.runtime.LaunchedEffect(state.selectedLoanDetail?.summary?.id, state.isLoanUpdating) {
                        if (!state.isLoanUpdating && detail != null && state.successMessage != null) {
                            navController.popBackStack()
                        }
                    }

                    if (detail == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.CircularProgressIndicator()
                        }
                    } else {
                        LoanEditScreen(
                            detail = detail,
                            collectors = state.adminCollectors,
                            isSaving = state.isLoanUpdating,
                            onLoadCollectors = onLoadAdminCollectors,
                            onSubmit = { input ->
                                if (loanId != null) onUpdateAdminLoan(loanId, input)
                            },
                        )
                    }
                }

                composable(AppDestination.CollectorsAdmin.route) {
                    LaunchedEffect(Unit) { onLoadAdminCollectors() }

                    CollectorsScreen(
                        collectors = state.adminCollectors,
                        isLoading = state.isDetailLoading,
                        onOpenCollector = { collectorId ->
                            navController.navigate(AppRoutes.adminCollectorDetail(collectorId))
                        },
                        onCreateCollector = { navController.navigate(AppRoutes.AdminCollectorCreate) },
                    )
                }

                composable(AppRoutes.AdminCollectorCreate) {
                    LaunchedEffect(state.lastCreatedCollectorId) {
                        if (state.lastCreatedCollectorId != null) {
                            val id = state.lastCreatedCollectorId
                            onClearCreationMarkers()
                            navController.popBackStack()
                            navController.navigate(AppRoutes.adminCollectorDetail(id))
                        }
                    }

                    CollectorFormScreen(
                        isSaving = state.isCollectorSaving,
                        onCreateCollector = onCreateAdminCollector,
                    )
                }

                composable(AppRoutes.AdminCollectorDetail) { backStackEntry ->
                    val collectorId = backStackEntry.arguments?.getString("collectorId")?.toLongOrNull()

                    LaunchedEffect(collectorId) {
                        if (collectorId != null) onLoadAdminCollectorDetail(collectorId)
                    }

                    CollectorDetailScreen(
                        detail = state.selectedCollectorDetail?.takeIf { it.id == collectorId },
                        isLoading = state.isDetailLoading,
                        isPayingCommission = state.isCollectorSaving,
                        onEdit = if (collectorId != null) {
                            { navController.navigate(AppRoutes.adminCollectorEdit(collectorId)) }
                        } else { {} },
                        onPayCommission = { commissionId ->
                            if (collectorId != null) onPayCollectorCommission(collectorId, commissionId)
                        },
                    )
                }

                composable(AppRoutes.AdminCollectorEdit) { backStackEntry ->
                    val collectorId = backStackEntry.arguments?.getString("collectorId")?.toLongOrNull()
                    val detail = state.selectedCollectorDetail?.takeIf { it.id == collectorId }

                    LaunchedEffect(state.isCollectorSaving, state.successMessage) {
                        if (!state.isCollectorSaving && state.successMessage != null && detail != null) {
                            navController.popBackStack()
                        }
                    }

                    if (detail == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.CircularProgressIndicator()
                        }
                    } else {
                        CollectorFormScreen(
                            existing = detail,
                            isSaving = state.isCollectorSaving,
                            onUpdateCollector = { input -> if (collectorId != null) onUpdateAdminCollector(collectorId, input) },
                        )
                    }
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
                        onGenerateDocument = if (state.canGenerateDocuments) onGenerateLoanDocument else null,
                        isDocumentGenerating = state.isDocumentGenerating,
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
                        canCancelPayment = state.canCancelPayments,
                        isCancelling = state.isPaymentCancelling,
                        onCancelPayment = if (state.canCancelPayments) { paymentId, reason ->
                            onCancelPayment(paymentId, reason) { }
                        } else null,
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
                // Solo la pestaña activa muestra su etiqueta (a tamaño normal):
                // con 6 destinos en pantallas angostas, mostrar todas las
                // etiquetas obligaba a encogerlas y se distorsionaba la barra.
                alwaysShowLabel = false,
                icon = {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SecondaryContainer)
                                .padding(horizontal = 14.dp, vertical = 4.dp),
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
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) PrimaryContainer else NavUnselected,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
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
            AppRoutes.AdminClientDetail,
            AppRoutes.AdminClientCreate,
            AppRoutes.AdminClientEdit -> AppDestination.ClientsAdmin
            AppRoutes.AdminLoanDetail,
            AppRoutes.AdminLoanCreate,
            AppRoutes.AdminLoanEdit,
            AppRoutes.AdminQuotes,
            AppRoutes.AdminQuoteCreate,
            AppRoutes.AdminQuoteDetail -> AppDestination.LoansAdmin
            AppRoutes.AdminCollectors,
            AppRoutes.AdminCollectorCreate,
            AppRoutes.AdminCollectorDetail,
            AppRoutes.AdminCollectorEdit -> AppDestination.CollectorsAdmin
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
        AppRoutes.ClientDetail, AppRoutes.AdminClientDetail -> "Detalle del Cliente"
        AppRoutes.LoanDetail, AppRoutes.AdminLoanDetail -> "Detalle del Préstamo"
        AppRoutes.InstallmentDetail -> "Detalle de la Cuota"
        AppRoutes.AdminClientCreate -> "Nuevo Cliente"
        AppRoutes.AdminClientEdit -> "Editar Cliente"
        AppRoutes.AdminLoanCreate -> "Nuevo Préstamo"
        AppRoutes.AdminLoanEdit -> "Editar Préstamo"
        AppRoutes.AdminCollectorCreate -> "Nuevo Cobrador"
        AppRoutes.AdminCollectorDetail -> "Detalle del Cobrador"
        AppRoutes.AdminCollectorEdit -> "Editar Cobrador"
        AppRoutes.AdminQuotes -> "Cotizaciones"
        AppRoutes.AdminQuoteCreate -> "Nueva Cotización"
        AppRoutes.AdminQuoteDetail -> "Detalle de Cotización"
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
