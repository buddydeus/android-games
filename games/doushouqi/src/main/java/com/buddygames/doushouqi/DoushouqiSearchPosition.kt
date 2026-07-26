package com.buddygames.doushouqi

internal class DoushouqiSearchPosition(state: DoushouqiState) {
    private val board = IntArray(DoushouqiState.SQUARES) { index ->
        encode(state.boardSnapshot()[index])
    }
    var sideToMove: DoushouqiSide = state.sideToMove
        private set
    var quietHalfMoves: Int = state.quietHalfMoves
        private set
    var positionKey: Long = state.positionKey
        private set
    var zobristKey: Long = computeZobrist()
        private set
    var lastMove: DoushouqiMove? = state.lastMove
        private set
    var result: DoushouqiResult? = state.result
        private set
    private val repetitions = state.repetitionCounts.toMutableMap()

    fun legalMoves(side: DoushouqiSide = sideToMove): List<DoushouqiMove> {
        if (result != null && side == sideToMove) return emptyList()
        val moves = ArrayList<DoushouqiMove>(32)
        board.indices.forEach { index ->
            val code = board[index]
            if (code == EMPTY || sideOf(code) != side) return@forEach
            val from = position(index)
            DIRECTIONS.forEach { (rowDelta, columnDelta) ->
                candidateMove(from, code, rowDelta, columnDelta)?.let(moves::add)
            }
        }
        moves.sortWith(MOVE_COMPARATOR)
        return moves
    }

    fun pieceAt(position: DoushouqiPosition): DoushouqiPiece? = decode(board[position.index])

    fun pieceSideAt(index: Int): DoushouqiSide? =
        board[index].takeIf { it != EMPTY }?.let(::sideOf)

    fun pieceAnimalAt(index: Int): DoushouqiAnimal? =
        board[index].takeIf { it != EMPTY }?.let(::animalOf)

    fun make(move: DoushouqiMove): Undo {
        val moving = board[move.from.index]
        require(moving != EMPTY)
        val captured = board[move.to.index]
        val undo = Undo(
            captured = captured,
            previousSide = sideToMove,
            previousQuiet = quietHalfMoves,
            previousPositionKey = positionKey,
            previousZobristKey = zobristKey,
            previousLastMove = lastMove,
            previousResult = result,
        )
        board[move.from.index] = EMPTY
        board[move.to.index] = moving
        sideToMove = sideToMove.other()
        quietHalfMoves = if (captured == EMPTY) quietHalfMoves + 1 else 0
        lastMove = move
        result = null
        zobristKey = undo.previousZobristKey xor
            zobristPiece(move.from.index, moving) xor
            zobristPiece(move.to.index, moving) xor
            (if (captured == EMPTY) 0L else zobristPiece(move.to.index, captured)) xor
            ZOBRIST_SIDE
        positionKey = computePositionKey()
        val previousCount = repetitions.getOrDefault(positionKey, 0)
        repetitions[positionKey] = previousCount + 1
        undo.recordedPositionKey = positionKey
        undo.previousRepetitionCount = previousCount
        result = adjudicate(moving, captured, move.to)
        return undo
    }

    fun unmake(move: DoushouqiMove, undo: Undo) {
        if (undo.previousRepetitionCount == 0) {
            repetitions.remove(undo.recordedPositionKey)
        } else {
            repetitions[undo.recordedPositionKey] = undo.previousRepetitionCount
        }
        board[move.from.index] = board[move.to.index]
        board[move.to.index] = undo.captured
        sideToMove = undo.previousSide
        quietHalfMoves = undo.previousQuiet
        positionKey = undo.previousPositionKey
        zobristKey = undo.previousZobristKey
        lastMove = undo.previousLastMove
        result = undo.previousResult
    }

    fun repetitionContextHash(): Long {
        var hash = 0L
        repetitions.entries.sortedBy { it.key }.forEach { (key, count) ->
            hash = stableMix(hash xor key xor count.toLong())
        }
        return hash
    }

