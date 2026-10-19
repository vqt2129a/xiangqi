package com.quangthanh.xiangqi.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quangthanh.xiangqi.R
import com.quangthanh.xiangqi.presentation.account.AccountNotice
import com.quangthanh.xiangqi.presentation.account.AccountUiState
import com.quangthanh.xiangqi.presentation.settings.AudioUiState

/**
 * Hộp thoại "Cài đặt".
 * - Âm thanh: chọn tạm, bấm "Đồng ý" mới lưu (qua [onSaveAudio]), bấm "Hủy" thì bỏ.
 * - Tài khoản: đăng nhập/đăng xuất có hiệu lực ngay, vì đó là thao tác riêng chứ không phải lựa chọn.
 */
@Composable
fun SettingsDialog(
    audio: AudioUiState,
    onSaveAudio: (musicEnabled: Boolean, effectsEnabled: Boolean) -> Unit,
    account: AccountUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    var music by rememberSaveable { mutableStateOf(audio.musicEnabled) }
    var effects by rememberSaveable { mutableStateOf(audio.effectsEnabled) }
    val onOff = listOf(true to stringResource(R.string.on), false to stringResource(R.string.off))

    ClassicDialog(
        title = stringResource(R.string.settings_title),
        confirmText = stringResource(R.string.ok),
        onConfirm = {
            onSaveAudio(music, effects)
            onDismiss()
        },
        dismissText = stringResource(R.string.cancel),
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DialogLabel(stringResource(R.string.settings_music).uppercase())
            ChoiceChips(onOff, music, { music = it })
            DialogLabel(stringResource(R.string.settings_sound_effects).uppercase())
            ChoiceChips(onOff, effects, { effects = it })
            DialogLabel(stringResource(R.string.account_title).uppercase())
            AccountSection(account, onSignIn, onSignOut)
        }
    }
}

@Composable
private fun AccountSection(state: AccountUiState, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val account = state.account
        when {
            account == null -> Text(stringResource(R.string.account_connecting), color = DialogColors.TextDim)
            account.isGuest -> {
                Text(stringResource(R.string.account_guest), color = DialogColors.Text, fontSize = 15.sp, textAlign = TextAlign.Center)
                ClassicButton(
                    stringResource(R.string.account_sign_in_google), onSignIn, Modifier.fillMaxWidth(),
                    enabled = !state.isBusy, colors = ClassicButtonColors.GoldFilled, fontSize = 16.sp,
                )
            }
            else -> {
                Text(account.displayName ?: account.email.orEmpty(), color = DialogColors.GoldLight, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                account.email?.let { Text(it, color = DialogColors.TextDim, fontSize = 14.sp) }
                ClassicButton(
                    stringResource(R.string.account_sign_out), onSignOut, Modifier.fillMaxWidth(),
                    enabled = !state.isBusy, colors = ClassicButtonColors.Gold, fontSize = 16.sp,
                )
            }
        }
        if (state.isBusy) CircularProgressIndicator(Modifier.size(20.dp), color = DialogColors.Gold, strokeWidth = 2.dp)
        state.notice?.let { notice ->
            val text = when (notice) {
                is AccountNotice.SignedIn ->
                    if (notice.restoredGames > 0) stringResource(R.string.account_signed_in_restored, notice.restoredGames)
                    else stringResource(R.string.account_signed_in)
                AccountNotice.SignInFailed -> stringResource(R.string.account_sign_in_failed)
                AccountNotice.SignedOut -> stringResource(R.string.account_signed_out)
            }
            Text(text, color = DialogColors.Text, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}
