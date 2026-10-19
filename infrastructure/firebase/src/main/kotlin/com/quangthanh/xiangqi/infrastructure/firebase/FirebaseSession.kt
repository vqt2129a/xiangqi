package com.quangthanh.xiangqi.infrastructure.firebase

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/** Tài khoản Firebase hiện tại; chưa có thì tự tạo tài khoản khách (ẩn danh). */
internal object FirebaseSession {

    private val lock = Mutex()
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    suspend fun uid(): String = lock.withLock {
        auth.currentUser?.uid ?: requireNotNull(auth.signInAnonymously().await().user).uid
    }

    /** Dùng khi đăng xuất: chuyển ngay sang tài khoản khách mới, không để khoảng trống. */
    suspend fun startGuestSession(): String = lock.withLock {
        auth.signOut()
        requireNotNull(auth.signInAnonymously().await().user).uid
    }
}
