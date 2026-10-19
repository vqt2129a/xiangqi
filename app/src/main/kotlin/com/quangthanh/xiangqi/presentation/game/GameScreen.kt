package com.quangthanh.xiangqi.presentation.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quangthanh.xiangqi.R
import com.quangthanh.xiangqi.application.dto.GameState
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.GameStatus
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.presentation.common.ClassicButton
import com.quangthanh.xiangqi.presentation.common.ClassicDialog
import com.quangthanh.xiangqi.presentation.common.DialogMessage
import com.quangthanh.xiangqi.presentation.common.modeText
import com.quangthanh.xiangqi.presentation.common.resultTitle
import com.quangthanh.xiangqi.presentation.common.woodBackground
import com.quangthanh.xiangqi.presentation.account.AccountViewModel
import com.quangthanh.xiangqi.presentation.settings.SettingsDialogHost
import com.quangthanh.xiangqi.presentation.settings.SettingsViewModel

/** Hộp thoại chỉ phục vụ hiển thị (hỏi lại, cài đặt) nên giữ trong View; quyết định nghiệp vụ nằm ở ViewModel. */
private enum class GameDialog { RESIGN, SETTINGS }

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    settingsViewModel: SettingsViewModel,
    accountViewModel: AccountViewModel,
    onOpenGame: (GameId) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val messageTexts = mapOf(
        GameMessage.CHECK to stringResource(R.string.game_message_check),
        GameMessage.ACTION_REJECTED to stringResource(R.string.game_message_rejected),
        GameMessage.LOAD_FAILED to stringResource(R.string.game_message_load_failed),
    )
    var checkBanner by remember { mutableIntStateOf(0) }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            if (message == GameMessage.CHECK) checkBanner++ else snackbarHostState.showSnackbar(messageTexts.getValue(message))
        }
    }
    LaunchedEffect(viewModel) { viewModel.gameToOpen.collect(onOpenGame) }
    var dialog by rememberSaveable { mutableStateOf<GameDialog?>(null) }

    Box(Modifier.fillMaxSize().woodBackground()) {
        val game = state.game
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.back),
                        tint = Color(0xFFFFE9C4),
                    )
                }
                game?.let { StatusPlate(it, state.isComputerThinking, Modifier.align(Alignment.Center)) }
            }
            game?.let {
                Text(modeText(it.participants), color = Color(0xFFFFE9C4), fontSize = 15.sp)
            }
            Spacer(Modifier.height(8.dp))

            if (game == null) {
                CircularProgressIndicator(Modifier.padding(32.dp), color = Color.White)
            } else {
                Box(contentAlignment = Alignment.Center) {
                    ChessBoard(
                        pieces = game.pieces,
                        selected = state.selected,
                        targets = state.targets,
                        lastMove = game.lastMove,
                        lastMoveSide = state.lastMoveSide,
                        generalInCheck = game.generalInCheck,
                        flipped = state.flipped,
                        onSquareTapped = viewModel::onSquareTapped,
                        modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    )
                    CheckBanner(checkBanner)
                }
            }

            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val inProgress = game?.status == GameStatus.InProgress
                ClassicButton(stringResource(R.string.game_new), viewModel::requestNewGame, Modifier.weight(1f), enabled = game != null)
                ClassicButton(stringResource(R.string.game_undo), viewModel::undo, Modifier.weight(1f), enabled = game?.canUndo == true)
                ClassicButton(stringResource(R.string.game_resign), { dialog = GameDialog.RESIGN }, Modifier.weight(1f), enabled = inProgress)
                ClassicButton(stringResource(R.string.game_settings), { dialog = GameDialog.SETTINGS }, Modifier.weight(1f))
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }

    if (state.confirmNewGame) {
        ClassicDialog(
            title = stringResource(R.string.home_abandon_title),
            confirmText = stringResource(R.string.home_abandon_confirm),
            onConfirm = viewModel::confirmNewGame,
            dismissText = stringResource(R.string.cancel),
            onDismiss = viewModel::cancelNewGame,
        ) { DialogMessage(stringResource(R.string.home_abandon_message)) }
    }

    when (dialog) {
        GameDialog.RESIGN -> ClassicDialog(
            title = stringResource(R.string.game_resign_title),
            confirmText = stringResource(R.string.game_resign),
            onConfirm = { dialog = null; viewModel.resign() },
            dismissText = stringResource(R.string.cancel),
            onDismiss = { dialog = null },
        ) { DialogMessage(stringResource(R.string.game_resign_message)) }
        GameDialog.SETTINGS -> SettingsDialogHost(settingsViewModel, accountViewModel, onDismiss = { dialog = null })
        null -> Unit
    }

    val finished = state.game?.status as? GameStatus.Finished
    if (state.showResult && finished != null) {
        ResultOverlay(
            result = finished.result,
            participants = state.game!!.participants,
            onNewGame = viewModel::requestNewGame,
            onViewBoard = viewModel::dismissResult,
        )
    }
}

/** Bảng báo lượt kiểu bản gốc: nền vàng gỗ, chữ đỏ khi tới lượt Đỏ, chữ đen khi tới lượt Đen. */
@Composable
private fun StatusPlate(game: GameState, isComputerThinking: Boolean, modifier: Modifier = Modifier) {
    val status = game.status
    val text = when {
        status is GameStatus.Finished -> resultTitle(status.result, game.participants)
        isComputerThinking -> stringResource(R.string.game_computer_thinking)
        game.participants.humanSideAgainstComputer == game.sideToMove -> stringResource(R.string.game_your_turn)
        game.sideToMove == Side.RED -> stringResource(R.string.game_turn_red)
        else -> stringResource(R.string.game_turn_black)
    }
    val color = if (game.sideToMove == Side.RED && status == GameStatus.InProgress) Color(0xFFE5191C) else Color(0xFF1A1A1A)
    Row(
        modifier = modifier
            .background(Color(0xFFDEB887))
            .padding(horizontal = 28.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isComputerThinking) {
            CircularProgressIndicator(Modifier.size(18.dp), color = color, strokeWidth = 2.dp)
            Spacer(Modifier.size(10.dp))
        }
        Text(text, color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}
