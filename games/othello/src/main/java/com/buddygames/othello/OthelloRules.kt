package com.buddygames.othello

enum class Disc {
    BLACK,
    WHITE;

    fun other(): Disc = if (this == BLACK) WHITE else BLACK
}

data class OthelloMove(val row: Int, val col: Int)

data class OthelloState(val board: List<List<Disc?>>) {
    companion object {
        const val SIZE = 8

        fun empty(): OthelloState = OthelloState(List(SIZE) { List<Disc?>(SIZE) { null } })

        fun initial(): OthelloState = empty()
            .put(3, 3, Disc.WHITE)
            .put(3, 4, Disc.BLACK)
            .put(4, 3, Disc.BLACK)
            .put(4, 4, Disc.WHITE)
    }

    fun cell(row: Int, col: Int): Disc? = board.getOrNull(row)?.getOrNull(col)

    fun put(row: Int, col: Int, disc: Disc): OthelloState {
        return copy(board = board.mapIndexed { r, cells ->
            if (r != row) cells else cells.mapIndexed { c, old -> if (c == col) disc else old }
        })
    }

    fun count(disc: Disc): Int = board.sumOf { row -> row.count { it == disc } }
}

object OthelloRules {
    private val directions = (-1..1).flatMap { dr ->
        (-1..1).mapNotNull { dc -> if (dr == 0 && dc == 0) null else dr to dc }
    }

    fun legalMoves(state: OthelloState, disc: Disc): List<OthelloMove> {
        return (0 until OthelloState.SIZE).flatMap { row ->
            (0 until OthelloState.SIZE).mapNotNull { col ->
                val move = OthelloMove(row, col)
                if (state.cell(row, col) == null && flipsForMove(state, move, disc).isNotEmpty()) move else null
            }
        }
    }

    fun applyMove(state: OthelloState, move: OthelloMove, disc: Disc): OthelloState {
        val flips = flipsForMove(state, move, disc)
        require(flips.isNotEmpty()) { "Illegal Othello move" }
        return flips.fold(state.put(move.row, move.col, disc)) { next, flip ->
            next.put(flip.row, flip.col, disc)
        }
    }

    fun robotMove(state: OthelloState, disc: Disc): OthelloMove? {
        return legalMoves(state, disc)
            .sortedWith(compareByDescending<OthelloMove> { isCorner(it) }
                .thenByDescending { flipsForMove(state, it, disc).size }
                .thenBy { it.row }
                .thenBy { it.col })
            .firstOrNull()
    }

    fun isGameOver(state: OthelloState): Boolean {
        return legalMoves(state, Disc.BLACK).isEmpty() && legalMoves(state, Disc.WHITE).isEmpty()
    }

    fun winner(state: OthelloState): Disc? {
        val black = state.count(Disc.BLACK)
        val white = state.count(Disc.WHITE)
        return when {
            black > white -> Disc.BLACK
            white > black -> Disc.WHITE
            else -> null
        }
    }

    private fun flipsForMove(state: OthelloState, move: OthelloMove, disc: Disc): List<OthelloMove> {
        return directions.flatMap { (dr, dc) ->
            val captured = mutableListOf<OthelloMove>()
            var row = move.row + dr
            var col = move.col + dc
            while (state.cell(row, col) == disc.other()) {
                captured += OthelloMove(row, col)
                row += dr
                col += dc
            }
            if (captured.isNotEmpty() && state.cell(row, col) == disc) captured else emptyList()
        }
    }

    private fun isCorner(move: OthelloMove): Boolean {
        return (move.row == 0 || move.row == OthelloState.SIZE - 1) &&
            (move.col == 0 || move.col == OthelloState.SIZE - 1)
    }
}

