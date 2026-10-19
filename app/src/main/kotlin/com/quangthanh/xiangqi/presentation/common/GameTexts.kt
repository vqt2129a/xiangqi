package com.quangthanh.xiangqi.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quangthanh.xiangqi.R
import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.DrawReason
import com.quangthanh.xiangqi.domain.model.GameResult
import com.quangthanh.xiangqi.domain.model.Participants
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.domain.model.WinReason

@Composable
fun difficultyText(difficulty: Difficulty): String = stringResource(
    when (difficulty) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
    },
)

@Composable
fun modeText(participants: Participants): String {
    val computer = participants.computer ?: return stringResource(R.string.mode_two_players)
    return stringResource(R.string.mode_vs_computer, difficultyText(computer.difficulty))
}

/** Tiêu đề kết quả: nhìn từ phía người chơi khi đánh với máy, trung lập khi 2 người chơi. */
@Composable
fun resultTitle(result: GameResult, participants: Participants): String {
    val humanSide = participants.humanSideAgainstComputer
    return stringResource(
        when {
            result is GameResult.Draw -> R.string.result_draw
            result !is GameResult.Win -> R.string.result_draw
            humanSide != null -> if (result.winner == humanSide) R.string.result_you_win else R.string.result_you_lose
            result.winner == Side.RED -> R.string.result_red_wins
            else -> R.string.result_black_wins
        },
    )
}

@Composable
fun resultReason(result: GameResult): String = stringResource(
    when (result) {
        is GameResult.Win -> when (result.reason) {
            WinReason.CHECKMATE -> R.string.reason_checkmate
            WinReason.STALEMATE -> R.string.reason_stalemate
            WinReason.RESIGNATION -> R.string.reason_resignation
        }
        is GameResult.Draw -> when (result.reason) {
            DrawReason.REPETITION -> R.string.reason_repetition
            DrawReason.MOVE_LIMIT -> R.string.reason_move_limit
            DrawReason.INSUFFICIENT_MATERIAL -> R.string.reason_insufficient_material
        }
    },
)
