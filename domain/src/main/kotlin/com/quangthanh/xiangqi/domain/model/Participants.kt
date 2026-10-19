package com.quangthanh.xiangqi.domain.model

enum class Difficulty { EASY, MEDIUM, HARD }

sealed interface Player {
    data object Human : Player
    data class Computer(val difficulty: Difficulty) : Player
}

/** Ai cầm quân Đỏ, ai cầm quân Đen. Ứng dụng không hỗ trợ cho máy tự đánh với máy. */
data class Participants(val red: Player, val black: Player) {

    init {
        require(red is Player.Human || black is Player.Human) { "At least one side must be played by a human" }
    }

    fun playerOf(side: Side): Player = if (side == Side.RED) red else black

    val isAgainstComputer: Boolean
        get() = red is Player.Computer || black is Player.Computer

    /** Bên do người cầm khi đánh với máy; null khi hai người chơi với nhau. */
    val humanSideAgainstComputer: Side?
        get() = when {
            red is Player.Computer -> Side.BLACK
            black is Player.Computer -> Side.RED
            else -> null
        }

    val computer: Player.Computer?
        get() = red as? Player.Computer ?: black as? Player.Computer

    companion object {
        fun twoPlayers() = Participants(Player.Human, Player.Human)

        fun againstComputer(humanSide: Side, difficulty: Difficulty): Participants {
            val computer = Player.Computer(difficulty)
            return if (humanSide == Side.RED) Participants(Player.Human, computer) else Participants(computer, Player.Human)
        }
    }
}
