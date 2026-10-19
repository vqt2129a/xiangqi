package com.quangthanh.xiangqi.infrastructure.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.quangthanh.xiangqi.application.port.Account
import com.quangthanh.xiangqi.application.port.AccountService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAccountService : AccountService {

    private val auth get() = FirebaseSession.auth

    override val account: Flow<Account?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAccount()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Khách đăng nhập Google: "nâng cấp" tài khoản khách (link), giữ nguyên uid và dữ liệu đã sao lưu.
     * Nếu tài khoản Google đó đã gắn với uid khác (từng dùng ở máy khác) thì chuyển hẳn sang uid đó;
     * lịch sử sau đó được hợp nhất bởi SyncCloudGamesUseCase.
     */
    override suspend fun signInWithGoogle(idToken: String): Account {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val current = auth.currentUser
        val user = if (current != null && current.isAnonymous) {
            try {
                current.linkWithCredential(credential).await().user
            } catch (e: FirebaseAuthUserCollisionException) {
                auth.signInWithCredential(credential).await().user
            }
        } else {
            auth.signInWithCredential(credential).await().user
        }
        return requireNotNull(user).toAccount()
    }

    override suspend fun signOut() {
        FirebaseSession.startGuestSession()
    }

    private fun FirebaseUser.toAccount(): Account {
        // Sau khi link, tên và email nằm trong thông tin của nhà cung cấp Google.
        val google = providerData.firstOrNull { it.providerId == GoogleAuthProvider.PROVIDER_ID }
        return Account(
            uid = uid,
            isGuest = isAnonymous,
            displayName = google?.displayName ?: displayName,
            email = google?.email ?: email,
        )
    }
}
