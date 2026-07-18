package com.buddygames.xiangqi

enum class Side {
    RED,
    BLACK;

    fun other(): Side = if (this == RED) BLACK else RED
}

enum class PieceType(val value: Int) {
    GENERAL(1000),
    ROOK(90),
    CANNON(45),
    HORSE(40),
    ELEPHANT(20),
    ADVISOR(20),
    SOLDIER(10)
}

data class XiangqiPiece(val side: Side, val type: PieceType)
data class XiangqiMove(val fromRow: Int, val fromCol: Int, val toRow: Int, val toCol: Int)

data class XiangqiState(val board: List<List<XiangqiPiece?>>) {
    companion object {
        const val ROWS = 10
        const val COLS = 9

        fun empty(): XiangqiState = XiangqiState(List(ROWS) { List<XiangqiPiece?>(COLS) { null } })

        fun initial(): XiangqiState {
            var state = empty()
            fun add(row: Int, col: Int, side: Side, type: PieceType) {
                state = state.put(row, col, XiangqiPiece(side, type))
            }
            listOf(0 to Side.BLACK, 9 to Side.RED).forEach { (row, side) ->
                add(row, 0, side, PieceType.ROOK)
                add(row, 1, side, PieceType.HORSE)
                add(row, 2, side, PieceType.ELEPHANT)
                add(row, 3, side, PieceType.ADVISOR)
                add(row, 4, side, PieceType.GENERAL)
                add(row, 5, side, PieceType.ADVISOR)
                add(row, 6, side, PieceType.ELEPHANT)
                add(row, 7, side, PieceType.HORSE)
                add(row, 8, side, PieceType.ROOK)
            }
            add(2, 1, Side.BLACK, PieceType.CANNON)
            add(2, 7, Side.BLACK, PieceType.CANNON)
            add(7, 1, Side.RED, PieceType.CANNON)
            add(7, 7, Side.RED, PieceType.CANNON)
            for (col in listOf(0, 2, 4, 6, 8)) {
                add(3, col, Side.BLACK, PieceType.SOLDIER)
                add(6, col, Side.RED, PieceType.SOLDIER)
            }
            return state
        }
    }

    fun piece(row: Int, col: Int): XiangqiPiece? = board.getOrNull(row)?.getOrNull(col)

    fun put(row: Int, col: Int, piece: XiangqiPiece): XiangqiState {
        return copy(board = board.mapIndexed { r, cells ->
            if (r != row) cells else cells.mapIndexed { c, old -> if (c == col) piece else old }
        })
    }

    fun remove(row: Int, col: Int): XiangqiState {
        return copy(board = board.mapIndexed { r, cells ->
            if (r != row) cells else cells.mapIndexed { c, old -> if (c == col) null else old }
        })
    }

    fun apply(move: XiangqiMove): XiangqiState {
        val moving = requireNotNull(piece(move.fromRow, move.fromCol))
        return remove(move.fromRow, move.fromCol).put(move.toRow, move.toCol, moving)
    }
}

object XiangqiRules {
    fun isLegalMove(state: XiangqiState, move: XiangqiMove, side: Side): Boolean {
        if (!isPseudoLegalMove(state, move, side)) return false
        return !isInCheck(state.apply(move), side)
    }

    private fun isPseudoLegalMove(state: XiangqiState, move: XiangqiMove, side: Side): Boolean {
        if (move.toRow !in 0 until XiangqiState.ROWS || move.toCol !in 0 until XiangqiState.COLS) return false
        val piece = state.piece(move.fromRow, move.fromCol) ?: return false
        if (piece.side != side) return false
        if (state.piece(move.toRow, move.toCol)?.side == side) return false
        val dr = move.toRow - move.fromRow
        val dc = move.toCol - move.fromCol
        return when (piece.type) {
            PieceType.ROOK -> (dr == 0 || dc == 0) && screensBetween(state, move) == 0
            PieceType.CANNON -> {
                val target = state.piece(move.toRow, move.toCol)
                (dr == 0 || dc == 0) &&
                    if (target == null) screensBetween(state, move) == 0 else screensBetween(state, move) == 1
            }
            PieceType.HORSE -> horseLegal(state, move, dr, dc)
            PieceType.ELEPHANT -> elephantLegal(state, move, piece.side, dr, dc)
            PieceType.ADVISOR -> kotlin.math.abs(dr) == 1 && kotlin.math.abs(dc) == 1 && inPalace(move.toRow, move.toCol, piece.side)
            PieceType.GENERAL -> generalLegal(state, move, piece.side, dr, dc)
            PieceType.SOLDIER -> soldierLegal(move, piece.side, dr, dc)
        }
    }

