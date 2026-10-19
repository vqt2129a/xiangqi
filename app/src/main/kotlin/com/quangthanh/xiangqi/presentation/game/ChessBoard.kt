package com.quangthanh.xiangqi.presentation.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.quangthanh.xiangqi.R
import com.quangthanh.xiangqi.domain.model.Move
import com.quangthanh.xiangqi.domain.model.Piece
import com.quangthanh.xiangqi.domain.model.PieceType
import com.quangthanh.xiangqi.domain.model.PlacedPiece
import com.quangthanh.xiangqi.domain.model.Side
import com.quangthanh.xiangqi.domain.model.Square
import kotlin.math.roundToInt

/**
 * Bàn cờ dựng từ ảnh: ảnh bàn gỗ làm nền, quân cờ là ảnh, khung góc đánh dấu quân đang chọn
 * và nước vừa đi (đỏ hoặc xanh theo bên vừa đi), chấm đỏ cho ô có thể đi tới.
 */
@Composable
fun ChessBoard(
    pieces: List<PlacedPiece>,
    selected: Square?,
    targets: List<Square>,
    lastMove: Move?,
    lastMoveSide: Side?,
    generalInCheck: Square?,
    flipped: Boolean,
    onSquareTapped: (Square) -> Unit,
    modifier: Modifier = Modifier,
) {
    val images = rememberBoardImages()
    Canvas(
        modifier = modifier
            .aspectRatio(BOARD_WIDTH / BOARD_HEIGHT)
            .pointerInput(flipped, onSquareTapped) {
                detectTapGestures { offset ->
                    BoardGeometry(size.width.toFloat(), size.height.toFloat(), flipped).squareAt(offset)?.let(onSquareTapped)
                }
            },
    ) {
        val geometry = BoardGeometry(size.width, size.height, flipped)
        drawBitmap(images.board, Offset.Zero, size.width, size.height)

        generalInCheck?.let {
            val center = geometry.center(it)
            drawCircle(
                Brush.radialGradient(listOf(Color(0xCCFF1744), Color.Transparent), center, geometry.cell * 0.75f),
                geometry.cell * 0.75f,
                center,
            )
        }

        val pieceSize = geometry.cell * 0.94f
        pieces.forEach { (square, piece) -> drawCentered(images.piece(piece), geometry.center(square), pieceSize) }

        val boxSize = geometry.cell * 1.02f
        if (lastMove != null && lastMoveSide != null) {
            val box = images.box(lastMoveSide)
            drawCentered(box, geometry.center(lastMove.from), boxSize)
            drawCentered(box, geometry.center(lastMove.to), boxSize)
        }
        selected?.let { square ->
            pieces.firstOrNull { it.square == square }?.let { drawCentered(images.box(it.piece.side), geometry.center(square), boxSize) }
        }
        // Chấm đỏ chỉ chiếm phần giữa của ảnh move_hint, nên vẽ ảnh cỡ gần một ô.
        targets.forEach { drawCentered(images.moveHint, geometry.center(it), geometry.cell * 0.95f) }
    }
}

// Kích thước ảnh bàn cờ và vị trí lưới, đo trực tiếp trên ảnh board.png (đơn vị pixel).
private const val BOARD_WIDTH = 750f
private const val BOARD_HEIGHT = 909f
private const val GRID_LEFT = 42.5f
private const val GRID_TOP = 79.5f
private const val GRID_STEP_X = 83f
private const val GRID_STEP_Y = 83.11f

/** Chuyển đổi giữa ô trên bàn cờ và tọa độ pixel; khi lật bàn thì quân Đen nằm phía dưới. */
private class BoardGeometry(width: Float, height: Float, private val flipped: Boolean) {
    private val stepX = width * GRID_STEP_X / BOARD_WIDTH
    private val stepY = height * GRID_STEP_Y / BOARD_HEIGHT
    private val left = width * GRID_LEFT / BOARD_WIDTH
    private val top = height * GRID_TOP / BOARD_HEIGHT
    val cell = stepX

    fun center(square: Square): Offset {
        val column = if (flipped) Square.FILES - 1 - square.file else square.file
        val row = if (flipped) square.rank else Square.RANKS - 1 - square.rank
        return Offset(left + column * stepX, top + row * stepY)
    }

    fun squareAt(offset: Offset): Square? {
        val column = ((offset.x - left) / stepX).roundToInt()
        val row = ((offset.y - top) / stepY).roundToInt()
        if (column !in 0 until Square.FILES || row !in 0 until Square.RANKS) return null
        val file = if (flipped) Square.FILES - 1 - column else column
        val rank = if (flipped) row else Square.RANKS - 1 - row
        return Square(file, rank)
    }
}

private class BoardImages(
    val board: ImageBitmap,
    val moveHint: ImageBitmap,
    private val redBox: ImageBitmap,
    private val blackBox: ImageBitmap,
    private val pieces: Map<Piece, ImageBitmap>,
) {
    fun piece(piece: Piece): ImageBitmap = pieces.getValue(piece)
    fun box(side: Side): ImageBitmap = if (side == Side.RED) redBox else blackBox
}

@Composable
private fun rememberBoardImages(): BoardImages {
    val board = ImageBitmap.imageResource(R.drawable.board)
    val moveHint = ImageBitmap.imageResource(R.drawable.move_hint)
    val redBox = ImageBitmap.imageResource(R.drawable.box_red)
    val blackBox = ImageBitmap.imageResource(R.drawable.box_black)
    val resources = LocalContext.current.resources
    val pieces = remember(resources) {
        PieceType.entries.flatMap { type -> Side.entries.map { side -> Piece(type, side) } }
            .associateWith { ImageBitmap.imageResource(resources, it.drawableRes()) }
    }
    return remember(board, moveHint, redBox, blackBox, pieces) { BoardImages(board, moveHint, redBox, blackBox, pieces) }
}

private fun Piece.drawableRes(): Int = when (side) {
    Side.RED -> when (type) {
        PieceType.GENERAL -> R.drawable.piece_red_general
        PieceType.ADVISOR -> R.drawable.piece_red_advisor
        PieceType.ELEPHANT -> R.drawable.piece_red_elephant
        PieceType.HORSE -> R.drawable.piece_red_horse
        PieceType.CHARIOT -> R.drawable.piece_red_chariot
        PieceType.CANNON -> R.drawable.piece_red_cannon
        PieceType.SOLDIER -> R.drawable.piece_red_soldier
    }
    Side.BLACK -> when (type) {
        PieceType.GENERAL -> R.drawable.piece_black_general
        PieceType.ADVISOR -> R.drawable.piece_black_advisor
        PieceType.ELEPHANT -> R.drawable.piece_black_elephant
        PieceType.HORSE -> R.drawable.piece_black_horse
        PieceType.CHARIOT -> R.drawable.piece_black_chariot
        PieceType.CANNON -> R.drawable.piece_black_cannon
        PieceType.SOLDIER -> R.drawable.piece_black_soldier
    }
}

private fun DrawScope.drawCentered(image: ImageBitmap, center: Offset, size: Float) =
    drawBitmap(image, center - Offset(size / 2, size / 2), size, size)

private fun DrawScope.drawBitmap(image: ImageBitmap, topLeft: Offset, width: Float, height: Float) = drawImage(
    image = image,
    dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
    dstSize = IntSize(width.roundToInt(), height.roundToInt()),
    filterQuality = FilterQuality.High,
)
