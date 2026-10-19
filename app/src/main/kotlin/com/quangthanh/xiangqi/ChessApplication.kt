package com.quangthanh.xiangqi

import android.app.Application
import com.quangthanh.xiangqi.di.AppContainer
import com.quangthanh.xiangqi.infrastructure.firebase.FirebaseSetup

class ChessApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        FirebaseSetup.init(this, BuildConfig.VERSION_NAME)
    }
}
