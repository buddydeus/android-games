package com.buddygames.xiangqi

import kotlin.math.abs

internal const val XIANGQI_SEARCH_NO_MOVE = -1

internal fun encodeSearchMove(move: XiangqiMove): Int =
    ((move.fromRow * XiangqiState.COLS + move.fromCol) shl 7) or
        (move.toRow * XiangqiState.COLS + move.toCol)

internal fun decodeSearchMove(move: Int): XiangqiMove {
    val from = move ushr 7
    val to = move and 0x7f
    return XiangqiMove(
        fromRow = from / XiangqiState.COLS,
        fromCol = from % XiangqiState.COLS,
        toRow = to / XiangqiState.COLS,
        toCol = to % XiangqiState.COLS
    )
}

internal class XiangqiSearchPosition private constructor(
    private val board: IntArray,
    private var redGeneral: Int,
    private var blackGeneral: Int,
    var hash: Long
) {
    private var validateGeneratedMoves = true

    companion object {
        private const val MAX_LEGAL_MOVES = 256
        private const val CHECK_PENALTY = 8_000
        private const val MOBILITY_WEIGHT = 4
        private const val THREAT_WEIGHT = 18
        private const val GENERAL_SHIELD_WEIGHT = 120
        private const val SUPPORTED_ATTACKER_WEIGHT = 6
        private const val ACTIVITY_PRESSURE_SHIFT = 10
        private const val ACTIVITY_SUPPORT_SHIFT = 32
        private const val ACTIVITY_MOBILITY_MASK = 0x3ffL
        private const val ACTIVITY_PRESSURE_MASK = 0x3fffffL

        private val ORTHOGONAL_OFFSETS = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        private val DIAGONAL_ONE_OFFSETS = arrayOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        private val DIAGONAL_TWO_OFFSETS = arrayOf(-2 to -2, -2 to 2, 2 to -2, 2 to 2)
        private val HORSE_OFFSETS = arrayOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        private val SHIELD_TYPES = setOf(PieceType.ADVISOR, PieceType.ELEPHANT)

        fun from(state: XiangqiState): XiangqiSearchPosition {
            val board = IntArray(XiangqiState.ROWS * XiangqiState.COLS)
            var redGeneral = -1
            var blackGeneral = -1
            var hash = 0L
            state.board.forEachIndexed { row, cells ->
                cells.forEachIndexed { col, piece ->
                    if (piece == null) return@forEachIndexed
                    val index = index(row, col)
                    val code = encodePiece(piece)
                    board[index] = code
                    hash = hash xor zobrist(index, code)
                    if (piece.type == PieceType.GENERAL) {
                        if (piece.side == Side.RED) redGeneral = index else blackGeneral = index
                    }
                }
            }
            return XiangqiSearchPosition(board, redGeneral, blackGeneral, hash)
        }

        private fun index(row: Int, col: Int): Int = row * XiangqiState.COLS + col

        private fun encodePiece(piece: XiangqiPiece): Int {
            val value = piece.type.ordinal + 1
            return if (piece.side == Side.RED) value else -value
        }

        private fun decodePiece(code: Int): XiangqiPiece? {
            if (code == 0) return null
            return XiangqiPiece(
                side = if (code > 0) Side.RED else Side.BLACK,
                type = PieceType.entries[abs(code) - 1]
            )
        }

        private fun zobrist(index: Int, code: Int): Long {
            var value = index.toLong() * -7046029254386353131L +
                (code + PieceType.entries.size + 1).toLong() * -4658895280553007687L
            value = (value xor (value ushr 30)) * -4658895280553007687L
            value = (value xor (value ushr 27)) * -7723592293110705685L
            return value xor (value ushr 31)
        }
    }

    fun makeMove(move: Int): Int {
        val from = move ushr 7
        val to = move and 0x7f
        val moving = board[from]
        require(moving != 0) { "Search move must have a moving piece" }
        val captured = board[to]
        hash = hash xor zobrist(from, moving)
        if (captured != 0) hash = hash xor zobrist(to, captured)
        hash = hash xor zobrist(to, moving)
        board[from] = 0
        board[to] = moving
        updateGeneralsAfterMove(moving, captured, to)
        return captured
    }

    fun unmakeMove(move: Int, captured: Int) {
        val from = move ushr 7
        val to = move and 0x7f
        val moving = board[to]
        hash = hash xor zobrist(to, moving)
        if (captured != 0) hash = hash xor zobrist(to, captured)
        hash = hash xor zobrist(from, moving)
        board[from] = moving
        board[to] = captured
        if (pieceType(moving) == PieceType.GENERAL) {
            setGeneral(sideOf(moving), from)
        }
        if (captured != 0 && pieceType(captured) == PieceType.GENERAL) {
            setGeneral(sideOf(captured), to)
        }
    }

    fun legalMoves(side: Side): List<Int> {
        val buffer = IntArray(MAX_LEGAL_MOVES)
        val count = generateLegalMoves(side, buffer)
        return List(count) { buffer[it] }
    }

    fun generateLegalMoves(
        side: Side,
        buffer: IntArray,
        capturesOnly: Boolean = false
    ): Int = generateMoves(side, buffer, capturesOnly, validateSelfCheck = true)

    private fun generatePseudoMoves(
        side: Side,
        buffer: IntArray
    ): Int = generateMoves(side, buffer, capturesOnly = false, validateSelfCheck = false)

    private fun generateMoves(
        side: Side,
        buffer: IntArray,
        capturesOnly: Boolean,
        validateSelfCheck: Boolean
    ): Int {
        val previousValidation = validateGeneratedMoves
        validateGeneratedMoves = validateSelfCheck
        return try {
            var count = 0
            for (from in board.indices) {
                val code = board[from]
                if (code == 0 || sideOf(code) != side) continue
                val row = from / XiangqiState.COLS
                val col = from % XiangqiState.COLS
                count = when (pieceType(code)) {
                PieceType.ROOK -> generateRayMoves(
                    from, row, col, side, buffer, count, cannon = false, capturesOnly
                )
                PieceType.CANNON -> generateRayMoves(
                    from, row, col, side, buffer, count, cannon = true, capturesOnly
                )
                PieceType.HORSE -> {
                    var next = count
                    HORSE_OFFSETS.forEach { (dr, dc) ->
                        val targetRow = row + dr
                        val targetCol = col + dc
                        val leg = when {
                            !inside(targetRow, targetCol) -> -1
                            abs(dr) == 2 -> index(row + dr / 2, col)
                            else -> index(row, col + dc / 2)
                        }
                        if (leg >= 0 && board[leg] == 0) {
                            next = addLegalMove(
                                from, targetRow, targetCol, side, buffer, next, capturesOnly
                            )
                        }
                    }
                    next
                }
                PieceType.ELEPHANT -> {
                    var next = count
                    DIAGONAL_TWO_OFFSETS.forEach { (dr, dc) ->
                        val targetRow = row + dr
                        val targetCol = col + dc
                        if (
                            targetRow in 0 until XiangqiState.ROWS &&
                            targetCol in 0 until XiangqiState.COLS &&
                            elephantOnOwnSide(targetRow, side) &&
                            board[index(row + dr / 2, col + dc / 2)] == 0
                        ) {
                            next = addLegalMove(
                                from, targetRow, targetCol, side, buffer, next, capturesOnly
                            )
                        }
                    }
                    next
                }
                PieceType.ADVISOR -> {
                    var next = count
                    DIAGONAL_ONE_OFFSETS.forEach { (dr, dc) ->
                        val targetRow = row + dr
                        val targetCol = col + dc
                        if (inPalace(targetRow, targetCol, side)) {
                            next = addLegalMove(
                                from, targetRow, targetCol, side, buffer, next, capturesOnly
                            )
                        }
                    }
                    next
                }
                PieceType.GENERAL -> generateGeneralMoves(
                    from, row, col, side, buffer, count, capturesOnly
                )
                PieceType.SOLDIER -> generateSoldierMoves(
                    from, row, col, side, buffer, count, capturesOnly
                )
                }
            }
            count
        } finally {
            validateGeneratedMoves = previousValidation
        }
    }

    fun isInCheck(side: Side): Boolean {
        val general = if (side == Side.RED) redGeneral else blackGeneral
        if (general < 0) return false
        val opponent = side.other()
        for (from in board.indices) {
            val code = board[from]
            if (code != 0 && sideOf(code) == opponent && attacks(from, general, code)) {
                return true
            }
        }
        return false
    }

    fun hasGeneral(side: Side): Boolean =
        if (side == Side.RED) redGeneral >= 0 else blackGeneral >= 0

    fun generalIndex(side: Side): Int =
        if (side == Side.RED) redGeneral else blackGeneral

    fun pieceCodeAt(index: Int): Int = board[index]

    fun pieceValueAt(index: Int): Int {
        val code = board[index]
        return if (code == 0) 0 else pieceType(code).value
    }

    fun movingPieceValue(move: Int): Int = pieceValueAt(move ushr 7)

    fun capturedPieceValue(move: Int): Int = pieceValueAt(move and 0x7f)

    fun evaluate(
        side: Side,
        profile: XiangqiEvaluationProfile,
        scratchMoves: IntArray = IntArray(MAX_LEGAL_MOVES)
    ): Int {
        var score = 0
        val pieceCount = board.count { it != 0 }
        for (index in board.indices) {
            val code = board[index]
            if (code == 0) continue
            val pieceSide = sideOf(code)
            val direction = if (pieceSide == side) 1 else -1
            val type = pieceType(code)
            score += direction * type.value * 100
            if (profile >= XiangqiEvaluationProfile.BASIC_POSITIONAL) {
                score += direction * positionalValue(type, pieceSide, index, pieceCount)
            }
        }
        val ownActivity = if (profile >= XiangqiEvaluationProfile.MATERIAL_AND_SAFETY) {
            activity(side, scratchMoves)
        } else {
            0L
        }
        val opponentActivity = if (
            profile >= XiangqiEvaluationProfile.MATERIAL_AND_SAFETY
        ) {
            activity(side.other(), scratchMoves)
        } else {
            0L
        }
        if (profile >= XiangqiEvaluationProfile.MATERIAL_AND_SAFETY) {
            if (isInCheck(side)) score -= CHECK_PENALTY
            if (isInCheck(side.other())) score += CHECK_PENALTY
            score += activityPressure(ownActivity) - activityPressure(opponentActivity)
        }
        if (profile >= XiangqiEvaluationProfile.FULL_POSITIONAL) {
            score += (
                activityMobility(ownActivity) - activityMobility(opponentActivity)
                ) * MOBILITY_WEIGHT
            score += generalShield(side) - generalShield(side.other())
            score += activitySupport(ownActivity) - activitySupport(opponentActivity)
        }
        return score
    }

    fun supportedAttackerScore(side: Side): Int =
        activitySupport(activity(side, IntArray(MAX_LEGAL_MOVES)))

    fun toState(): XiangqiState = XiangqiState(
        List(XiangqiState.ROWS) { row ->
            List(XiangqiState.COLS) { col ->
                decodePiece(board[index(row, col)])
            }
        }
    )

    private fun updateGeneralsAfterMove(moving: Int, captured: Int, to: Int) {
        if (pieceType(moving) == PieceType.GENERAL) {
            setGeneral(sideOf(moving), to)
        }
        if (captured != 0 && pieceType(captured) == PieceType.GENERAL) {
            setGeneral(sideOf(captured), -1)
        }
    }

    private fun setGeneral(side: Side, index: Int) {
        if (side == Side.RED) redGeneral = index else blackGeneral = index
    }

    private fun generateRayMoves(
        from: Int,
        row: Int,
        col: Int,
        side: Side,
        buffer: IntArray,
        initialCount: Int,
        cannon: Boolean,
        capturesOnly: Boolean
    ): Int {
        var count = initialCount
        ORTHOGONAL_OFFSETS.forEach { (rowStep, colStep) ->
            var targetRow = row + rowStep
            var targetCol = col + colStep
            var screenFound = false
            while (inside(targetRow, targetCol)) {
                val target = board[index(targetRow, targetCol)]
                if (!cannon) {
                    if (target == 0) {
                        if (!capturesOnly) {
                            count = addLegalMove(
                                from, targetRow, targetCol, side, buffer, count, capturesOnly
                            )
                        }
                    } else {
                        if (sideOf(target) != side) {
                            count = addLegalMove(
                                from, targetRow, targetCol, side, buffer, count, capturesOnly
                            )
                        }
                        break
                    }
                } else if (!screenFound) {
                    if (target == 0) {
                        if (!capturesOnly) {
                            count = addLegalMove(
                                from, targetRow, targetCol, side, buffer, count, capturesOnly
                            )
                        }
                    } else {
                        screenFound = true
                    }
                } else if (target != 0) {
                    if (sideOf(target) != side) {
                        count = addLegalMove(
                            from, targetRow, targetCol, side, buffer, count, capturesOnly
                        )
                    }
                    break
                }
                targetRow += rowStep
                targetCol += colStep
            }
        }
        return count
    }

    private fun generateGeneralMoves(
        from: Int,
        row: Int,
        col: Int,
        side: Side,
        buffer: IntArray,
        initialCount: Int,
        capturesOnly: Boolean
    ): Int {
        var count = initialCount
        ORTHOGONAL_OFFSETS.forEach { (dr, dc) ->
            val targetRow = row + dr
            val targetCol = col + dc
            if (inPalace(targetRow, targetCol, side)) {
                count = addLegalMove(
                    from, targetRow, targetCol, side, buffer, count, capturesOnly
                )
            }
        }
        for (direction in intArrayOf(-1, 1)) {
            var targetRow = row + direction
            while (targetRow in 0 until XiangqiState.ROWS) {
                val target = board[index(targetRow, col)]
                if (target != 0) {
                    if (sideOf(target) != side && pieceType(target) == PieceType.GENERAL) {
                        count = addLegalMove(
                            from, targetRow, col, side, buffer, count, capturesOnly
                        )
                    }
                    break
                }
                targetRow += direction
            }
        }
        return count
    }

    private fun generateSoldierMoves(
        from: Int,
        row: Int,
        col: Int,
        side: Side,
        buffer: IntArray,
        initialCount: Int,
        capturesOnly: Boolean
    ): Int {
        var count = addLegalMove(
            from,
            row + if (side == Side.RED) -1 else 1,
            col,
            side,
            buffer,
            initialCount,
            capturesOnly
        )
        val crossedRiver = if (side == Side.RED) row <= 4 else row >= 5
        if (crossedRiver) {
            count = addLegalMove(from, row, col - 1, side, buffer, count, capturesOnly)
            count = addLegalMove(from, row, col + 1, side, buffer, count, capturesOnly)
        }
        return count
    }

    private fun addLegalMove(
        from: Int,
        toRow: Int,
        toCol: Int,
        side: Side,
        buffer: IntArray,
        count: Int,
        capturesOnly: Boolean
    ): Int {
        if (!inside(toRow, toCol)) return count
        val to = index(toRow, toCol)
        val target = board[to]
        if (target != 0 && sideOf(target) == side) return count
        if (capturesOnly && target == 0) return count
        val move = (from shl 7) or to
        if (validateGeneratedMoves) {
            val captured = makeMove(move)
            val legal = !isInCheck(side)
            unmakeMove(move, captured)
            if (!legal) return count
        }
        require(count < buffer.size) { "Xiangqi search move buffer is too small" }
        buffer[count] = move
        return count + 1
    }

    private fun attacks(from: Int, to: Int, code: Int): Boolean {
        val fromRow = from / XiangqiState.COLS
        val fromCol = from % XiangqiState.COLS
        val toRow = to / XiangqiState.COLS
        val toCol = to % XiangqiState.COLS
        val dr = toRow - fromRow
        val dc = toCol - fromCol
        val side = sideOf(code)
        return when (pieceType(code)) {
            PieceType.ROOK -> (dr == 0 || dc == 0) && screensBetween(from, to) == 0
            PieceType.CANNON -> (dr == 0 || dc == 0) && screensBetween(from, to) == 1
            PieceType.HORSE -> {
                val horseMove = abs(dr) * abs(dc) == 2
                if (!horseMove) {
                    false
                } else {
                    val leg = if (abs(dr) == 2) {
                        index(fromRow + dr / 2, fromCol)
                    } else {
                        index(fromRow, fromCol + dc / 2)
                    }
                    board[leg] == 0
                }
            }
            PieceType.ELEPHANT ->
                abs(dr) == 2 && abs(dc) == 2 &&
                    elephantOnOwnSide(toRow, side) &&
                    board[index(fromRow + dr / 2, fromCol + dc / 2)] == 0
            PieceType.ADVISOR ->
                abs(dr) == 1 && abs(dc) == 1 && inPalace(toRow, toCol, side)
            PieceType.GENERAL ->
                (abs(dr) + abs(dc) == 1 && inPalace(toRow, toCol, side)) ||
                    (dc == 0 && screensBetween(from, to) == 0)
            PieceType.SOLDIER -> {
                val forward = if (side == Side.RED) -1 else 1
                (dr == forward && dc == 0) ||
                    ((if (side == Side.RED) fromRow <= 4 else fromRow >= 5) &&
                        dr == 0 && abs(dc) == 1)
            }
        }
    }

    private fun positionalValue(
        type: PieceType,
        side: Side,
        index: Int,
        pieceCount: Int
    ): Int {
        val row = index / XiangqiState.COLS
        val col = index % XiangqiState.COLS
        val centerDistance = abs(col - 4)
        val advancement = if (side == Side.RED) 9 - row else row
        val endgame = pieceCount <= 12
        return when (type) {
            PieceType.SOLDIER -> advancement * (if (endgame) 16 else 12) - centerDistance * 2
            PieceType.HORSE -> (if (endgame) 36 else 30) - centerDistance * 5
            PieceType.CANNON -> (if (endgame) 12 else 20) - centerDistance * 3
            PieceType.ROOK -> advancement * if (endgame) 3 else 2
            PieceType.GENERAL -> if (endgame) 12 - centerDistance * 3 else -centerDistance * 4
            PieceType.ADVISOR,
            PieceType.ELEPHANT -> 0
        }
    }

    private fun activity(side: Side, scratchMoves: IntArray): Long {
        val mobility = generatePseudoMoves(side, scratchMoves)
        var pressure = 0
        var support = 0
        var previousFrom = -1
        var previousAttacks = false
        for (index in 0 until mobility) {
            val move = scratchMoves[index]
            val from = move ushr 7
            if (from != previousFrom) {
                if (previousAttacks && isDefended(previousFrom, side)) {
                    support += pieceValueAt(previousFrom) * SUPPORTED_ATTACKER_WEIGHT
                }
                previousFrom = from
                previousAttacks = false
            }
            val capturedValue = capturedPieceValue(move)
            if (capturedValue > 0) {
                pressure += capturedValue * THREAT_WEIGHT
                previousAttacks = true
            }
        }
        if (previousAttacks && isDefended(previousFrom, side)) {
            support += pieceValueAt(previousFrom) * SUPPORTED_ATTACKER_WEIGHT
        }
        return mobility.toLong() or
            (pressure.toLong() shl ACTIVITY_PRESSURE_SHIFT) or
            (support.toLong() shl ACTIVITY_SUPPORT_SHIFT)
    }

    private fun activityMobility(activity: Long): Int =
        (activity and ACTIVITY_MOBILITY_MASK).toInt()

    private fun activityPressure(activity: Long): Int =
        ((activity ushr ACTIVITY_PRESSURE_SHIFT) and ACTIVITY_PRESSURE_MASK).toInt()

    private fun activitySupport(activity: Long): Int =
        (activity ushr ACTIVITY_SUPPORT_SHIFT).toInt()

    private fun generalShield(side: Side): Int {
        val general = generalIndex(side)
        if (general < 0) return 0
        val row = general / XiangqiState.COLS
        val col = general % XiangqiState.COLS
        var score = 0
        for (targetRow in (row - 1)..(row + 1)) {
            for (targetCol in (col - 1)..(col + 1)) {
                if (!inside(targetRow, targetCol)) continue
                val code = board[index(targetRow, targetCol)]
                if (
                    code != 0 &&
                    sideOf(code) == side &&
                    pieceType(code) in SHIELD_TYPES
                ) {
                    score += GENERAL_SHIELD_WEIGHT
                }
            }
        }
        return score
    }

    private fun isDefended(target: Int, side: Side): Boolean {
        for (from in board.indices) {
            val defender = board[from]
            if (
                from != target &&
                defender != 0 &&
                sideOf(defender) == side &&
                attacks(from, target, defender)
            ) {
                return true
            }
        }
        return false
    }

    private fun screensBetween(from: Int, to: Int): Int {
        val fromRow = from / XiangqiState.COLS
        val fromCol = from % XiangqiState.COLS
        val toRow = to / XiangqiState.COLS
        val toCol = to % XiangqiState.COLS
        val rowStep = (toRow - fromRow).sign()
        val colStep = (toCol - fromCol).sign()
        var row = fromRow + rowStep
        var col = fromCol + colStep
        var screens = 0
        while (row != toRow || col != toCol) {
            if (board[index(row, col)] != 0) screens++
            row += rowStep
            col += colStep
        }
        return screens
    }

    private fun inPalace(row: Int, col: Int, side: Side): Boolean {
        if (col !in 3..5) return false
        return if (side == Side.RED) row in 7..9 else row in 0..2
    }

    private fun elephantOnOwnSide(row: Int, side: Side): Boolean =
        if (side == Side.RED) row >= 5 else row <= 4

    private fun inside(row: Int, col: Int): Boolean =
        row in 0 until XiangqiState.ROWS && col in 0 until XiangqiState.COLS

    private fun sideOf(code: Int): Side = if (code > 0) Side.RED else Side.BLACK

    private fun pieceType(code: Int): PieceType = PieceType.entries[abs(code) - 1]

    private fun Int.sign(): Int = when {
        this < 0 -> -1
        this > 0 -> 1
        else -> 0
    }

}
