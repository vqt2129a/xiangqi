package com.quangthanh.xiangqi.application.usecase

import com.quangthanh.xiangqi.application.dto.GameState
import com.quangthanh.xiangqi.application.dto.GameUpdate
import com.quangthanh.xiangqi.domain.model.Game
import com.quangthanh.xiangqi.domain.model.Player

internal fun Game.toState(): GameState {
    val humanToMove = playerToMove is Player.Human && !isFinished
    return GameState(
        gameId = id,
        participants = participants,
        pieces = position.board.pieces(),
        sideToMove = sideToMove,
        status = status,
        lastMove = lastMove?.move,
        generalInCheck = if (isInCheck) position.board.generalOf(sideToMove) else null,
        legalMoves = if (humanToMove) legalMoves().groupBy({ it.from }, { it.to }) else emptyMap(),
        moveCount = moves.size,
        canUndo = UndoPolicy.pliesToUndo(this) > 0,
        isComputerTurn = playerToMove is Player.Computer && !isFinished,
    )
}

internal fun Game.toUpdate(): GameUpdate = GameUpdate(toState(), pullEvents())
