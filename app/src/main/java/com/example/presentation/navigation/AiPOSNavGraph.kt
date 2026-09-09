package com.example.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.AiPOSApplication
import com.example.data.local.prefs.SessionManager
import com.example.domain.model.SampleProductCatalog
import com.example.domain.repository.ProductRepository
import com.example.domain.usecase.AuthenticateUserUseCase
import com.example.domain.usecase.ResetDataUseCase
import com.example.domain.usecase.SeedDefaultAdminUseCase
import com.example.domain.backup.DatabaseBackupManager
import com.example.presentation.backup.BackupRestoreScreen
import com.example.presentation.backup.BackupViewModel
import com.example.presentation.backup.BackupViewModelFactory
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.login.LoginScreen
import com.example.presentation.login.LoginViewModelFactory
import com.example.presentation.more.MoreMenuScreen
import com.example.presentation.pos.PosScreen
import com.example.presentation.cart.ShoppingCartViewModel
import com.example.presentation.cart.ShoppingCartViewModelFactory
import com.example.presentation.products.ProductListScreen
import com.example.presentation.products.ProductViewModel
import com.example.presentation.products.ProductViewModelFactory
import com.example.presentation.stocks.StocksScreen
import com.example.presentation.stocks.StocksViewModel
import com.example.presentation.stocks.StocksViewModelFactory
import com.example.presentation.utang.UtangListScreen
import com.example.domain.auth.StaffSessionManager
import com.example.presentation.auth.PinLockoutOverlay
import com.example.presentation.staff.StaffManagementScreen
import kotlinx.coroutines.launch

import com.example.domain.repository.TransactionRepository
import com.example.presentation.sales.DailySalesSummaryScreen
import com.example.presentation.sales.DailySalesViewModel
import com.example.presentation.sales.DailySalesViewModelFactory

@Composable
fun AiPOSNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    val context = LocalContext.current
    val app = context.applicationContext as AiPOSApplication
    val userDao = app.database.userDao()
    val productDao = app.database.productDao()
    val transactionLogDao = app.database.transactionLogDao()
    val productRepository = remember { ProductRepository(productDao) }
    val transactionRepository = remember { TransactionRepository(transactionLogDao) }
    val coroutineScope = rememberCoroutineScope()
    
    val sessionManager = SessionManager(context)
    val authUseCase = AuthenticateUserUseCase(userDao)
    val seedUseCase = SeedDefaultAdminUseCase(userDao)
    val resetDataUseCase = remember { ResetDataUseCase(userDao, productRepository, transactionRepository) }
    val staffSessionManager = remember { StaffSessionManager(userDao, sessionManager) }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
        composable(Screen.Login.route) {
            val loginViewModel: com.example.presentation.login.LoginViewModel = viewModel(
                factory = LoginViewModelFactory(authUseCase, seedUseCase, sessionManager)
            )
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.StoreSetup.route) {
            PlaceholderScreen("Store Setup Screen")
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToPOS = { navController.navigate(Screen.POS.route) },
                onNavigateToInventory = { navController.navigate(Screen.Inventory.route) },
                onNavigateToMore = { navController.navigate(Screen.More.route) },
                onNavigateToSales = { navController.navigate(Screen.SalesHistory.route) }
            )
        }
        composable(Screen.POS.route) {
            val dbProducts by productRepository.allProducts.collectAsState(initial = emptyList())
            PosScreen(
                onNavigateBack = { navController.popBackStack() },
                products = if (dbProducts.isNotEmpty()) dbProducts else SampleProductCatalog.items,
                staffSessionManager = staffSessionManager,
                onCheckoutSuccess = { soldItems, receipt ->
                    coroutineScope.launch {
                        productRepository.deductStockForSale(soldItems)
                        transactionRepository.recordTransaction(receipt)
                    }
                }
            )
        }
        composable(Screen.Inventory.route) {
            val productViewModel: ProductViewModel = viewModel(
                factory = ProductViewModelFactory(productRepository)
            )
            val cartViewModel: ShoppingCartViewModel = viewModel(
                factory = ShoppingCartViewModelFactory(productRepository, transactionRepository)
            )
            ProductListScreen(
                viewModel = productViewModel,
                cartViewModel = cartViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPos = { navController.navigate(Screen.POS.route) }
            )
        }
        composable(Screen.SalesHistory.route) {
            val dailySalesViewModel: DailySalesViewModel = viewModel(
                factory = DailySalesViewModelFactory(transactionRepository, resetDataUseCase)
            )
            DailySalesSummaryScreen(
                viewModel = dailySalesViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPos = { navController.navigate(Screen.POS.route) }
            )
        }
        composable(Screen.More.route) {
            MoreMenuScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUtang = { navController.navigate(Screen.UtangList.route) },
                onNavigateToBackup = { navController.navigate(Screen.BackupRestore.route) },
                onNavigateToInventory = { navController.navigate(Screen.Inventory.route) },
                onNavigateToPOS = { navController.navigate(Screen.POS.route) },
                onNavigateToSales = { navController.navigate(Screen.SalesHistory.route) },
                onNavigateToStaff = { navController.navigate(Screen.StaffManagement.route) },
                onPerformReset = { scope, reseedSampleCatalog, password, onResult ->
                    coroutineScope.launch {
                        val isValid = resetDataUseCase.verifyAdminPassword(password)
                        if (!isValid) {
                            onResult(false, "Incorrect admin password. Action aborted.")
                            return@launch
                        }
                        try {
                            when (scope) {
                                com.example.presentation.common.ResetScope.ALL_PRODUCTS_AND_SALES -> {
                                    resetDataUseCase.resetAll(reseedSampleCatalog)
                                }
                                com.example.presentation.common.ResetScope.PRODUCTS_ONLY -> {
                                    resetDataUseCase.resetProducts(reseedSampleCatalog)
                                }
                                com.example.presentation.common.ResetScope.SALES_AND_TRANSACTIONS_ONLY -> {
                                    resetDataUseCase.resetSalesAndTransactions()
                                }
                            }
                            onResult(true, null)
                        } catch (e: Exception) {
                            onResult(false, e.localizedMessage ?: "Failed to reset records.")
                        }
                    }
                }
            )
        }
        composable(Screen.BackupRestore.route) {
            val backupManager = remember { DatabaseBackupManager(context, app.database) }
            val backupViewModel: BackupViewModel = viewModel(
                factory = BackupViewModelFactory(backupManager, resetDataUseCase)
            )
            BackupRestoreScreen(
                viewModel = backupViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.UtangList.route) {
            UtangListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { customerId ->
                    navController.navigate(Screen.CustomerDetail.createRoute(customerId))
                }
            )
        }
        composable(Screen.CustomerDetail.route) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            com.example.presentation.utang.CustomerDetailScreen(
                customerId = customerId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPayment = { id -> 
                    navController.navigate(Screen.RecordPayment.createRoute(id))
                }
            )
        }
        composable(Screen.RecordPayment.route) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            com.example.presentation.utang.RecordUtangPaymentScreen(
                customerId = customerId,
                onNavigateBack = { navController.popBackStack() },
                onPaymentComplete = { navController.popBackStack() }
            )
        }
        composable(Screen.StaffManagement.route) {
            StaffManagementScreen(
                staffSessionManager = staffSessionManager,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    // Automatic Inactivity Lockout overlay (requires 4-digit PIN to unlock)
    PinLockoutOverlay(staffSessionManager = staffSessionManager)
}
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title)
    }
}