    fun snapshot(): Snapshot = Snapshot(
        board = board.toList(),
        sideToMove = sideToMove,
        quietHalfMoves = quietHalfMoves,
        positionKey = positionKey,
        zobristKey = zobristKey,
        repetitions = repetitions.toMap(),
        lastMove = lastMove,
        result = result,
    )

    private fun adjudicate(
        moving: Int,
        captured: Int,
        destination: DoushouqiPosition,
    ): DoushouqiResult? {
        val mover = sideOf(moving)
        if (denOwner(destination) == mover.other()) {
            return DoushouqiResult.Win(mover, DoushouqiWinReason.DEN)
        }
        if (captured != EMPTY && board.none { it != EMPTY && sideOf(it) == mover.other() }) {
            return DoushouqiResult.Win(mover, DoushouqiWinReason.FINAL_CAPTURE)
        }
        if (legalMoves().isEmpty()) {
            return DoushouqiResult.Win(mover, DoushouqiWinReason.NO_LEGAL_MOVE)
        }
        if (repetitions.getOrDefault(positionKey, 0) >= 3) {
            return DoushouqiResult.Draw(DoushouqiDrawReason.REPETITION)
        }
        if (quietHalfMoves >= 100) {
            return DoushouqiResult.Draw(DoushouqiDrawReason.QUIET_100)
        }
        return null
    }

    private fun candidateMove(
        from: DoushouqiPosition,
        moving: Int,
        rowDelta: Int,
        columnDelta: Int,
    ): DoushouqiMove? {
        val adjacent = positionOrNull(from.row + rowDelta, from.column + columnDelta)
            ?: return null
        val animal = animalOf(moving)
        val destination = if (
            terrainAt(adjacent) == DoushouqiTerrain.RIVER &&
            animal in JUMPING_ANIMALS
        ) {
            jumpDestination(adjacent, rowDelta, columnDelta)
        } else {
            adjacent
        } ?: return null
        val side = sideOf(moving)
        if (denOwner(destination) == side) return null
        if (
            terrainAt(destination) == DoushouqiTerrain.RIVER &&
            animal != DoushouqiAnimal.RAT
        ) {
            return null
        }
        val occupant = board[destination.index]
        if (occupant != EMPTY && sideOf(occupant) == side) return null
        if (occupant != EMPTY && !canCapture(moving, from, occupant, destination)) return null
        return DoushouqiMove(from, destination)
    }

    private fun jumpDestination(
        firstRiver: DoushouqiPosition,
        rowDelta: Int,
        columnDelta: Int,
    ): DoushouqiPosition? {
        var current = firstRiver
        while (terrainAt(current) == DoushouqiTerrain.RIVER) {
            val occupant = board[current.index]
            if (occupant != EMPTY && animalOf(occupant) == DoushouqiAnimal.RAT) return null
            current = positionOrNull(
                current.row + rowDelta,
                current.column + columnDelta,
            ) ?: return null
        }
        return current
    }

    private fun canCapture(
        attacker: Int,
        from: DoushouqiPosition,
        defender: Int,
        to: DoushouqiPosition,
    ): Boolean {
        val attackerSide = sideOf(attacker)
        val defenderSide = sideOf(defender)
        if (attackerSide == defenderSide) return false
        val attackerAnimal = animalOf(attacker)
        val defenderAnimal = animalOf(defender)
        val fromIsRiver = terrainAt(from) == DoushouqiTerrain.RIVER
        val toIsRiver = terrainAt(to) == DoushouqiTerrain.RIVER
        if (
            (attackerAnimal == DoushouqiAnimal.RAT ||
                defenderAnimal == DoushouqiAnimal.RAT) &&
            fromIsRiver != toIsRiver
        ) {
            return false
        }
        if (trapOwner(to) == attackerSide) return true
        if (
            attackerAnimal == DoushouqiAnimal.RAT &&
            defenderAnimal == DoushouqiAnimal.ELEPHANT
        ) {
            return !fromIsRiver && !toIsRiver
        }
        if (
            attackerAnimal == DoushouqiAnimal.ELEPHANT &&
            defenderAnimal == DoushouqiAnimal.RAT
        ) {
            return false
        }
        return attackerAnimal.rank >= defenderAnimal.rank
    }

