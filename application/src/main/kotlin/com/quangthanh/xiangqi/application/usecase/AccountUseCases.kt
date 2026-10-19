package com.quangthanh.xiangqi.application.usecase

import com.quangthanh.xiangqi.application.port.Account
import com.quangthanh.xiangqi.application.port.AccountService

data class SignInResult(val account: Account, val restoredGames: Int)

/**
 * Đăng nhập Google rồi hợp nhất lịch sử: khách lên tài khoản Google thì giữ nguyên dữ liệu;
 * nếu tài khoản Google đó đã dùng ở máy khác thì các ván ở đó được khôi phục về máy này.
 */
class SignInWithGoogleUseCase(
    private val accounts: AccountService,
    private val syncCloudGames: SyncCloudGamesUseCase,
) {
    suspend operator fun invoke(idToken: String): SignInResult {
        val account = accounts.signInWithGoogle(idToken)
        val sync = syncCloudGames()
        return SignInResult(account, sync.restored)
    }
}

/**
 * Đăng xuất: đẩy nốt các ván chưa sao lưu lên tài khoản hiện tại trước, để không mất lịch sử,
 * rồi mới chuyển về tài khoản khách. Các ván vẫn còn trong máy.
 */
class SignOutUseCase(
    private val accounts: AccountService,
    private val syncCloudGames: SyncCloudGamesUseCase,
) {
    suspend operator fun invoke() {
        runCatching { syncCloudGames() } // mất mạng thì vẫn cho đăng xuất; ván vẫn còn trong máy
        accounts.signOut()
    }
}
