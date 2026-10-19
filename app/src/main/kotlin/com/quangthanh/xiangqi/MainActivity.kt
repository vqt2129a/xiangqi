package com.quangthanh.xiangqi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quangthanh.xiangqi.infrastructure.firebase.GameNotifications
import com.quangthanh.xiangqi.presentation.ChessNavHost
import com.quangthanh.xiangqi.presentation.theme.ChessTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val container get() = (application as ChessApplication).container

    /** Hành động gửi kèm thông báo đẩy (ví dụ "resume_game"), chờ giao diện xử lý. */
    private val pendingAction = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) readAction(intent)
        setContent {
            val action by pendingAction.collectAsStateWithLifecycle()
            ChessTheme {
                ChessNavHost(container, action, onActionHandled = { pendingAction.value = null })
            }
        }
    }

    /** App đang mở mà người dùng bấm thông báo: Activity được dùng lại (launchMode singleTask). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readAction(intent)
    }

    private fun readAction(intent: Intent?) {
        intent?.getStringExtra(GameNotifications.EXTRA_ACTION)?.let { pendingAction.value = it }
    }

    override fun onStart() {
        super.onStart()
        container.soundPlayer.onForeground()
    }

    override fun onStop() {
        container.soundPlayer.onBackground()
        super.onStop()
    }
}
