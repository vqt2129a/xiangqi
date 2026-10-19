package com.quangthanh.xiangqi.infrastructure.ai

/**
 * Bảng băm lưu kết quả các thế cờ đã tính, vì cùng một thế cờ có thể đạt được bằng nhiều thứ tự nước đi.
 * Dùng các mảng song song thay cho object để tránh tạo rác cho bộ thu gom bộ nhớ.
 */
internal class TranspositionTable(sizeBits: Int) {

    private val mask = (1 shl sizeBits) - 1
    private val keys = LongArray(1 shl sizeBits)
    private val moves = IntArray(1 shl sizeBits)
    private val scores = IntArray(1 shl sizeBits)
    private val depths = ByteArray(1 shl sizeBits)
    private val flags = ByteArray(1 shl sizeBits)

    /** Trả về chỉ số ô chứa [key], hoặc -1 nếu chưa có. */
    fun probe(key: Long): Int {
        val index = (key xor (key ushr 32)).toInt() and mask
        return if (flags[index] != EMPTY && keys[index] == key) index else -1
    }

    fun move(index: Int) = moves[index]
    fun score(index: Int) = scores[index]
    fun depth(index: Int) = depths[index].toInt()
    fun flag(index: Int) = flags[index]

    fun store(key: Long, depth: Int, score: Int, flag: Byte, move: Int) {
        val index = (key xor (key ushr 32)).toInt() and mask
        // Giữ kết quả tính sâu hơn của cùng thế cờ; thế cờ khác thì ghi đè.
        if (flags[index] != EMPTY && keys[index] == key && depths[index] > depth) return
        keys[index] = key
        moves[index] = move
        scores[index] = score
        depths[index] = depth.coerceAtMost(Byte.MAX_VALUE.toInt()).toByte()
        flags[index] = flag
    }

    companion object {
        const val EMPTY: Byte = 0
        const val EXACT: Byte = 1
        const val LOWER_BOUND: Byte = 2
        const val UPPER_BOUND: Byte = 3
    }
}
