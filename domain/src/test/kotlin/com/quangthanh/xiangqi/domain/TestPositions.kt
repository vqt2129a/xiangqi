package com.quangthanh.xiangqi.domain

import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Position
import com.quangthanh.xiangqi.domain.model.Square
import com.quangthanh.xiangqi.domain.notation.Fen

fun fen(text: String): Position = Fen.parse(text)

fun move(text: String): Move = Move.parse(text)

fun square(text: String): Square = Square.parse(text)
