package com.buddygames.gomoku

enum class Stone {
    BLACK,
    WHITE;

    fun other(): Stone = if (this == BLACK) WHITE else BLACK
}

data class GomokuMove(val row: Int, val col: Int)

data class GomokuState(val board: List<List<Stone?>>) {
    companion object {
        const val SIZE = 15

        fun empty(): GomokuState = GomokuState(
            List(SIZE) { List<Stone?>(SIZE) { null } }
        )
    }

    fun cell(row: Int, col: Int): Stone? = board.getOrNull(row)?.getOrNull(col)

    fun place(row: Int, col: Int, stone: Stone): GomokuState {
        require(row in 0 until SIZE && col in 0 until SIZE) { "Move outside board" }
        require(cell(row, col) == null) { "Cell is occupied" }
        return copy(board = board.mapIndexed { r, cells ->
            if (r != row) cells else cells.mapIndexed { c, old -> if (c == col) stone else old }
        })
    }

    fun legalMoves(): List<GomokuMove> = board.flatMapIndexed { row, cells ->
        cells.mapIndexedNotNull { col, stone -> if (stone == null) GomokuMove(row, col) else null }
    }
}

object GomokuRules {
    private val directions = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)

    fun winner(state: GomokuState): Stone? {
        for (row in 0 until GomokuState.SIZE) {
            for (col in 0 until GomokuState.SIZE) {
                val stone = state.cell(row, col) ?: continue
                if (directions.any { (dr, dc) -> hasFive(state, row, col, dr, dc, stone) }) {
                    return stone
                }
            }
        }
        return null
    }

    fun robotMove(state: GomokuState, robot: Stone): GomokuMove {
        immediateWinningMove(state, robot)?.let { return it }
        immediateWinningMove(state, robot.other())?.let { return it }
        return state.legalMoves()
            .sortedWith(compareByDescending<GomokuMove> { neighborCount(state, it) }
                .thenBy { it.row }
                .thenBy { it.col })
            .first()
    }

    private fun immediateWinningMove(state: GomokuState, stone: Stone): GomokuMove? {
        return state.legalMoves().firstOrNull { move ->
            winner(state.place(move.row, move.col, stone)) == stone
        }
    }

    private fun hasFive(state: GomokuState, row: Int, col: Int, dr: Int, dc: Int, stone: Stone): Boolean {
        return (0 until 5).all { step -> state.cell(row + dr * step, col + dc * step) == stone }
    }

    private fun neighborCount(state: GomokuState, move: GomokuMove): Int {
        var count = 0
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            if (state.cell(move.row + dr, move.col + dc) != null) count++
        }
        return count
    }
}

