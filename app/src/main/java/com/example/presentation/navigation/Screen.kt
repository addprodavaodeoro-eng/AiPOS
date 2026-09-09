package com.example.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object StoreSetup : Screen("store_setup")
    object Dashboard : Screen("dashboard")
    object POS : Screen("pos")
    object Inventory : Screen("inventory")
    object SalesHistory : Screen("sales_history")
    object More : Screen("more")
    
    // Utang features
    object UtangList : Screen("utang_list")
    object CustomerDetail : Screen("customer_detail/{customerId}") {
        fun createRoute(customerId: String) = "customer_detail/$customerId"
    }
    object RecordPayment : Screen("record_payment/{customerId}") {
        fun createRoute(customerId: String) = "record_payment/$customerId"
    }

    // Backup & Data Recovery
    object BackupRestore : Screen("backup_restore")

    // Staff & PIN Security
    object StaffManagement : Screen("staff_management")
}
