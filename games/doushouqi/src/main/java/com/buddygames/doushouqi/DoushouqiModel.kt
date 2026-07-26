package com.buddygames.doushouqi

data class DoushouqiPosition(val row: Int, val column: Int) {
    init {
        require(row in 0 until DoushouqiState.ROWS)
        require(column in 0 until DoushouqiState.COLUMNS)
    }
}

enum class DoushouqiSide {
    PINE_GREEN,
    VERMILION;

    fun other(): DoushouqiSide =
        if (this == PINE_GREEN) VERMILION else PINE_GREEN
}

enum class DoushouqiAnimal(val label: String, val rank: Int) {
    ELEPHANT("象", 8),
    LION("狮", 7),
    TIGER("虎", 6),
    LEOPARD("豹", 5),
    WOLF("狼", 4),
    DOG("狗", 3),
    CAT("猫", 2),
    RAT("鼠", 1),
}

data class DoushouqiPiece(
    val side: DoushouqiSide,
    val animal: DoushouqiAnimal,
)

data class DoushouqiMove(
    val from: DoushouqiPosition,
    val to: DoushouqiPosition,
)

enum class DoushouqiTerrain {
    LAND,
    RIVER,
    TRAP,
    DEN,
}

enum class DoushouqiWinReason {
    DEN,
    FINAL_CAPTURE,
    NO_LEGAL_MOVE,
}

enum class DoushouqiDrawReason {
    REPETITION,
    QUIET_100,
}

sealed interface DoushouqiResult {
    data class Win(
        val winner: DoushouqiSide,
        val reason: DoushouqiWinReason,
    ) : DoushouqiResult

    data class Draw(val reason: DoushouqiDrawReason) : DoushouqiResult
}

class DoushouqiState private constructor(
    board: List<DoushouqiPiece?>,
    val sideToMove: DoushouqiSide,
    val quietHalfMoves: Int,
    repetitionCounts: Map<Long, Int>?,
    val lastMove: DoushouqiMove?,
    val result: DoushouqiResult?,
) {
    private val board = board.toList()
    val positionKey: Long = doushouqiPositionKey(this.board, sideToMove)
    val repetitionCounts: Map<Long, Int> =
        (repetitionCounts ?: mapOf(positionKey to 1)).toMap()

    init {
        require(this.board.size == SQUARES)
        require(quietHalfMoves >= 0)
        require(this.repetitionCounts.values.all { it > 0 })
    }

    fun pieceAt(position: DoushouqiPosition): DoushouqiPiece? =
        board[position.index]

    fun boardSnapshot(): List<DoushouqiPiece?> = board.toList()

    fun pieces(): List<Pair<DoushouqiPosition, DoushouqiPiece>> =
        board.mapIndexedNotNull { index, piece ->
            piece?.let {
                DoushouqiPosition(index / COLUMNS, index % COLUMNS) to it
            }
        }

    internal fun copyWith(
        board: List<DoushouqiPiece?> = this.board,
        sideToMove: DoushouqiSide = this.sideToMove,
        quietHalfMoves: Int = this.quietHalfMoves,
        repetitionCounts: Map<Long, Int>? = this.repetitionCounts,
        lastMove: DoushouqiMove? = this.lastMove,
        result: DoushouqiResult? = this.result,
    ): DoushouqiState = DoushouqiState(
        board,
        sideToMove,
        quietHalfMoves,
        repetitionCounts,
        lastMove,
        result,
    )

    companion object {
        const val ROWS = 9
        const val COLUMNS = 7
        const val SQUARES = ROWS * COLUMNS

        fun initial(): DoushouqiState = fromPieces(
            sideToMove = DoushouqiSide.PINE_GREEN,
            pieces = initialPieces(),
        )

        internal fun fromPieces(
            sideToMove: DoushouqiSide,
            pieces: Map<DoushouqiPosition, DoushouqiPiece>,
            quietHalfMoves: Int = 0,
            repetitionCounts: Map<Long, Int>? = null,
            lastMove: DoushouqiMove? = null,
            result: DoushouqiResult? = null,
        ): DoushouqiState {
            val board = MutableList<DoushouqiPiece?>(SQUARES) { null }
            pieces.forEach { (position, piece) ->
                require(board[position.index] == null)
                board[position.index] = piece
            }
            return DoushouqiState(
                board,
                sideToMove,
                quietHalfMoves,
                repetitionCounts,
                lastMove,
                result,
            )
        }

        private fun initialPieces(): Map<DoushouqiPosition, DoushouqiPiece> =
            linkedMapOf(
                DoushouqiPosition(6, 0) to green(DoushouqiAnimal.ELEPHANT),
                DoushouqiPosition(6, 2) to green(DoushouqiAnimal.WOLF),
                DoushouqiPosition(6, 4) to green(DoushouqiAnimal.LEOPARD),
                DoushouqiPosition(6, 6) to green(DoushouqiAnimal.RAT),
                DoushouqiPosition(7, 1) to green(DoushouqiAnimal.CAT),
                DoushouqiPosition(7, 5) to green(DoushouqiAnimal.DOG),
                DoushouqiPosition(8, 0) to green(DoushouqiAnimal.TIGER),
                DoushouqiPosition(8, 6) to green(DoushouqiAnimal.LION),
                DoushouqiPosition(0, 0) to red(DoushouqiAnimal.LION),
                DoushouqiPosition(0, 6) to red(DoushouqiAnimal.TIGER),
                DoushouqiPosition(1, 1) to red(DoushouqiAnimal.DOG),
                DoushouqiPosition(1, 5) to red(DoushouqiAnimal.CAT),
                DoushouqiPosition(2, 0) to red(DoushouqiAnimal.RAT),
                DoushouqiPosition(2, 2) to red(DoushouqiAnimal.LEOPARD),
                DoushouqiPosition(2, 4) to red(DoushouqiAnimal.WOLF),
                DoushouqiPosition(2, 6) to red(DoushouqiAnimal.ELEPHANT),
            )

        private fun green(animal: DoushouqiAnimal) =
            DoushouqiPiece(DoushouqiSide.PINE_GREEN, animal)

        private fun red(animal: DoushouqiAnimal) =
            DoushouqiPiece(DoushouqiSide.VERMILION, animal)
    }
}

