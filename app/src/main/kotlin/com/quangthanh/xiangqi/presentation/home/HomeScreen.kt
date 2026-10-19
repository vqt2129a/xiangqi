package com.quangthanh.xiangqi.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quangthanh.xiangqi.R
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.presentation.common.ClassicButton
import com.quangthanh.xiangqi.presentation.common.ClassicButtonColors
import com.quangthanh.xiangqi.presentation.common.ClassicDialog
import com.quangthanh.xiangqi.presentation.common.DialogMessage
import com.quangthanh.xiangqi.presentation.DeepLinkAction
import com.quangthanh.xiangqi.presentation.common.RequestNotificationPermission
import com.quangthanh.xiangqi.presentation.common.modeText
import com.quangthanh.xiangqi.presentation.account.AccountViewModel
import com.quangthanh.xiangqi.presentation.settings.SettingsDialogHost
import com.quangthanh.xiangqi.presentation.settings.SettingsViewModel

private enum class HomeDialog { VS_COMPUTER, SETTINGS }

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel,
    accountViewModel: AccountViewModel,
    pendingAction: String?,
    onActionHandled: () -> Unit,
    onOpenGame: (GameId) -> Unit,
    onOpenHistory: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(pendingAction) {
        if (pendingAction == DeepLinkAction.RESUME_GAME) viewModel.resumeLatestGame()
        if (pendingAction != null) onActionHandled()
    }
    RequestNotificationPermission()
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel) { viewModel.gameToOpen.collect(onOpenGame) }
    var dialog by rememberSaveable { mutableStateOf<HomeDialog?>(null) }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_home),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Lớp tối mờ dần ở nửa dưới để chữ trên nút luôn đọc rõ, bất kể ảnh nền phía sau.
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6120A05)))),
        )
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.app_name).uppercase(),
                color = Color(0xFFFFD27A),
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                style = TextStyle(shadow = Shadow(Color(0xCC000000), Offset(0f, 4f), blurRadius = 12f)),
                modifier = Modifier.padding(top = 24.dp),
            )
            // Bàn cờ trong ảnh nền nằm ở giữa màn hình; menu đặt ngay dưới để không che nó.
            Spacer(Modifier.weight(1f))
            val menuWidth = Modifier.widthIn(max = 320.dp).fillMaxWidth()
            val continueGame = state.unfinishedGame != null
            if (continueGame) {
                ClassicButton(
                    stringResource(R.string.home_continue), viewModel::continueGame, menuWidth,
                    colors = ClassicButtonColors.GoldFilled, fontSize = 20.sp,
                )
            }
            ClassicButton(
                stringResource(R.string.home_vs_computer), { dialog = HomeDialog.VS_COMPUTER }, menuWidth,
                colors = if (continueGame) ClassicButtonColors.Gold else ClassicButtonColors.GoldFilled,
                fontSize = if (continueGame) 18.sp else 20.sp,
            )
            ClassicButton(stringResource(R.string.home_two_players), viewModel::startTwoPlayers, menuWidth, colors = ClassicButtonColors.Gold)
            Row(menuWidth, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ClassicButton(
                    stringResource(R.string.home_history_short), onOpenHistory, Modifier.weight(1f),
                    colors = ClassicButtonColors.Gold, fontSize = 16.sp,
                )
                ClassicButton(
                    stringResource(R.string.home_settings), { dialog = HomeDialog.SETTINGS }, Modifier.weight(1f),
                    colors = ClassicButtonColors.Gold, fontSize = 16.sp,
                )
            }
        }
    }

    when (dialog) {
        HomeDialog.VS_COMPUTER -> ClassicDialog(
            title = stringResource(R.string.home_choose_level),
            confirmText = stringResource(R.string.home_start),
            onConfirm = { dialog = null; viewModel.startAgainstComputer() },
            dismissText = stringResource(R.string.cancel),
            onDismiss = { dialog = null },
        ) {
            NewGameOptions(state.difficulty, viewModel::selectDifficulty, state.humanSide, viewModel::selectSide)
        }
        HomeDialog.SETTINGS -> SettingsDialogHost(settingsViewModel, accountViewModel, onDismiss = { dialog = null })
        null -> Unit
    }

    val unfinished = state.unfinishedGame
    if (state.showResumePrompt && unfinished != null) {
        ClassicDialog(
            title = stringResource(R.string.home_resume_title),
            confirmText = stringResource(R.string.home_continue),
            onConfirm = viewModel::continueGame,
            dismissText = stringResource(R.string.game_new),
            onDismiss = viewModel::skipResume,
        ) {
            DialogMessage(
                "${modeText(unfinished.participants)} · ${stringResource(R.string.move_count, unfinished.moveCount)}\n" +
                    stringResource(R.string.home_resume_message),
            )
        }
    }

    if (state.pendingMode != null) {
        ClassicDialog(
            title = stringResource(R.string.home_abandon_title),
            confirmText = stringResource(R.string.home_abandon_confirm),
            onConfirm = viewModel::confirmStart,
            dismissText = stringResource(R.string.cancel),
            onDismiss = viewModel::cancelStart,
        ) { DialogMessage(stringResource(R.string.home_abandon_message)) }
    }
}
