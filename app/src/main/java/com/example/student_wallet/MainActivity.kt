package com.example.student_wallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.student_wallet.core.domain.model.ThemeMode
import com.example.student_wallet.core.domain.model.UserSettings
import com.example.student_wallet.core.domain.repository.SettingsRepository
import com.example.student_wallet.core.ui.theme.Student_walletTheme
import com.example.student_wallet.feature.budget.BudgetScreen
import com.example.student_wallet.feature.group.GroupScreen
import com.example.student_wallet.feature.scanner.ScannerScreen
import com.example.student_wallet.feature.security.SecurityScreen
import com.example.student_wallet.feature.transaction.TransactionScreen
import com.example.student_wallet.ui.home.HomeScreen
import org.koin.compose.koinInject

private object AppRoutes {
    const val HOME = "home"
    const val TRANSACTION = "transaction"
    const val BUDGET = "budget"
    const val GROUP = "group"
    const val SECURITY = "security"
    const val SCANNER = "scanner"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomNavTabs = listOf(
    BottomNavTab(AppRoutes.HOME, "Trang chủ", Icons.Filled.Home),
    BottomNavTab(AppRoutes.TRANSACTION, "Giao dịch", Icons.AutoMirrored.Filled.ReceiptLong),
    BottomNavTab(AppRoutes.BUDGET, "Ngân sách", Icons.Filled.AccountBalanceWallet),
    BottomNavTab(AppRoutes.GROUP, "Nhóm", Icons.Filled.Groups),
    BottomNavTab(AppRoutes.SECURITY, "Cài đặt", Icons.Filled.Security),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsRepository: SettingsRepository = koinInject()
            val settings by settingsRepository.observeSettings()
                .collectAsStateWithLifecycle(initialValue = UserSettings())

            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            Student_walletTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
            ) {
                StudentWalletMainScreen(settings = settings)
            }
        }
    }
}

@Composable
fun StudentWalletMainScreen(settings: UserSettings) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val showFab = currentDestination?.route in setOf(AppRoutes.HOME, AppRoutes.TRANSACTION)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navigateToTab(AppRoutes.TRANSACTION) },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Thêm giao dịch")
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                bottomNavTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateToTab(tab.route) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppRoutes.HOME) {
                HomeScreen(
                    onOpenScanner = {
                        navController.navigate(AppRoutes.SCANNER) { launchSingleTop = true }
                    },
                    onOpenGroup = { navigateToTab(AppRoutes.GROUP) },
                    onOpenBudget = { navigateToTab(AppRoutes.BUDGET) },
                    onOpenTransaction = { navigateToTab(AppRoutes.TRANSACTION) },
                    currency = settings.currency,
                )
            }
            composable(AppRoutes.TRANSACTION) { TransactionScreen() }
            composable(AppRoutes.BUDGET) { BudgetScreen() }
            composable(AppRoutes.GROUP) { GroupScreen() }
            composable(AppRoutes.SECURITY) { SecurityScreen() }
            composable(AppRoutes.SCANNER) { ScannerScreen() }
        }
    }
}
