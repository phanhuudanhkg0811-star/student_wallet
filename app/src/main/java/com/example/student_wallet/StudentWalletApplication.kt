package com.example.student_wallet

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.student_wallet.core.di.coreModule
import com.example.student_wallet.feature.security.di.securityModule
import com.example.student_wallet.feature.security.lock.AppLockManager
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class StudentWalletApplication : Application() {

    private val appLockManager: AppLockManager by inject()
    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@StudentWalletApplication)
            modules(coreModule, securityModule)
        }

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    if (startedActivities == 0) {
                        appLockManager.onAppForegrounded()
                    }
                    startedActivities++
                }

                override fun onActivityStopped(activity: Activity) {
                    startedActivities = (startedActivities - 1).coerceAtLeast(0)
                    if (startedActivities == 0 && !activity.isChangingConfigurations) {
                        appLockManager.onAppBackgrounded()
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }
}
