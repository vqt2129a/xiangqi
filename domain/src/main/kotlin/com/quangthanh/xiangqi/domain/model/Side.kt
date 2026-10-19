package com.quangthanh.xiangqi.domain.model

enum class Side {
    RED,
    BLACK;

    val opponent: Side
        get() = if (this == RED) BLACK else RED

    /** Hướng "tiến lên" theo hàng: Đỏ đi từ hàng 0 lên, Đen đi từ hàng 9 xuống. */
    val forward: Int
        get() = if (this == RED) 1 else -1
}
