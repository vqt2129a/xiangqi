package com.quangthanh.xiangqi.di

import android.content.Context
import com.quangthanh.xiangqi.application.port.AccountService
import com.quangthanh.xiangqi.application.usecase.GetGameHistoryUseCase
import com.quangthanh.xiangqi.application.usecase.OpenGameUseCase
import com.quangthanh.xiangqi.application.usecase.PlayComputerMoveUseCase
import com.quangthanh.xiangqi.application.usecase.PlayMoveUseCase
import com.quangthanh.xiangqi.application.usecase.ResignUseCase
import com.quangthanh.xiangqi.application.usecase.SignInWithGoogleUseCase
import com.quangthanh.xiangqi.application.usecase.SignOutUseCase
import com.quangthanh.xiangqi.application.usecase.StartNewGameUseCase
import com.quangthanh.xiangqi.application.usecase.SyncCloudGamesUseCase
import com.quangthanh.xiangqi.application.usecase.UndoMoveUseCase
import com.quangthanh.xiangqi.audio.SoundPlayer
import com.quangthanh.xiangqi.domain.repository.GameRepository
import com.quangthanh.xiangqi.domain.service.ComputerPlayer
import com.quangthanh.xiangqi.infrastructure.ai.AlphaBetaComputerPlayer
import com.quangthanh.xiangqi.infrastructure.firebase.CloudBackupGameRepository
import com.quangthanh.xiangqi.infrastructure.firebase.FirebaseAccountService
import com.quangthanh.xiangqi.infrastructure.firebase.FirestoreCloudGameStore
import com.quangthanh.xiangqi.infrastructure.firebase.PerformanceTracedComputerPlayer
import com.quangthanh.xiangqi.infrastructure.persistence.RoomGameRepository
import java.time.Clock

/**
 * Composition root: nơi duy nhất biết cài đặt cụ thể của từng port (Room, Firestore, Firebase Auth, AI alpha-beta...).
 * Thêm hay bỏ cloud, đổi engine AI đều chỉ sửa ở đây, use case và domain giữ nguyên.
 */
class AppContainer(context: Context) {

    /** Kho trong máy (Room): nguồn dữ liệu chính, chơi offline được. */
    private val localRepository: GameRepository by lazy { RoomGameRepository.create(context) }

    // Offline-first: mọi use case dùng kho trong máy; decorator gửi thêm bản sao ván đã xong lên Firestore.
    private val repository: GameRepository by lazy { CloudBackupGameRepository(localRepository) }

    // Decorator: AI thật được bọc để đo thời gian nghĩ bằng Firebase Performance.
    private val computerPlayer: ComputerPlayer = PerformanceTracedComputerPlayer(AlphaBetaComputerPlayer())
    private val clock: Clock = Clock.systemUTC()

    val soundPlayer by lazy { SoundPlayer(context) }
    val accountService: AccountService by lazy { FirebaseAccountService() }

    val startNewGame by lazy { StartNewGameUseCase(repository, clock) }
    val openGame by lazy { OpenGameUseCase(repository) }
    val playMove by lazy { PlayMoveUseCase(repository) }
    val playComputerMove by lazy { PlayComputerMoveUseCase(repository, computerPlayer) }
    val undoMove by lazy { UndoMoveUseCase(repository) }
    val resign by lazy { ResignUseCase(repository) }
    val getGameHistory by lazy { GetGameHistoryUseCase(repository) }

    private val syncCloudGames by lazy { SyncCloudGamesUseCase(localRepository, FirestoreCloudGameStore()) }
    val signInWithGoogle by lazy { SignInWithGoogleUseCase(accountService, syncCloudGames) }
    val signOut by lazy { SignOutUseCase(accountService, syncCloudGames) }
}
