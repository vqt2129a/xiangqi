package com.quangthanh.xiangqi.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quangthanh.xiangqi.presentation.account.AccountViewModel
import com.quangthanh.xiangqi.presentation.account.rememberGoogleSignIn
import com.quangthanh.xiangqi.presentation.common.SettingsDialog

/** Nối hộp thoại Cài đặt với ViewModel âm thanh và tài khoản; dùng chung cho màn hình chính và màn hình chơi. */
@Composable
fun SettingsDialogHost(settingsViewModel: SettingsViewModel, accountViewModel: AccountViewModel, onDismiss: () -> Unit) {
    val audio by settingsViewModel.audioState.collectAsStateWithLifecycle()
    val account by accountViewModel.state.collectAsStateWithLifecycle()
    val signInWithGoogle = rememberGoogleSignIn(
        onIdToken = accountViewModel::onGoogleIdToken,
        onError = accountViewModel::onGoogleSignInError,
    )
    SettingsDialog(
        audio = audio,
        onSaveAudio = settingsViewModel::saveAudio,
        account = account,
        onSignIn = signInWithGoogle,
        onSignOut = accountViewModel::signOut,
        onDismiss = {
            accountViewModel.clearNotice()
            onDismiss()
        },
    )
}
