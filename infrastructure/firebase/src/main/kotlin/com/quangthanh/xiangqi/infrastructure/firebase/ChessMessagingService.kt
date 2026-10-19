package com.quangthanh.xiangqi.infrastructure.firebase

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Nhận thông báo đẩy từ FCM.
 * - App ở nền: hệ thống tự hiện thông báo, bấm vào sẽ mở app kèm dữ liệu "action".
 * - App đang mở: hệ thống không tự hiện, nên tự dựng thông báo ở đây.
 */
class ChessMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification
        val title = notification?.title ?: message.data["title"]
        val body = notification?.body ?: message.data["body"]
        if (title != null || body != null) GameNotifications.show(this, title, body, message.data)
    }

    /** Token định danh máy này để gửi thông báo riêng; xem trong logcat với tag "FCM". */
    override fun onNewToken(token: String) {
        Log.i(FirebaseSetup.TAG, "New FCM token: $token")
        CloudBackup.registerDevice(token, FirebaseSetup.appVersion)
    }
}
