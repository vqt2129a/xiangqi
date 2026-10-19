package com.quangthanh.xiangqi.infrastructure.ai

import com.quangthanh.xiangqi.infrastructure.ai.EngineBoard.Companion.SQUARES
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/**
 * Tìm nước đi tốt nhất bằng negamax alpha-beta, kèm:
 * - Tìm sâu dần (iterative deepening): tính độ sâu 1, 2, 3... cho đến khi hết giờ, luôn có sẵn nước tốt nhất.
 * - PVS: nước đầu tiên tính đủ cửa sổ, các nước sau chỉ kiểm tra nhanh xem có tốt hơn không.
 * - Bảng băm vị trí, sắp xếp nước đi (nước từ bảng băm, ăn quân MVV-LVA, killer, history).
 * - Quiescence search: ở lá vẫn tính tiếp các nước ăn quân để tránh đánh giá sai giữa một chuỗi đổi quân.
 * - Gia hạn khi bị chiếu để không bỏ sót đòn chiếu bí.
 *
 * Mỗi lần tìm dùng một [Searcher] mới, không dùng chung giữa các luồng.
 */
internal class Searcher(
    private val board: EngineBoard,
    private val settings: SearchSettings,
    private val random: Random,
    private val isCancelled: () -> Boolean,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private val table = TranspositionTable(settings.tableSizeBits)
    private val killers = Array(MAX_PLY) { IntArray(2) }
    private val history = IntArray(SQUARES * SQUARES)
    private val moveBuffers = Array(MAX_PLY) { IntArray(MAX_MOVES) }
    private val scoreBuffers = Array(MAX_PLY) { IntArray(MAX_MOVES) }

    private val timeBudget = settings.timeLimitMillis * 1_000_000
    private var nodes = 0L
    private var stopped = false
    private var rootScore = 0
    private var deadline = 0L

    /** Trả về nước đi đã mã hóa, hoặc 0 nếu bên đang đi không còn nước nào. */
    fun search(): Int {
        val startedAt = nanoTime()
        deadline = startedAt + timeBudget
        val rootMoves = legalMoves()
        if (rootMoves.isEmpty()) return 0
        if (rootMoves.size == 1) return rootMoves[0]

        var best = rootMoves[0]
        for (depth in 1..settings.maxDepth) {
            val move = searchRoot(rootMoves, depth)
            if (stopped) break
            best = move
            // Đưa nước tốt nhất lên đầu để vòng sau tính nó trước, giúp cắt tỉa nhiều hơn.
            rootMoves.remove(move)
            rootMoves.add(0, move)
            // Đã thấy chiếu bí (thắng hoặc thua) thì tính sâu hơn cũng không đổi kết quả.
            if (abs(rootScore) > MATE_THRESHOLD) break
            // Độ sâu sau tốn gấp nhiều lần độ sâu trước; quá nửa thời gian thì không bắt đầu thêm.
            if (nanoTime() - startedAt > timeBudget / 2) break
        }
        return if (settings.randomMargin > 0 && !stopped) pickAmongGoodMoves(rootMoves, best) else best
    }

    private fun searchRoot(moves: List<Int>, depth: Int): Int {
        var alpha = -INFINITY
        var bestMove = moves[0]
        for ((index, move) in moves.withIndex()) {
            board.make(move)
            var score: Int
            if (index == 0) {
                score = -negamax(depth - 1, 1, -INFINITY, -alpha)
            } else {
                score = -negamax(depth - 1, 1, -alpha - 1, -alpha)
                if (score > alpha && !stopped) score = -negamax(depth - 1, 1, -INFINITY, -alpha)
            }
            board.unmake(move)
            if (stopped) return bestMove
            if (score > alpha) {
                alpha = score
                bestMove = move
            }
        }
        rootScore = alpha
        return bestMove
    }

    private fun negamax(requestedDepth: Int, ply: Int, alphaIn: Int, beta: Int): Int {
        if ((++nodes and 2047L) == 0L) checkStop()
        if (stopped) return 0
        if (ply >= MAX_PLY - 1) return Evaluator.evaluate(board)

        val inCheck = board.isInCheck(board.side)
        val depth = if (inCheck) requestedDepth + 1 else requestedDepth
        if (depth <= 0) return quiescence(alphaIn, beta, ply)

        var alpha = alphaIn
        var tableMove = 0
        val entry = table.probe(board.hash)
        if (entry >= 0) {
            tableMove = table.move(entry)
            if (table.depth(entry) >= depth) {
                val score = fromTable(table.score(entry), ply)
                when (table.flag(entry)) {
                    TranspositionTable.EXACT -> return score
                    TranspositionTable.LOWER_BOUND -> if (score >= beta) return score
                    TranspositionTable.UPPER_BOUND -> if (score <= alpha) return score
                }
            }
        }

        val moves = moveBuffers[ply]
        val scores = scoreBuffers[ply]
        val count = board.generateMoves(moves, capturesOnly = false)
        scoreMoves(moves, scores, count, tableMove, ply)

        var best = -INFINITY
        var bestMove = 0
        var legal = 0
        for (i in 0 until count) {
            pickNext(moves, scores, i, count)
            val move = moves[i]
            board.make(move)
            if (board.isInCheck(-board.side)) {
                board.unmake(move)
                continue
            }
            legal++
            var score: Int
            if (legal == 1) {
                score = -negamax(depth - 1, ply + 1, -beta, -alpha)
            } else {
                score = -negamax(depth - 1, ply + 1, -alpha - 1, -alpha)
                if (score > alpha && score < beta) score = -negamax(depth - 1, ply + 1, -beta, -alpha)
            }
            board.unmake(move)
            if (stopped) return 0

            if (score > best) {
                best = score
                bestMove = move
            }
            if (score > alpha) alpha = score
            if (alpha >= beta) {
                if (board[EngineBoard.moveTo(move)] == 0) rememberQuietCutoff(move, depth, ply)
                break
            }
        }

        // Cờ tướng: hết nước đi là thua, dù có bị chiếu hay không. Thua càng muộn càng tốt.
        if (legal == 0) return -MATE + ply

        val flag = when {
            best <= alphaIn -> TranspositionTable.UPPER_BOUND
            best >= beta -> TranspositionTable.LOWER_BOUND
            else -> TranspositionTable.EXACT
        }
        table.store(board.hash, depth, toTable(best, ply), flag, bestMove)
        return best
    }

    private fun quiescence(alphaIn: Int, beta: Int, ply: Int): Int {
        if ((++nodes and 2047L) == 0L) checkStop()
        if (stopped) return 0

        val standPat = Evaluator.evaluate(board)
        if (ply >= MAX_PLY - 1 || standPat >= beta) return standPat
        var alpha = max(alphaIn, standPat)

        val moves = moveBuffers[ply]
        val scores = scoreBuffers[ply]
        val count = board.generateMoves(moves, capturesOnly = true)
        scoreMoves(moves, scores, count, tableMove = 0, ply)

        var best = standPat
        for (i in 0 until count) {
            pickNext(moves, scores, i, count)
            val move = moves[i]
            board.make(move)
            if (board.isInCheck(-board.side)) {
                board.unmake(move)
                continue
            }
            val score = -quiescence(-beta, -alpha, ply + 1)
            board.unmake(move)
            if (stopped) return 0
            if (score > best) best = score
            if (score > alpha) alpha = score
            if (alpha >= beta) break
        }
        return best
    }

    private fun scoreMoves(moves: IntArray, scores: IntArray, count: Int, tableMove: Int, ply: Int) {
        for (i in 0 until count) {
            val move = moves[i]
            val victim = abs(board[EngineBoard.moveTo(move)])
            scores[i] = when {
                move == tableMove -> 1_000_000
                // MVV-LVA: ưu tiên ăn quân giá trị cao bằng quân giá trị thấp.
                victim != 0 -> 100_000 + Evaluator.pieceValue(victim) * 10 -
                    Evaluator.pieceValue(abs(board[EngineBoard.moveFrom(move)])) / 10
                move == killers[ply][0] -> 90_000
                move == killers[ply][1] -> 80_000
                else -> history[EngineBoard.moveFrom(move) * SQUARES + EngineBoard.moveTo(move)]
            }
        }
    }

    /** Chọn nước có điểm cao nhất trong phần còn lại; rẻ hơn sắp xếp cả danh sách vì thường cắt tỉa sớm. */
    private fun pickNext(moves: IntArray, scores: IntArray, start: Int, count: Int) {
        var bestIndex = start
        for (i in start + 1 until count) if (scores[i] > scores[bestIndex]) bestIndex = i
        if (bestIndex != start) {
            moves[start] = moves[bestIndex].also { moves[bestIndex] = moves[start] }
            scores[start] = scores[bestIndex].also { scores[bestIndex] = scores[start] }
        }
    }

    private fun rememberQuietCutoff(move: Int, depth: Int, ply: Int) {
        if (killers[ply][0] != move) {
            killers[ply][1] = killers[ply][0]
            killers[ply][0] = move
        }
        val index = EngineBoard.moveFrom(move) * SQUARES + EngineBoard.moveTo(move)
        history[index] = (history[index] + depth * depth).coerceAtMost(HISTORY_LIMIT)
    }

    private fun pickAmongGoodMoves(rootMoves: List<Int>, best: Int): Int {
        val scored = rootMoves.map { move ->
            board.make(move)
            val score = -negamax(settings.maxDepth - 1, 1, -INFINITY, INFINITY)
            board.unmake(move)
            move to score
        }
        if (stopped) return best
        val top = scored.maxOf { it.second }
        val candidates = scored.filter { it.second >= top - settings.randomMargin }
        return candidates[random.nextInt(candidates.size)].first
    }

    private fun legalMoves(): MutableList<Int> {
        val buffer = IntArray(MAX_MOVES)
        val count = board.generateMoves(buffer, capturesOnly = false)
        val result = mutableListOf<Int>()
        for (i in 0 until count) {
            board.make(buffer[i])
            if (!board.isInCheck(-board.side)) result += buffer[i]
            board.unmake(buffer[i])
        }
        return result
    }

    private fun checkStop() {
        if (isCancelled() || nanoTime() > deadline) stopped = true
    }

    // Điểm chiếu bí phụ thuộc số nước tới khi bị bí, nên khi lưu vào bảng băm phải đổi sang
    // "tính từ thế cờ này" rồi đổi lại khi đọc ra ở độ sâu khác.
    private fun toTable(score: Int, ply: Int) = when {
        score > MATE_THRESHOLD -> score + ply
        score < -MATE_THRESHOLD -> score - ply
        else -> score
    }

    private fun fromTable(score: Int, ply: Int) = when {
        score > MATE_THRESHOLD -> score - ply
        score < -MATE_THRESHOLD -> score + ply
        else -> score
    }

    companion object {
        const val MATE = 30_000
        private const val INFINITY = 32_000
        private const val MAX_PLY = 128
        private const val MATE_THRESHOLD = MATE - MAX_PLY
        private const val MAX_MOVES = 128
        private const val HISTORY_LIMIT = 70_000
    }
}