    fun legalMoves(state: XiangqiState, side: Side): List<XiangqiMove> {
        val moves = mutableListOf<XiangqiMove>()
        for (fromRow in 0 until XiangqiState.ROWS) for (fromCol in 0 until XiangqiState.COLS) {
            val piece = state.piece(fromRow, fromCol) ?: continue
            if (piece.side != side) continue
            candidateDestinations(state, fromRow, fromCol, piece).forEach { (toRow, toCol) ->
                val move = XiangqiMove(fromRow, fromCol, toRow, toCol)
                if (isLegalMove(state, move, side)) moves += move
            }
        }
        return moves
    }

    private fun candidateDestinations(
        state: XiangqiState,
        row: Int,
        col: Int,
        piece: XiangqiPiece
    ): Sequence<Pair<Int, Int>> {
        val candidates = when (piece.type) {
            PieceType.ROOK -> rayDestinations(state, row, col, cannon = false)
            PieceType.CANNON -> rayDestinations(state, row, col, cannon = true)
            PieceType.HORSE -> HORSE_OFFSETS.map { (dr, dc) -> row + dr to col + dc }
            PieceType.ELEPHANT -> DIAGONAL_TWO_OFFSETS.map { (dr, dc) -> row + dr to col + dc }
            PieceType.ADVISOR -> DIAGONAL_ONE_OFFSETS.map { (dr, dc) -> row + dr to col + dc }
            PieceType.GENERAL -> buildList {
                ORTHOGONAL_OFFSETS.forEach { (dr, dc) -> add(row + dr to col + dc) }
                for (direction in listOf(-1, 1)) {
                    var targetRow = row + direction
                    while (targetRow in 0 until XiangqiState.ROWS) {
                        val target = state.piece(targetRow, col)
                        if (target != null) {
                            if (target.side != piece.side && target.type == PieceType.GENERAL) {
                                add(targetRow to col)
                            }
                            break
                        }
                        targetRow += direction
                    }
                }
            }
            PieceType.SOLDIER -> {
                val forward = if (piece.side == Side.RED) -1 else 1
                listOf(row + forward to col, row to col - 1, row to col + 1)
            }
        }
        return candidates
            .asSequence()
            .distinct()
            .filter { (targetRow, targetCol) ->
                targetRow in 0 until XiangqiState.ROWS &&
                    targetCol in 0 until XiangqiState.COLS
            }
    }

    private fun rayDestinations(
        state: XiangqiState,
        row: Int,
        col: Int,
        cannon: Boolean
    ): List<Pair<Int, Int>> = buildList {
        ORTHOGONAL_OFFSETS.forEach { (rowStep, colStep) ->
            var targetRow = row + rowStep
            var targetCol = col + colStep
            var screenFound = false
            while (
                targetRow in 0 until XiangqiState.ROWS &&
                targetCol in 0 until XiangqiState.COLS
            ) {
                val target = state.piece(targetRow, targetCol)
                if (!cannon) {
                    add(targetRow to targetCol)
                    if (target != null) break
                } else if (!screenFound) {
                    if (target == null) {
                        add(targetRow to targetCol)
                    } else {
                        screenFound = true
                    }
                } else if (target != null) {
                    add(targetRow to targetCol)
                    break
                }
                targetRow += rowStep
                targetCol += colStep
            }
        }
    }

    fun winnerAfterMove(state: XiangqiState, move: XiangqiMove): Side? {
        val mover = state.piece(move.fromRow, move.fromCol)?.side ?: return null
        val captured = state.piece(move.toRow, move.toCol)
        if (captured?.type == PieceType.GENERAL) return mover
        val nextState = state.apply(move)
        return if (legalMoves(nextState, mover.other()).isEmpty()) mover else null
    }

