package com.buddygames.chess

import kotlin.math.abs

enum class ChessSide {
    WHITE,
    BLACK;

    fun other(): ChessSide = if (this == WHITE) BLACK else WHITE
}

enum class ChessPieceType(val value: Int) {
    KING(20_000),
    QUEEN(900),
    ROOK(500),
    BISHOP(330),
    KNIGHT(320),
    PAWN(100)
}

data class ChessPiece(val side: ChessSide, val type: ChessPieceType)

data class ChessMove(
    val from: Int,
    val to: Int,
    val promotion: ChessPieceType? = null
) {
    companion object {
        fun fromUci(uci: String): ChessMove {
            require(uci.length in 4..5) { "Invalid UCI move: $uci" }
            val promotion = uci.getOrNull(4)?.let {
                when (it.lowercaseChar()) {
                    'q' -> ChessPieceType.QUEEN
                    'r' -> ChessPieceType.ROOK
                    'b' -> ChessPieceType.BISHOP
                    'n' -> ChessPieceType.KNIGHT
                    else -> error("Invalid promotion piece: $it")
                }
            }
            return ChessMove(
                from = chessSquare(uci.substring(0, 2)),
                to = chessSquare(uci.substring(2, 4)),
                promotion = promotion
            )
        }
    }

    fun toUci(): String = buildString {
        append(chessSquareName(from))
        append(chessSquareName(to))
        promotion?.let {
            append(
                when (it) {
                    ChessPieceType.QUEEN -> 'q'
                    ChessPieceType.ROOK -> 'r'
                    ChessPieceType.BISHOP -> 'b'
                    ChessPieceType.KNIGHT -> 'n'
                    else -> error("Invalid promotion type: $it")
                }
            )
        }
    }
}

data class ChessCastlingRights(
    val whiteKingSide: Boolean = false,
    val whiteQueenSide: Boolean = false,
    val blackKingSide: Boolean = false,
    val blackQueenSide: Boolean = false
)

