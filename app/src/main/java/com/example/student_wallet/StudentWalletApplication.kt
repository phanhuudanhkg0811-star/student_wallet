package com.example.student_wallet

import android.app.Application
import com.example.student_wallet.core.di.coreModule
import com.example.student_wallet.feature.security.di.securityModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class StudentWalletApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@StudentWalletApplication)
            modules(coreModule, securityModule)
        }
    }
}
