package com.example.student_wallet.feature.security.di

import android.os.SystemClock
import com.example.student_wallet.feature.security.SecurityViewModel
import com.example.student_wallet.feature.security.audit.AuditJournalViewModel
import com.example.student_wallet.feature.security.crypto.KeystoreSecretProtector
import com.example.student_wallet.feature.security.crypto.PinHasher
import com.example.student_wallet.feature.security.crypto.SecretProtector
import com.example.student_wallet.feature.security.data.DataStoreSecurityLockStore
import com.example.student_wallet.feature.security.data.SecurityLockStore
import com.example.student_wallet.feature.security.lock.AppLockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val securityModule = module {
    single { PinHasher() }
    single<SecretProtector> { KeystoreSecretProtector() }
    single<SecurityLockStore> { DataStoreSecurityLockStore(context = androidContext()) }
    single {
        AppLockManager(
            lockStore = get(),
            pinHasher = get(),
            secretProtector = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            elapsedRealtimeMillis = { SystemClock.elapsedRealtime() },
        )
    }

    viewModelOf(::SecurityViewModel)
    viewModel {
        AuditJournalViewModel(
            transactionRepository = get(),
            settingsRepository = get(),
        )
    }
}
