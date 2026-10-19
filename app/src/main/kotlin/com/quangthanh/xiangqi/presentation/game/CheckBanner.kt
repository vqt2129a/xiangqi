package com.quangthanh.xiangqi.presentation.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.quangthanh.xiangqi.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Biển "Chiếu tướng" hiện đè giữa bàn cờ: phóng từ to về đúng cỡ, giữ một lúc rồi mờ dần.
 * Mỗi lần [trigger] tăng lên là chạy lại hiệu ứng; bằng 0 thì không hiện gì.
 */
@Composable
fun CheckBanner(trigger: Int, modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        alpha.snapTo(0f)
        scale.snapTo(1.6f)
        coroutineScope {
            launch { alpha.animateTo(1f, tween(durationMillis = 150)) }
            launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
        }
        delay(900)
        alpha.animateTo(0f, tween(durationMillis = 350))
    }

    if (alpha.value > 0f) {
        val banner = painterResource(R.drawable.banner_check)
        Image(
            painter = banner,
            contentDescription = stringResource(R.string.game_message_check),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(banner.intrinsicSize.width / banner.intrinsicSize.height)
                .graphicsLayer {
                    this.alpha = alpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
    }
}
