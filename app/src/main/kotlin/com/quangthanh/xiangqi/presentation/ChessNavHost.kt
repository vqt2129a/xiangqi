package com.quangthanh.xiangqi.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quangthanh.xiangqi.di.AppContainer
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.presentation.account.AccountViewModel
import com.quangthanh.xiangqi.presentation.game.GameScreen
import com.quangthanh.xiangqi.presentation.game.GameViewModel
import com.quangthanh.xiangqi.presentation.history.HistoryScreen
import com.quangthanh.xiangqi.presentation.history.HistoryViewModel
import com.quangthanh.xiangqi.presentation.home.HomeScreen
import com.quangthanh.xiangqi.presentation.home.HomeViewModel
import com.quangthanh.xiangqi.presentation.settings.SettingsViewModel

private object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val GAME = "game/{gameId}"
    fun game(id: GameId) = "game/${id.value}"
}

/** Hành động đi kèm thông báo đẩy. Gửi từ Firebase Console bằng "Custom data": action = resume_game. */
object DeepLinkAction {
    const val RESUME_GAME = "resume_game"
}

/** Mỗi màn hình nhận ViewModel tạo từ use case trong [AppContainer]; ViewModel sống theo màn hình trên back stack. */
@Composable
fun ChessNavHost(container: AppContainer, pendingAction: String?, onActionHandled: () -> Unit) {
    val navController = rememberNavController()

    // Bấm thông báo khi đang ở màn hình khác: quay về màn hình chính để nó xử lý hành động.
    LaunchedEffect(pendingAction) {
        if (pendingAction != null) navController.popBackStack(Routes.HOME, inclusive = false)
    }

    @Composable
    fun settingsViewModel(): SettingsViewModel =
        viewModel(factory = viewModelFactory { initializer { SettingsViewModel(container.soundPlayer) } })

    @Composable
    fun accountViewModel(): AccountViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AccountViewModel(container.accountService, container.signInWithGoogle, container.signOut) }
        },
    )

    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = viewModelFactory { initializer { HomeViewModel(container.openGame, container.startNewGame) } },
            )
            HomeScreen(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel(),
                accountViewModel = accountViewModel(),
                pendingAction = pendingAction,
                onActionHandled = onActionHandled,
                onOpenGame = { navController.navigate(Routes.game(it)) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
            )
        }

        composable(Routes.GAME, arguments = listOf(navArgument("gameId") { type = NavType.StringType })) { entry ->
            val gameId = GameId(requireNotNull(entry.arguments?.getString("gameId")))
            val viewModel: GameViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        GameViewModel(
                            gameId, container.openGame, container.playMove, container.playComputerMove,
                            container.undoMove, container.resign, container.startNewGame, container.soundPlayer,
                        )
                    }
                },
            )
            GameScreen(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel(),
                accountViewModel = accountViewModel(),
                onOpenGame = { id ->
                    navController.navigate(Routes.game(id)) { popUpTo(Routes.HOME) }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.HISTORY) {
            val viewModel: HistoryViewModel = viewModel(
                factory = viewModelFactory { initializer { HistoryViewModel(container.getGameHistory) } },
            )
            HistoryScreen(
                viewModel = viewModel,
                onOpenGame = { navController.navigate(Routes.game(it)) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
