package com.example.student_wallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.student_wallet.core.ui.theme.Student_walletTheme
import com.example.student_wallet.feature.budget.BudgetScreen
import com.example.student_wallet.feature.group.GroupScreen
import com.example.student_wallet.feature.scanner.ScannerScreen
import com.example.student_wallet.feature.security.SecurityScreen
import com.example.student_wallet.feature.transaction.TransactionScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Student_walletTheme {
                StudentWalletMainScreen()
            }
        }
    }
}

@Composable
fun StudentWalletMainScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        "Giao dịch" to Icons.AutoMirrored.Filled.ReceiptLong,
        "Quét QR" to Icons.Filled.QrCodeScanner,
        "Ngân sách" to Icons.Filled.AccountBalanceWallet,
        "Nhóm" to Icons.Filled.Groups,
        "Bảo mật" to Icons.Filled.Security
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            0 -> TransactionScreen(modifier)
            1 -> ScannerScreen(modifier)
            2 -> BudgetScreen(modifier)
            3 -> GroupScreen(modifier)
            4 -> SecurityScreen(modifier)
        }
    }
}