data class ChessState(
    val board: List<ChessPiece?>,
    val sideToMove: ChessSide,
    val castlingRights: ChessCastlingRights,
    val enPassantSquare: Int?,
    val halfMoveClock: Int,
    val fullMoveNumber: Int
) {
    init {
        require(board.size == BOARD_SQUARES)
        require(enPassantSquare == null || enPassantSquare in 0 until BOARD_SQUARES)
    }

    fun pieceAt(square: Int): ChessPiece? = board.getOrNull(square)

    fun pieceAt(name: String): ChessPiece? = pieceAt(chessSquare(name))

    fun put(square: Int, piece: ChessPiece?): ChessState {
        require(square in 0 until BOARD_SQUARES)
        return copy(board = board.toMutableList().also { it[square] = piece })
    }

    fun put(name: String, piece: ChessPiece?): ChessState = put(chessSquare(name), piece)

    fun apply(move: ChessMove): ChessState {
        val legalMove = ChessRules.resolveLegalMove(this, move)
        return applyUnchecked(legalMove)
    }

    internal fun applyUnchecked(move: ChessMove): ChessState {
        require(move.from in 0 until BOARD_SQUARES && move.to in 0 until BOARD_SQUARES)
        val moving = requireNotNull(board[move.from]) { "No piece at ${chessSquareName(move.from)}" }
        val nextBoard = board.toMutableList()
        val captured = nextBoard[move.to]
        nextBoard[move.from] = null

        val isEnPassant = moving.type == ChessPieceType.PAWN &&
            move.to == enPassantSquare &&
            captured == null &&
            fileOf(move.from) != fileOf(move.to)
        if (isEnPassant) {
            val capturedSquare = move.to + if (moving.side == ChessSide.WHITE) -8 else 8
            nextBoard[capturedSquare] = null
        }

        val placed = if (moving.type == ChessPieceType.PAWN && rankOf(move.to) in setOf(0, 7)) {
            ChessPiece(moving.side, move.promotion ?: ChessPieceType.QUEEN)
        } else {
            moving
        }
        nextBoard[move.to] = placed

        if (moving.type == ChessPieceType.KING && abs(fileOf(move.to) - fileOf(move.from)) == 2) {
            val kingSide = fileOf(move.to) == 6
            val rookFrom = rankOf(move.from) * 8 + if (kingSide) 7 else 0
            val rookTo = rankOf(move.from) * 8 + if (kingSide) 5 else 3
            nextBoard[rookTo] = nextBoard[rookFrom]
            nextBoard[rookFrom] = null
        }

        val nextEnPassant = if (
            moving.type == ChessPieceType.PAWN &&
            abs(rankOf(move.to) - rankOf(move.from)) == 2
        ) {
            (move.from + move.to) / 2
        } else {
            null
        }
        val isCapture = captured != null || isEnPassant
        return ChessState(
            board = nextBoard,
            sideToMove = sideToMove.other(),
            castlingRights = updatedCastlingRights(move, moving, captured),
            enPassantSquare = nextEnPassant,
            halfMoveClock = if (moving.type == ChessPieceType.PAWN || isCapture) 0 else halfMoveClock + 1,
            fullMoveNumber = fullMoveNumber + if (sideToMove == ChessSide.BLACK) 1 else 0
        )
    }

    private fun updatedCastlingRights(
        move: ChessMove,
        moving: ChessPiece,
        captured: ChessPiece?
    ): ChessCastlingRights {
        var rights = castlingRights
        if (moving.type == ChessPieceType.KING) {
            rights = if (moving.side == ChessSide.WHITE) {
                rights.copy(whiteKingSide = false, whiteQueenSide = false)
            } else {
                rights.copy(blackKingSide = false, blackQueenSide = false)
            }
        }
        if (moving.type == ChessPieceType.ROOK) {
            rights = rights.withoutRookAt(move.from)
        }
        if (captured?.type == ChessPieceType.ROOK) {
            rights = rights.withoutRookAt(move.to)
        }
        return rights
    }

    companion object {
        fun empty(
            sideToMove: ChessSide = ChessSide.WHITE,
            castlingRights: ChessCastlingRights = ChessCastlingRights(),
            enPassantSquare: Int? = null,
            halfMoveClock: Int = 0,
            fullMoveNumber: Int = 1
        ): ChessState = ChessState(
            board = List(BOARD_SQUARES) { null },
            sideToMove = sideToMove,
            castlingRights = castlingRights,
            enPassantSquare = enPassantSquare,
            halfMoveClock = halfMoveClock,
            fullMoveNumber = fullMoveNumber
        )

        fun initial(): ChessState {
            val board = MutableList<ChessPiece?>(BOARD_SQUARES) { null }
            val backRank = listOf(
                ChessPieceType.ROOK,
                ChessPieceType.KNIGHT,
                ChessPieceType.BISHOP,
                ChessPieceType.QUEEN,
                ChessPieceType.KING,
                ChessPieceType.BISHOP,
                ChessPieceType.KNIGHT,
                ChessPieceType.ROOK
            )
            for (file in 0..7) {
                board[file] = ChessPiece(ChessSide.WHITE, backRank[file])
                board[8 + file] = ChessPiece(ChessSide.WHITE, ChessPieceType.PAWN)
                board[48 + file] = ChessPiece(ChessSide.BLACK, ChessPieceType.PAWN)
                board[56 + file] = ChessPiece(ChessSide.BLACK, backRank[file])
            }
            return ChessState(
                board = board,
                sideToMove = ChessSide.WHITE,
                castlingRights = ChessCastlingRights(
                    whiteKingSide = true,
                    whiteQueenSide = true,
                    blackKingSide = true,
                    blackQueenSide = true
                ),
                enPassantSquare = null,
                halfMoveClock = 0,
                fullMoveNumber = 1
            )
        }
    }
}

enum class ChessResult {
    WHITE_WIN,
    BLACK_WIN,
    DRAW_STALEMATE,
    DRAW_FIFTY_MOVE,
    DRAW_REPETITION,
    DRAW_INSUFFICIENT_MATERIAL;

    val winner: ChessSide?
        get() = when (this) {
            WHITE_WIN -> ChessSide.WHITE
            BLACK_WIN -> ChessSide.BLACK
            else -> null
        }
}

object ChessRules {
    fun legalMoves(state: ChessState): List<ChessMove> {
        val side = state.sideToMove
        return pseudoLegalMoves(state, side).filter { move ->
            !isInCheck(state.applyUnchecked(move), side)
        }
    }

