package com.quangthanh.xiangqi.application.usecase

import com.quangthanh.xiangqi.application.fakes.InMemoryGameRepository
import com.quangthanh.xiangqi.application.port.Account
import com.quangthanh.xiangqi.application.port.AccountService
import com.quangthanh.xiangqi.application.port.CloudGameStore
import com.quangthanh.xiangqi.domain.model.Game
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Participants
import com.quangthanh.xiangqi.domain.model.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountUseCasesTest {

    private class FakeCloud : CloudGameStore {
        val games = linkedMapOf<GameId, Game>()
        override suspend fun downloadFinishedGames() = games.values.toList()
        override suspend fun upload(game: Game) { games[game.id] = game }
    }

    private class FakeAccounts : AccountService {
        override val account = MutableStateFlow<Account?>(Account("guest", isGuest = true, null, null))
        var signedOut = false
        override suspend fun signInWithGoogle(idToken: String) =
            Account("google-uid", isGuest = false, "Võ Quang Thành", "thanh@example.com").also { account.value = it }
        override suspend fun signOut() { signedOut = true }
    }

    private val local = InMemoryGameRepository()
    private val cloud = FakeCloud()
    private val accounts = FakeAccounts()
    private val sync = SyncCloudGamesUseCase(local, cloud)

    private fun finishedGame(id: String) = Game.start(GameId(id), Participants.twoPlayers(), Instant.EPOCH).apply {
        play(Move.parse("h2e2"))
        resign(Side.BLACK)
    }

    @Test
    fun `sync uploads finished local games and restores games only on the cloud`() = runTest {
        local.save(finishedGame("played-here"))
        local.save(Game.start(GameId("unfinished"), Participants.twoPlayers(), Instant.EPOCH))
        cloud.upload(finishedGame("played-elsewhere"))

        val result = sync()

        assertEquals(SyncResult(uploaded = 1, restored = 1), result)
        assertEquals(setOf(GameId("played-here"), GameId("played-elsewhere")), cloud.games.keys)
        assertTrue(local.findById(GameId("played-elsewhere"))!!.isFinished)
    }

    @Test
    fun `syncing twice changes nothing the second time`() = runTest {
        local.save(finishedGame("a"))
        cloud.upload(finishedGame("b"))
        sync()

        assertEquals(SyncResult(uploaded = 0, restored = 0), sync())
    }

    @Test
    fun `signing in reports how many games were restored`() = runTest {
        cloud.upload(finishedGame("from-old-phone"))

        val result = SignInWithGoogleUseCase(accounts, sync)("id-token")

        assertEquals(false, result.account.isGuest)
        assertEquals(1, result.restoredGames)
    }

    @Test
    fun `signing out backs up pending games first`() = runTest {
        local.save(finishedGame("not-yet-backed-up"))

        SignOutUseCase(accounts, sync)()

        assertTrue(GameId("not-yet-backed-up") in cloud.games)
        assertTrue(accounts.signedOut)
    }
}