internal val DoushouqiPosition.index: Int
    get() = row * DoushouqiState.COLUMNS + column

internal fun terrainAt(position: DoushouqiPosition): DoushouqiTerrain = when {
    position in DENS -> DoushouqiTerrain.DEN
    position in ALL_TRAPS -> DoushouqiTerrain.TRAP
    position.row in 3..5 && position.column in setOf(1, 2, 4, 5) ->
        DoushouqiTerrain.RIVER
    else -> DoushouqiTerrain.LAND
}

internal fun denOwner(position: DoushouqiPosition): DoushouqiSide? = when (position) {
    DoushouqiPosition(0, 3) -> DoushouqiSide.VERMILION
    DoushouqiPosition(8, 3) -> DoushouqiSide.PINE_GREEN
    else -> null
}

internal fun trapOwner(position: DoushouqiPosition): DoushouqiSide? = when {
    position in VERMILION_TRAPS -> DoushouqiSide.VERMILION
    position in GREEN_TRAPS -> DoushouqiSide.PINE_GREEN
    else -> null
}

internal fun doushouqiPositionKey(
    board: List<DoushouqiPiece?>,
    sideToMove: DoushouqiSide,
): Long {
    var hash = -0x340d631b7bdddcdbL
    board.forEachIndexed { index, piece ->
        val code = piece?.let {
            1 + it.side.ordinal * DoushouqiAnimal.entries.size + it.animal.ordinal
        } ?: 0
        hash = (hash xor (index * 17L + code)) * 0x100000001b3L
    }
    return (hash xor sideToMove.ordinal.toLong()) * 0x100000001b3L
}

private val VERMILION_TRAPS = setOf(
    DoushouqiPosition(0, 2),
    DoushouqiPosition(0, 4),
    DoushouqiPosition(1, 3),
)

private val GREEN_TRAPS = setOf(
    DoushouqiPosition(8, 2),
    DoushouqiPosition(8, 4),
    DoushouqiPosition(7, 3),
)

private val ALL_TRAPS = VERMILION_TRAPS + GREEN_TRAPS

private val DENS = setOf(
    DoushouqiPosition(0, 3),
    DoushouqiPosition(8, 3),
)
