package com.quangthanh.xiangqi.infrastructure.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging

/** Khởi tạo các dịch vụ Firebase lúc mở app. Firebase SDK tự đọc cấu hình từ google-services.json. */
object FirebaseSetup {

    const val TAG = "FCM"

    /** Mọi máy đều theo dõi topic này, để gửi thông báo cho tất cả người chơi từ Firebase Console. */
    private const val TOPIC_ALL_PLAYERS = "all_players"

    lateinit var appVersion: String
        private set

    fun init(context: Context, appVersion: String) {
        this.appVersion = appVersion
        GameNotifications.createChannel(context)

        FirebaseCrashlytics.getInstance().setCustomKey("app_version", appVersion)
        CloudBackup.start(appVersion)

        val messaging = FirebaseMessaging.getInstance()
        messaging.subscribeToTopic(TOPIC_ALL_PLAYERS)
        messaging.token.addOnSuccessListener { token ->
            Log.i(TAG, "FCM token: $token")
            CloudBackup.registerDevice(token, appVersion)
        }
    }
}
