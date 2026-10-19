package com.quangthanh.xiangqi.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quangthanh.xiangqi.application.port.Account
import com.quangthanh.xiangqi.application.port.AccountService
import com.quangthanh.xiangqi.application.usecase.SignInWithGoogleUseCase
import com.quangthanh.xiangqi.application.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Kết quả thao tác gần nhất, hiện thành một dòng chữ trong hộp thoại Cài đặt. */
sealed interface AccountNotice {
    data class SignedIn(val restoredGames: Int) : AccountNotice
    data object SignInFailed : AccountNotice
    data object SignedOut : AccountNotice
}

data class AccountUiState(
    val account: Account? = null,
    val isBusy: Boolean = false,
    val notice: AccountNotice? = null,
)

class AccountViewModel(
    accountService: AccountService,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val signOut: SignOutUseCase,
) : ViewModel() {

    private val progress = MutableStateFlow(AccountUiState())

    val state: StateFlow<AccountUiState> = combine(accountService.account, progress) { account, progress ->
        progress.copy(account = account)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AccountUiState())

    /** [idToken] do màn hình chọn tài khoản Google trả về. */
    fun onGoogleIdToken(idToken: String) {
        progress.update { it.copy(isBusy = true, notice = null) }
        viewModelScope.launch {
            val notice = runCatching { signInWithGoogle(idToken) }
                .fold({ AccountNotice.SignedIn(it.restoredGames) }, { AccountNotice.SignInFailed })
            progress.update { it.copy(isBusy = false, notice = notice) }
        }
    }

    /** Người dùng đóng màn hình chọn tài khoản thì không báo lỗi; chỉ lỗi thật mới gọi hàm này. */
    fun onGoogleSignInError() = progress.update { it.copy(isBusy = false, notice = AccountNotice.SignInFailed) }

    fun signOut() {
        progress.update { it.copy(isBusy = true, notice = null) }
        viewModelScope.launch {
            runCatching { signOut.invoke() }
            progress.update { it.copy(isBusy = false, notice = AccountNotice.SignedOut) }
        }
    }

    fun clearNotice() = progress.update { it.copy(notice = null) }
}
