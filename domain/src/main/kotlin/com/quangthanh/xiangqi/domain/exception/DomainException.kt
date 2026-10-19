package com.quangthanh.xiangqi.domain.exception

import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.domain.model.Move

/** Vi phạm quy tắc nghiệp vụ của ván cờ. */
sealed class DomainException(message: String) : RuntimeException(message)

class IllegalMoveException(val move: Move) : DomainException("Illegal move: $move")

class GameAlreadyFinishedException(val gameId: GameId) : DomainException("Game ${gameId.value} is already finished")

class NothingToUndoException(val gameId: GameId) : DomainException("Game ${gameId.value} has no move to undo")
