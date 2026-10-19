package com.quangthanh.xiangqi.infrastructure.firebase

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.quangthanh.xiangqi.application.port.CloudGameStore
import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.Game
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.GameResult
import com.quangthanh.xiangqi.domain.model.GameStatus
import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Participants
import com.quangthanh.xiangqi.domain.model.Player
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.domain.model.WinReason
import com.quangthanh.xiangqi.domain.notation.Fen
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Ván đã kết thúc của tài khoản hiện tại, lưu ở users/{uid}/games/{gameId}.
 * Giống Room, chỉ lưu nước đi; khi tải về thì Game.restore đi lại từng nước, nên dữ liệu hỏng bị phát hiện ngay.
 */
class FirestoreCloudGameStore : CloudGameStore {

    private val firestore get() = FirebaseFirestore.getInstance()

    private suspend fun games() =
        firestore.collection("users").document(FirebaseSession.uid()).collection("games")

    override suspend fun downloadFinishedGames(): List<Game> =
        games().get().await().documents.mapNotNull { document ->
            runCatching { document.toGame() }
                .onFailure { Log.w(TAG, "Skipping corrupted cloud game ${document.id}", it) }
                .getOrNull()
        }

    override suspend fun upload(game: Game) {
        val result = (game.status as? GameStatus.Finished)?.result ?: return
        games().document(game.id.value).set(game.toDocument(result)).await()
    }

    private fun Game.toDocument(result: GameResult): Map<String, Any?> = mapOf(
        "red" to participants.red.encode(),
        "black" to participants.black.encode(),
        "startingFen" to Fen.format(startingPosition),
        "moves" to moves.map { it.move.toString() },
        "moveCount" to moves.size,
        "result" to if (result is GameResult.Win) "WIN" else "DRAW",
        "winner" to (result as? GameResult.Win)?.winner?.name,
        "reason" to when (result) {
            is GameResult.Win -> result.reason.name
            is GameResult.Draw -> result.reason.name
        },
        "startedAt" to Timestamp(Date.from(startedAt)),
        "finishedAt" to FieldValue.serverTimestamp(),
    )

    private fun DocumentSnapshot.toGame(): Game {
        val winner = getString("winner")?.let(Side::valueOf)
        // Ván kết thúc vì xin thua: bên xin thua là bên không thắng; các kết quả khác tự suy ra khi đi lại nước.
        val resignedBy = if (getString("reason") == WinReason.RESIGNATION.name) winner?.opponent else null
        @Suppress("UNCHECKED_CAST")
        val moves = (get("moves") as List<String>).map(Move::parse)
        return Game.restore(
            id = GameId(id),
            participants = Participants(decode(getString("red")!!), decode(getString("black")!!)),
            startedAt = getTimestamp("startedAt")!!.toDate().toInstant(),
            startingPosition = Fen.parse(getString("startingFen")!!),
            moves = moves,
            resignedBy = resignedBy,
        )
    }

    private fun Player.encode(): String = when (this) {
        Player.Human -> HUMAN
        is Player.Computer -> COMPUTER_PREFIX + difficulty.name
    }

    private fun decode(value: String): Player = when {
        value == HUMAN -> Player.Human
        value.startsWith(COMPUTER_PREFIX) -> Player.Computer(Difficulty.valueOf(value.removePrefix(COMPUTER_PREFIX)))
        else -> error("Unknown player: $value")
    }

    private companion object {
        const val TAG = "CloudGameStore"
        const val HUMAN = "HUMAN"
        const val COMPUTER_PREFIX = "COMPUTER_"
    }
}
