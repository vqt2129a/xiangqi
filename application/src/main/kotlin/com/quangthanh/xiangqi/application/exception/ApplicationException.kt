package com.quangthanh.xiangqi.application.exception

import com.quangthanh.xiangqi.domain.model.GameId

sealed class ApplicationException(message: String) : RuntimeException(message)

class GameNotFoundException(val gameId: GameId) : ApplicationException("Game ${gameId.value} not found")

/** Người bấm đi khi đang là lượt của máy, hoặc ngược lại. */
class NotYourTurnException(val gameId: GameId) : ApplicationException("It is not this player's turn in game ${gameId.value}")

/** Ván cờ đã bị thay đổi (ví dụ người chơi đi lại) trong lúc máy đang tính nước. */
class GameChangedException(val gameId: GameId) : ApplicationException("Game ${gameId.value} changed while the computer was thinking")