    internal fun resolveLegalMove(state: ChessState, requested: ChessMove): ChessMove {
        require(requested.from in 0 until BOARD_SQUARES && requested.to in 0 until BOARD_SQUARES) {
            "Move squares must be on the board"
        }
        require(requested.promotion == null || requested.promotion in PROMOTION_TYPES) {
            "Promotion must be queen, rook, bishop, or knight"
        }
        val moving = state.board[requested.from]
        val normalized = if (
            moving?.type == ChessPieceType.PAWN &&
            rankOf(requested.to) in setOf(0, 7) &&
            requested.promotion == null
        ) {
            requested.copy(promotion = ChessPieceType.QUEEN)
        } else {
            requested
        }
        return requireNotNull(legalMoves(state).firstOrNull { it == normalized }) {
            "Illegal chess move: ${requested.toUci()}"
        }
    }

    fun isInCheck(state: ChessState, side: ChessSide): Boolean {
        val kingSquare = state.board.indexOfFirst {
            it == ChessPiece(side, ChessPieceType.KING)
        }
        return kingSquare < 0 || isSquareAttacked(state, kingSquare, side.other())
    }

    fun isSquareAttacked(state: ChessState, square: Int, bySide: ChessSide): Boolean {
        val targetFile = fileOf(square)
        val targetRank = rankOf(square)

        val pawnOriginRank = targetRank + if (bySide == ChessSide.WHITE) -1 else 1
        for (fileDelta in listOf(-1, 1)) {
            val originFile = targetFile + fileDelta
            if (
                originFile in 0..7 &&
                pawnOriginRank in 0..7 &&
                state.board[pawnOriginRank * 8 + originFile] ==
                ChessPiece(bySide, ChessPieceType.PAWN)
            ) {
                return true
            }
        }

        for ((fileDelta, rankDelta) in KNIGHT_DELTAS) {
            val file = targetFile + fileDelta
            val rank = targetRank + rankDelta
            if (
                file in 0..7 &&
                rank in 0..7 &&
                state.board[rank * 8 + file] == ChessPiece(bySide, ChessPieceType.KNIGHT)
            ) {
                return true
            }
        }

        for ((fileDelta, rankDelta) in KING_DELTAS) {
            val file = targetFile + fileDelta
            val rank = targetRank + rankDelta
            if (
                file in 0..7 &&
                rank in 0..7 &&
                state.board[rank * 8 + file] == ChessPiece(bySide, ChessPieceType.KING)
            ) {
                return true
            }
        }

        if (attackedOnRay(state, square, bySide, ORTHOGONAL_DELTAS, ChessPieceType.ROOK)) {
            return true
        }
        return attackedOnRay(state, square, bySide, DIAGONAL_DELTAS, ChessPieceType.BISHOP)
    }

    fun result(state: ChessState, repetitions: Int = 1): ChessResult? {
        val legal = legalMoves(state)
        if (legal.isEmpty()) {
            return if (isInCheck(state, state.sideToMove)) {
                if (state.sideToMove == ChessSide.WHITE) {
                    ChessResult.BLACK_WIN
                } else {
                    ChessResult.WHITE_WIN
                }
            } else {
                ChessResult.DRAW_STALEMATE
            }
        }
        if (state.halfMoveClock >= 100) return ChessResult.DRAW_FIFTY_MOVE
        if (repetitions >= 3) return ChessResult.DRAW_REPETITION
        if (hasInsufficientMaterial(state)) return ChessResult.DRAW_INSUFFICIENT_MATERIAL
        return null
    }

    private fun pseudoLegalMoves(state: ChessState, side: ChessSide): List<ChessMove> {
        val moves = ArrayList<ChessMove>(64)
        state.board.forEachIndexed { from, piece ->
            if (piece?.side != side) return@forEachIndexed
            when (piece.type) {
                ChessPieceType.PAWN -> addPawnMoves(state, from, piece, moves)
                ChessPieceType.KNIGHT -> addLeaperMoves(state, from, piece, KNIGHT_DELTAS, moves)
                ChessPieceType.BISHOP -> addSlidingMoves(state, from, piece, DIAGONAL_DELTAS, moves)
                ChessPieceType.ROOK -> addSlidingMoves(state, from, piece, ORTHOGONAL_DELTAS, moves)
                ChessPieceType.QUEEN -> addSlidingMoves(
                    state,
                    from,
                    piece,
                    ORTHOGONAL_DELTAS + DIAGONAL_DELTAS,
                    moves
                )
                ChessPieceType.KING -> {
                    addLeaperMoves(state, from, piece, KING_DELTAS, moves)
                    addCastlingMoves(state, from, piece, moves)
                }
            }
        }
        return moves
    }

