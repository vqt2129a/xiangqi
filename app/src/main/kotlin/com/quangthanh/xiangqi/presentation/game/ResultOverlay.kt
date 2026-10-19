package com.quangthanh.xiangqi.presentation.game

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quangthanh.xiangqi.R
import com.quangthanh.xiangqi.domain.model.GameResult
import com.quangthanh.xiangqi.domain.model.Participants
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.presentation.common.ClassicButton
import com.quangthanh.xiangqi.presentation.common.ClassicButtonColors
import com.quangthanh.xiangqi.presentation.common.resultReason
import com.quangthanh.xiangqi.presentation.common.resultTitle
import kotlinx.coroutines.launch

/**
 * Màn hình kết thúc ván: biển kết quả nảy vào giữa màn hình tối, kèm lý do và hai nút.
 * - Đánh với máy: biển "Chiến thắng" hoặc "Thất bại".
 * - Hai người chơi: biển "Hết cờ", dòng chữ bên dưới cho biết bên nào thắng.
 * - Hòa: biển "Hòa cờ".
 */
@Composable
fun ResultOverlay(
    result: GameResult,
    participants: Participants,
    onNewGame: () -> Unit,
    onViewBoard: () -> Unit,
) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(durationMillis = 200)) }
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Dialog(onDismissRequest = onViewBoard, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val banner = painterResource(bannerFor(result, participants))
            Image(
                painter = banner,
                contentDescription = resultTitle(result, participants),
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .aspectRatio(banner.intrinsicSize.width / banner.intrinsicSize.height)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    },
            )
            Text(
                subtitle(result, participants),
                color = Color(0xFFFFE2A8),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(Color.Black, Offset(0f, 3f), blurRadius = 8f)),
                modifier = Modifier.graphicsLayer { this.alpha = alpha.value },
            )
            Row(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ClassicButton(stringResource(R.string.game_new), onNewGame, Modifier.weight(1f), colors = ClassicButtonColors.GoldFilled)
                ClassicButton(stringResource(R.string.game_view_board), onViewBoard, Modifier.weight(1f), colors = ClassicButtonColors.Gold)
            }
        }
    }
}

@DrawableRes
private fun bannerFor(result: GameResult, participants: Participants): Int {
    val humanSide = participants.humanSideAgainstComputer
    return when {
        result is GameResult.Draw -> R.drawable.banner_draw
        result is GameResult.Win && humanSide != null ->
            if (result.winner == humanSide) R.drawable.banner_victory else R.drawable.banner_defeat
        else -> R.drawable.banner_game_over
    }
}

/** Đánh với máy chỉ cần lý do; hai người chơi phải nói rõ bên nào thắng. */
@Composable
private fun subtitle(result: GameResult, participants: Participants): String {
    val reason = resultReason(result)
    if (result !is GameResult.Win || participants.isAgainstComputer) return reason
    val winner = stringResource(if (result.winner == Side.RED) R.string.result_red_wins else R.string.result_black_wins)
    return "$winner · $reason"
}
