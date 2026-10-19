package com.quangthanh.xiangqi.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quangthanh.xiangqi.R
import kotlin.math.ceil

/** Bảng màu cho nút: nút xanh như bản gốc dùng trong ván cờ, nút tối viền vàng dùng trên ảnh nền màn hình chính. */
data class ClassicButtonColors(val container: Color, val pressed: Color, val content: Color, val border: Color) {
    companion object {
        val Blue = ClassicButtonColors(Color(0xFF00BFFF), Color(0xFF1E90FF), Color(0xFF1A1A1A), Color(0xFFDCDCDC))
        val Gold = ClassicButtonColors(Color(0xF0241509), Color(0xFF4A2C12), Color(0xFFFFE2A8), Color(0xFFC9973F))
        val GoldFilled = ClassicButtonColors(Color(0xFFE3AE4F), Color(0xFFC48A2C), Color(0xFF2A1708), Color(0xFFFFE6B0))
    }
}

@Composable
fun ClassicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ClassicButtonColors = ClassicButtonColors.Blue,
    fontSize: TextUnit = 18.sp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(2.dp, colors.border.copy(alpha = if (enabled) 1f else 0.5f)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) colors.pressed else colors.container,
            contentColor = colors.content,
            disabledContainerColor = colors.container.copy(alpha = 0.45f),
            disabledContentColor = colors.content.copy(alpha = 0.5f),
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        interactionSource = interaction,
    ) {
        Text(text, fontSize = fontSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** Màu chung cho hộp thoại kiểu gỗ tối viền vàng, đồng bộ với màn hình chính và biển kết quả. */
object DialogColors {
    val Gold = Color(0xFFE3AE4F)
    val GoldLight = Color(0xFFFFE2A8)
    val Text = Color(0xFFF3DDB3)
    val TextDim = Color(0xFFB89B6E)
    val PanelTop = Color(0xFF3A2414)
    val PanelBottom = Color(0xFF1C110A)
}

@Composable
fun ClassicDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DialogColors.PanelTop, DialogColors.PanelBottom)), shape)
                .border(2.dp, Brush.verticalGradient(listOf(DialogColors.GoldLight, DialogColors.Gold, Color(0xFF8A5E1E))), shape)
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DialogColors.GoldLight,
                style = TextStyle(shadow = Shadow(Color.Black, Offset(0f, 3f), blurRadius = 8f)),
            )
            content()
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ClassicButton(confirmText, onConfirm, Modifier.weight(1f), colors = ClassicButtonColors.GoldFilled)
                ClassicButton(dismissText, onDismiss, Modifier.weight(1f), colors = ClassicButtonColors.Gold)
            }
        }
    }
}

@Composable
fun DialogMessage(text: String) {
    Text(text, fontSize = 18.sp, color = DialogColors.Text, textAlign = TextAlign.Center)
}

@Composable
fun DialogLabel(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DialogColors.TextDim, letterSpacing = 1.sp)
}

/** Hàng ô chọn: ô đang chọn tô vàng, các ô khác nền tối viền vàng mờ. */
@Composable
fun <T> ChoiceChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val shape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSelected) DialogColors.Gold else Color(0x33000000), shape)
                    .border(1.5.dp, if (isSelected) DialogColors.GoldLight else DialogColors.Gold.copy(alpha = 0.45f), shape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(value) })
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF2A1708) else DialogColors.Text,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Nền gỗ: vân gỗ chạy dọc nên kéo giãn ảnh phủ kín màn hình trông vẫn tự nhiên và không có đường nối. */
@Composable
fun Modifier.woodBackground(): Modifier {
    val wood = ImageBitmap.imageResource(R.drawable.bg_wood)
    return drawBehind {
        drawImage(
            image = wood,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(ceil(size.width).toInt(), ceil(size.height).toInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}