    private fun addPawnMoves(
        state: ChessState,
        from: Int,
        piece: ChessPiece,
        moves: MutableList<ChessMove>
    ) {
        val direction = if (piece.side == ChessSide.WHITE) 1 else -1
        val startRank = if (piece.side == ChessSide.WHITE) 1 else 6
        val fromFile = fileOf(from)
        val fromRank = rankOf(from)
        val oneRank = fromRank + direction
        if (oneRank in 0..7) {
            val one = oneRank * 8 + fromFile
            if (state.board[one] == null) {
                addPawnDestination(from, one, moves)
                val twoRank = fromRank + direction * 2
                val two = twoRank * 8 + fromFile
                if (fromRank == startRank && state.board[two] == null) {
                    moves += ChessMove(from, two)
                }
            }
            for (fileDelta in listOf(-1, 1)) {
                val toFile = fromFile + fileDelta
                if (toFile !in 0..7) continue
                val to = oneRank * 8 + toFile
                val target = state.board[to]
                val enPassantPawnSquare = to + if (piece.side == ChessSide.WHITE) -8 else 8
                val validEnPassant = to == state.enPassantSquare &&
                    state.board[enPassantPawnSquare] ==
                    ChessPiece(piece.side.other(), ChessPieceType.PAWN)
                if (
                    (target != null && target.side != piece.side && target.type != ChessPieceType.KING) ||
                    validEnPassant
                ) {
                    addPawnDestination(from, to, moves)
                }
            }
        }
    }

    private fun addPawnDestination(from: Int, to: Int, moves: MutableList<ChessMove>) {
        if (rankOf(to) in setOf(0, 7)) {
            PROMOTION_TYPES.forEach { moves += ChessMove(from, to, it) }
        } else {
            moves += ChessMove(from, to)
        }
    }

    private fun addLeaperMoves(
        state: ChessState,
        from: Int,
        piece: ChessPiece,
        deltas: List<Pair<Int, Int>>,
        moves: MutableList<ChessMove>
    ) {
        val fromFile = fileOf(from)
        val fromRank = rankOf(from)
        for ((fileDelta, rankDelta) in deltas) {
            val file = fromFile + fileDelta
            val rank = fromRank + rankDelta
            if (file !in 0..7 || rank !in 0..7) continue
            val to = rank * 8 + file
            val target = state.board[to]
            if (target == null || (target.side != piece.side && target.type != ChessPieceType.KING)) {
                moves += ChessMove(from, to)
            }
        }
    }

    private fun addSlidingMoves(
        state: ChessState,
        from: Int,
        piece: ChessPiece,
        deltas: List<Pair<Int, Int>>,
        moves: MutableList<ChessMove>
    ) {
        for ((fileDelta, rankDelta) in deltas) {
            var file = fileOf(from) + fileDelta
            var rank = rankOf(from) + rankDelta
            while (file in 0..7 && rank in 0..7) {
                val to = rank * 8 + file
                val target = state.board[to]
                if (target == null) {
                    moves += ChessMove(from, to)
                } else {
                    if (target.side != piece.side && target.type != ChessPieceType.KING) {
                        moves += ChessMove(from, to)
                    }
                    break
                }
                file += fileDelta
                rank += rankDelta
            }
        }
    }

