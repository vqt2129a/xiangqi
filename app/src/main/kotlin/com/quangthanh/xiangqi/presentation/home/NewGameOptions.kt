package com.quangthanh.xiangqi.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quangthanh.xiangqi.R
import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.presentation.common.ChoiceChips
import com.quangthanh.xiangqi.presentation.common.DialogColors
import com.quangthanh.xiangqi.presentation.common.DialogLabel
import com.quangthanh.xiangqi.presentation.common.difficultyText

/** Nội dung hộp thoại "Đấu với máy": chọn độ khó và bên cầm quân. */
@Composable
fun NewGameOptions(
    difficulty: Difficulty,
    onDifficultyChange: (Difficulty) -> Unit,
    humanSide: Side,
    onSideChange: (Side) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DialogLabel(stringResource(R.string.home_difficulty).uppercase())
        ChoiceChips(
            options = Difficulty.entries.map { it to "${difficultyText(it)}\n${"★".repeat(it.ordinal + 1)}" },
            selected = difficulty,
            onSelect = onDifficultyChange,
        )
        DialogLabel(stringResource(R.string.home_side).uppercase())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SideOption(
                image = R.drawable.piece_red_general,
                label = stringResource(R.string.side_red_short),
                caption = stringResource(R.string.home_moves_first),
                selected = humanSide == Side.RED,
                onClick = { onSideChange(Side.RED) },
            )
            SideOption(
                image = R.drawable.piece_black_general,
                label = stringResource(R.string.side_black),
                caption = stringResource(R.string.home_moves_second),
                selected = humanSide == Side.BLACK,
                onClick = { onSideChange(Side.BLACK) },
            )
        }
    }
}

/** Ảnh quân Tướng để chọn bên; bên đang chọn to hơn, có viền vàng, bên kia mờ đi. */
@Composable
private fun SideOption(image: Int, label: String, caption: String, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1f else 0.82f, label = "sideScale")
    Column(
        modifier = Modifier
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(8.dp)
            .graphicsLayer { alpha = if (selected) 1f else 0.55f },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = label,
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .border(if (selected) 3.dp else 0.dp, if (selected) DialogColors.Gold else Color.Transparent, CircleShape),
        )
        Text(label, color = DialogColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(caption, color = DialogColors.TextDim, fontSize = 13.sp)
    }
}