    private fun computePositionKey(): Long {
        var hash = -0x340d631b7bdddcdbL
        board.forEachIndexed { index, code ->
            hash = (hash xor (index * 17L + code)) * 0x100000001b3L
        }
        return (hash xor sideToMove.ordinal.toLong()) * 0x100000001b3L
    }

    private fun computeZobrist(): Long {
        var hash = if (sideToMove == DoushouqiSide.VERMILION) ZOBRIST_SIDE else 0L
        board.forEachIndexed { index, code ->
            if (code != EMPTY) hash = hash xor zobristPiece(index, code)
        }
        return hash
    }

    internal data class Snapshot(
        val board: List<Int>,
        val sideToMove: DoushouqiSide,
        val quietHalfMoves: Int,
        val positionKey: Long,
        val zobristKey: Long,
        val repetitions: Map<Long, Int>,
        val lastMove: DoushouqiMove?,
        val result: DoushouqiResult?,
    )

    internal class Undo(
        val captured: Int,
        val previousSide: DoushouqiSide,
        val previousQuiet: Int,
        val previousPositionKey: Long,
        val previousZobristKey: Long,
        val previousLastMove: DoushouqiMove?,
        val previousResult: DoushouqiResult?,
    ) {
        var recordedPositionKey: Long = 0L
        var previousRepetitionCount: Int = 0
    }

    companion object {
        private const val EMPTY = 0
        private val DIRECTIONS = listOf(-1 to 0, 0 to -1, 0 to 1, 1 to 0)
        private val JUMPING_ANIMALS = setOf(DoushouqiAnimal.LION, DoushouqiAnimal.TIGER)
        private val MOVE_COMPARATOR = compareBy<DoushouqiMove>(
            { it.from.row },
            { it.from.column },
            { it.to.row },
            { it.to.column },
        )
        private val ZOBRIST_SIDE = stableMix(0x5A17L)

        private fun encode(piece: DoushouqiPiece?): Int = piece?.let {
            1 + it.side.ordinal * DoushouqiAnimal.entries.size + it.animal.ordinal
        } ?: EMPTY

        private fun decode(code: Int): DoushouqiPiece? {
            if (code == EMPTY) return null
            val normalized = code - 1
            return DoushouqiPiece(
                DoushouqiSide.entries[normalized / DoushouqiAnimal.entries.size],
                DoushouqiAnimal.entries[normalized % DoushouqiAnimal.entries.size],
            )
        }

        private fun sideOf(code: Int): DoushouqiSide =
            DoushouqiSide.entries[(code - 1) / DoushouqiAnimal.entries.size]

        private fun animalOf(code: Int): DoushouqiAnimal =
            DoushouqiAnimal.entries[(code - 1) % DoushouqiAnimal.entries.size]

        private fun position(index: Int) = DoushouqiPosition(
            index / DoushouqiState.COLUMNS,
            index % DoushouqiState.COLUMNS,
        )

        private fun positionOrNull(row: Int, column: Int): DoushouqiPosition? =
            if (row in 0 until DoushouqiState.ROWS &&
                column in 0 until DoushouqiState.COLUMNS
            ) {
                DoushouqiPosition(row, column)
            } else {
                null
            }

        private fun zobristPiece(index: Int, code: Int): Long =
            stableMix((index.toLong() shl 8) xor code.toLong() xor 0xD05A0L)

        private fun stableMix(value: Long): Long {
            var mixed = value
            mixed = (mixed xor (mixed ushr 30)) * -4658895280553007687L
            mixed = (mixed xor (mixed ushr 27)) * -7723592293110705685L
            return mixed xor (mixed ushr 31)
        }
    }
}
