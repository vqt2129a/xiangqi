package com.quangthanh.xiangqi.infrastructure.firebase

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.quangthanh.xiangqi.domain.model.Game
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Các việc sao lưu chạy ngầm, không bao giờ làm gián đoạn việc chơi: lỗi chỉ ghi log,
 * còn khi mất mạng Firestore tự xếp hàng và gửi lại sau.
 *
 *   users/{uid}                  hồ sơ: phiên bản app, lần mở gần nhất
 *   users/{uid}/games/{gameId}   ván đã kết thúc (xem FirestoreCloudGameStore)
 *   users/{uid}/devices/{token}  token FCM, để sau này server gửi thông báo cho đúng người
 */
object CloudBackup {

    private const val TAG = "CloudBackup"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = FirestoreCloudGameStore()
    private val firestore get() = FirebaseFirestore.getInstance()

    fun start(appVersion: String) = launchSafely("profile") {
        userDoc().set(
            mapOf("appVersion" to appVersion, "platform" to "android", "lastSeen" to FieldValue.serverTimestamp()),
            SetOptions.merge(),
        ).await()
    }

    fun registerDevice(token: String, appVersion: String) = launchSafely("device") {
        userDoc().collection("devices").document(token).set(
            mapOf("appVersion" to appVersion, "updatedAt" to FieldValue.serverTimestamp()),
        ).await()
    }

    fun uploadFinishedGame(game: Game) = launchSafely("game ${game.id.value}") { store.upload(game) }

    private suspend fun userDoc() = firestore.collection("users").document(FirebaseSession.uid())

    private fun launchSafely(what: String, block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }
                .onSuccess { Log.i(TAG, "Synced $what") }
                .onFailure { Log.w(TAG, "Could not sync $what", it) }
        }
    }
}
