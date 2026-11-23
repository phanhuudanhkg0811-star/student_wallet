package com.example.student_wallet.feature.security.di

import com.example.student_wallet.feature.security.SecurityViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val securityModule = module {
    viewModelOf(::SecurityViewModel)
}
