package com.quangthanh.xiangqi.presentation.home

import com.quangthanh.xiangqi.application.fakes.InMemoryGameRepository
import com.quangthanh.xiangqi.application.usecase.GameMode
import com.quangthanh.xiangqi.application.usecase.OpenGameUseCase
import com.quangthanh.xiangqi.application.usecase.StartNewGameUseCase
import com.quangthanh.xiangqi.domain.model.GameId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val repository = InMemoryGameRepository()
    private val startNewGame = StartNewGameUseCase(repository, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel() = HomeViewModel(OpenGameUseCase(repository), startNewGame)

    @Test
    fun `no prompt when there is no unfinished game`() = runTest {
        val viewModel = newViewModel()
        viewModel.refresh()
        assertFalse(viewModel.uiState.value.showResumePrompt)
    }

    @Test
    fun `opening the app with an unfinished game asks once`() = runTest {
        val unfinished = startNewGame(GameMode.TwoPlayers).gameId
        val viewModel = newViewModel()
        val opened = mutableListOf<GameId>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.gameToOpen.collect { opened += it } }

        viewModel.refresh()
        assertTrue(viewModel.uiState.value.showResumePrompt)

        viewModel.continueGame()
        assertFalse(viewModel.uiState.value.showResumePrompt)
        assertEquals(listOf(unfinished), opened)

        viewModel.refresh() // quay lại màn hình chính sau khi chơi: không hỏi lại
        assertFalse(viewModel.uiState.value.showResumePrompt)
    }

    @Test
    fun `choosing a new game at the prompt skips the second abandon confirmation`() = runTest {
        startNewGame(GameMode.TwoPlayers)
        val viewModel = newViewModel()
        val opened = mutableListOf<GameId>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.gameToOpen.collect { opened += it } }
        viewModel.refresh()

        viewModel.skipResume()
        viewModel.startTwoPlayers()

        assertNull(viewModel.uiState.value.pendingMode)
        assertEquals(1, opened.size)
    }

    @Test
    fun `starting a new game without the prompt still asks before abandoning`() = runTest {
        val viewModel = newViewModel()
        viewModel.refresh() // chưa có ván dở nên không hỏi gì
        startNewGame(GameMode.TwoPlayers) // ván dở xuất hiện sau đó, ví dụ vừa chơi xong rồi quay lại
        viewModel.refresh()

        viewModel.startTwoPlayers()
        assertEquals(GameMode.TwoPlayers, viewModel.uiState.value.pendingMode)
    }
}
