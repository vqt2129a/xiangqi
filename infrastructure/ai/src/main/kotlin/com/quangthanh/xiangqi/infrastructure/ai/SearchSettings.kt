package com.quangthanh.xiangqi.infrastructure.ai

import com.quangthanh.xiangqi.domain.model.Difficulty

internal data class SearchSettings(
    val maxDepth: Int,
    val timeLimitMillis: Long,
    /** Mức dễ chọn ngẫu nhiên trong các nước kém nước tốt nhất không quá ngần này điểm, để máy "biết sai". */
    val randomMargin: Int,
    val tableSizeBits: Int,
) {
    companion object {
        fun of(difficulty: Difficulty) = when (difficulty) {
            Difficulty.EASY -> SearchSettings(maxDepth = 2, timeLimitMillis = 500, randomMargin = 120, tableSizeBits = 14)
            Difficulty.MEDIUM -> SearchSettings(maxDepth = 4, timeLimitMillis = 1_500, randomMargin = 0, tableSizeBits = 16)
            Difficulty.HARD -> SearchSettings(maxDepth = 32, timeLimitMillis = 3_000, randomMargin = 0, tableSizeBits = 18)
        }
    }
}