    fun isInCheck(state: XiangqiState, side: Side): Boolean {
        val general = state.board.flatMapIndexed { row, cells ->
            cells.mapIndexedNotNull { col, piece ->
                if (piece == XiangqiPiece(side, PieceType.GENERAL)) row to col else null
            }
        }.firstOrNull() ?: return false

        val opponent = side.other()
        return state.board.anyIndexed { row, cells ->
            cells.anyIndexed { col, piece ->
                piece?.side == opponent &&
                    isPseudoLegalMove(
                        state,
                        XiangqiMove(row, col, general.first, general.second),
                        opponent
                    )
            }
        }
    }

    internal fun isDefended(state: XiangqiState, row: Int, col: Int, side: Side): Boolean {
        val target = state.piece(row, col) ?: return false
        if (target.side != side) return false
        val probe = state.put(row, col, target.copy(side = side.other()))
        return state.board.anyIndexed { fromRow, cells ->
            cells.anyIndexed { fromCol, piece ->
                piece?.side == side &&
                    (fromRow != row || fromCol != col) &&
                    isPseudoLegalMove(
                        probe,
                        XiangqiMove(fromRow, fromCol, row, col),
                        side
                    )
            }
        }
    }

    private inline fun <T> List<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
        forEachIndexed { index, value -> if (predicate(index, value)) return true }
        return false
    }

    private val HORSE_OFFSETS = listOf(
        -2 to -1,
        -2 to 1,
        -1 to -2,
        -1 to 2,
        1 to -2,
        1 to 2,
        2 to -1,
        2 to 1
    )
    private val DIAGONAL_TWO_OFFSETS = listOf(-2 to -2, -2 to 2, 2 to -2, 2 to 2)
    private val DIAGONAL_ONE_OFFSETS = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
    private val ORTHOGONAL_OFFSETS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    private fun screensBetween(state: XiangqiState, move: XiangqiMove): Int {
        val rowStep = move.toRow.compareTo(move.fromRow)
        val colStep = move.toCol.compareTo(move.fromCol)
        var row = move.fromRow + rowStep
        var col = move.fromCol + colStep
        var screens = 0
        while (row != move.toRow || col != move.toCol) {
            if (state.piece(row, col) != null) screens++
            row += rowStep
            col += colStep
        }
        return screens
    }

    private fun horseLegal(state: XiangqiState, move: XiangqiMove, dr: Int, dc: Int): Boolean {
        val adr = kotlin.math.abs(dr)
        val adc = kotlin.math.abs(dc)
        if (!((adr == 2 && adc == 1) || (adr == 1 && adc == 2))) return false
        val legRow = move.fromRow + if (adr == 2) dr / 2 else 0
        val legCol = move.fromCol + if (adc == 2) dc / 2 else 0
        return state.piece(legRow, legCol) == null
    }

    private fun elephantLegal(state: XiangqiState, move: XiangqiMove, side: Side, dr: Int, dc: Int): Boolean {
        if (kotlin.math.abs(dr) != 2 || kotlin.math.abs(dc) != 2) return false
        if (side == Side.RED && move.toRow < 5) return false
        if (side == Side.BLACK && move.toRow > 4) return false
        return state.piece(move.fromRow + dr / 2, move.fromCol + dc / 2) == null
    }

    private fun inPalace(row: Int, col: Int, side: Side): Boolean {
        val rows = if (side == Side.RED) 7..9 else 0..2
        return row in rows && col in 3..5
    }

    private fun generalLegal(
        state: XiangqiState,
        move: XiangqiMove,
        side: Side,
        dr: Int,
        dc: Int
    ): Boolean {
        val target = state.piece(move.toRow, move.toCol)
        val capturesOpposingGeneral = target?.side == side.other() &&
            target.type == PieceType.GENERAL &&
            dc == 0 &&
            screensBetween(state, move) == 0
        return capturesOpposingGeneral ||
            (kotlin.math.abs(dr) + kotlin.math.abs(dc) == 1 && inPalace(move.toRow, move.toCol, side))
    }

    private fun soldierLegal(move: XiangqiMove, side: Side, dr: Int, dc: Int): Boolean {
        val forward = if (side == Side.RED) -1 else 1
        val crossedRiver = if (side == Side.RED) move.fromRow <= 4 else move.fromRow >= 5
        return (dr == forward && dc == 0) || (crossedRiver && dr == 0 && kotlin.math.abs(dc) == 1)
    }
}
