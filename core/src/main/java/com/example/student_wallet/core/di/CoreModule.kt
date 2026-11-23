package com.example.student_wallet.core.di

import androidx.room.Room
import com.example.student_wallet.core.data.datastore.DataStoreSettingsRepository
import com.example.student_wallet.core.data.local.AppDatabase
import com.example.student_wallet.core.data.local.DatabaseSeedCallback
import com.example.student_wallet.core.data.repository.OfflineTransactionRepository
import com.example.student_wallet.core.domain.repository.SettingsRepository
import com.example.student_wallet.core.domain.repository.TransactionRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "student_wallet.db",
        )
            .addCallback(DatabaseSeedCallback())
            .build()
    }

    single { get<AppDatabase>().transactionDao() }
    single { get<AppDatabase>().categoryDao() }

    single<TransactionRepository> {
        OfflineTransactionRepository(
            transactionDao = get(),
            categoryDao = get(),
        )
    }

    single<SettingsRepository> {
        DataStoreSettingsRepository(context = androidContext())
    }
}