    private fun addCastlingMoves(
        state: ChessState,
        from: Int,
        piece: ChessPiece,
        moves: MutableList<ChessMove>
    ) {
        val homeRank = if (piece.side == ChessSide.WHITE) 0 else 7
        if (from != homeRank * 8 + 4) return
        val enemy = piece.side.other()
        if (isSquareAttacked(state, from, enemy)) return

        val kingSideRight = if (piece.side == ChessSide.WHITE) {
            state.castlingRights.whiteKingSide
        } else {
            state.castlingRights.blackKingSide
        }
        if (
            kingSideRight &&
            state.board[homeRank * 8 + 5] == null &&
            state.board[homeRank * 8 + 6] == null &&
            state.board[homeRank * 8 + 7] == ChessPiece(piece.side, ChessPieceType.ROOK) &&
            !isSquareAttacked(state, homeRank * 8 + 5, enemy) &&
            !isSquareAttacked(state, homeRank * 8 + 6, enemy)
        ) {
            moves += ChessMove(from, homeRank * 8 + 6)
        }

        val queenSideRight = if (piece.side == ChessSide.WHITE) {
            state.castlingRights.whiteQueenSide
        } else {
            state.castlingRights.blackQueenSide
        }
        if (
            queenSideRight &&
            state.board[homeRank * 8 + 1] == null &&
            state.board[homeRank * 8 + 2] == null &&
            state.board[homeRank * 8 + 3] == null &&
            state.board[homeRank * 8] == ChessPiece(piece.side, ChessPieceType.ROOK) &&
            !isSquareAttacked(state, homeRank * 8 + 3, enemy) &&
            !isSquareAttacked(state, homeRank * 8 + 2, enemy)
        ) {
            moves += ChessMove(from, homeRank * 8 + 2)
        }
    }

    private fun attackedOnRay(
        state: ChessState,
        square: Int,
        side: ChessSide,
        deltas: List<Pair<Int, Int>>,
        matchingType: ChessPieceType
    ): Boolean {
        for ((fileDelta, rankDelta) in deltas) {
            var file = fileOf(square) + fileDelta
            var rank = rankOf(square) + rankDelta
            while (file in 0..7 && rank in 0..7) {
                val piece = state.board[rank * 8 + file]
                if (piece != null) {
                    if (
                        piece.side == side &&
                        (piece.type == matchingType || piece.type == ChessPieceType.QUEEN)
                    ) {
                        return true
                    }
                    break
                }
                file += fileDelta
                rank += rankDelta
            }
        }
        return false
    }

    private fun hasInsufficientMaterial(state: ChessState): Boolean {
        val nonKings = state.board.withIndex().filter { (_, piece) ->
            piece != null && piece.type != ChessPieceType.KING
        }
        if (nonKings.isEmpty()) return true
        if (nonKings.size == 1) {
            return nonKings.single().value?.type in setOf(
                ChessPieceType.BISHOP,
                ChessPieceType.KNIGHT
            )
        }
        if (nonKings.size == 2 && nonKings.all { it.value?.type == ChessPieceType.BISHOP }) {
            return nonKings.map { (square) -> (fileOf(square) + rankOf(square)) and 1 }
                .distinct()
                .size == 1
        }
        return false
    }
}

fun chessSquare(name: String): Int {
    require(name.length == 2) { "Invalid chess square: $name" }
    val file = name[0].lowercaseChar() - 'a'
    val rank = name[1] - '1'
    require(file in 0..7 && rank in 0..7) { "Invalid chess square: $name" }
    return rank * 8 + file
}

fun chessSquareName(square: Int): String {
    require(square in 0 until BOARD_SQUARES)
    return "${('a'.code + fileOf(square)).toChar()}${('1'.code + rankOf(square)).toChar()}"
}

internal fun fileOf(square: Int): Int = square and 7

internal fun rankOf(square: Int): Int = square ushr 3

private fun ChessCastlingRights.withoutRookAt(square: Int): ChessCastlingRights = when (square) {
    chessSquare("a1") -> copy(whiteQueenSide = false)
    chessSquare("h1") -> copy(whiteKingSide = false)
    chessSquare("a8") -> copy(blackQueenSide = false)
    chessSquare("h8") -> copy(blackKingSide = false)
    else -> this
}

private const val BOARD_SQUARES = 64

private val PROMOTION_TYPES = listOf(
    ChessPieceType.QUEEN,
    ChessPieceType.ROOK,
    ChessPieceType.BISHOP,
    ChessPieceType.KNIGHT
)

private val ORTHOGONAL_DELTAS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
private val DIAGONAL_DELTAS = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
private val KING_DELTAS = ORTHOGONAL_DELTAS + DIAGONAL_DELTAS
private val KNIGHT_DELTAS = listOf(
    1 to 2,
    2 to 1,
    2 to -1,
    1 to -2,
    -1 to -2,
    -2 to -1,
    -2 to 1,
    -1 to 2
)
