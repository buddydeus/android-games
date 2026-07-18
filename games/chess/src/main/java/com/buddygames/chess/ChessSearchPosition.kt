package com.buddygames.chess

import kotlin.math.abs

internal class ChessSearchPosition private constructor(
    private val board: IntArray,
    var sideToMove: ChessSide,
    private var castlingMask: Int,
    private var enPassantSquare: Int,
    var halfMoveClock: Int,
    private var fullMoveNumber: Int,
    var hash: Long
) {
    data class Undo(
        val from: Int,
        val to: Int,
        val movingCode: Int,
        val capturedCode: Int,
        val capturedSquare: Int,
        val previousSide: ChessSide,
        val previousCastlingMask: Int,
        val previousEnPassantSquare: Int,
        val previousHalfMoveClock: Int,
        val previousFullMoveNumber: Int,
        val previousHash: Long,
        val rookFrom: Int,
        val rookTo: Int
    )

    fun toState(): ChessState = ChessState(
        board = board.map(::decodePiece),
        sideToMove = sideToMove,
        castlingRights = decodeCastlingRights(castlingMask),
        enPassantSquare = enPassantSquare.takeIf { it >= 0 },
        halfMoveClock = halfMoveClock,
        fullMoveNumber = fullMoveNumber
    )

    fun legalMoves(): List<ChessMove> {
        val movingSide = sideToMove
        return pseudoLegalMoves(movingSide).filter { move ->
            val undo = makeMove(move)
            val legal = !isInCheck(movingSide)
            unmakeMove(undo)
            legal
        }
    }

    fun makeMove(move: ChessMove): Undo {
        val moving = board[move.from]
        require(moving != 0) { "Search move must have a moving piece" }
        val movingSide = sideOf(moving)
        val isEnPassant = typeOf(moving) == ChessPieceType.PAWN &&
            move.to == enPassantSquare &&
            board[move.to] == 0 &&
            fileOf(move.from) != fileOf(move.to)
        val capturedSquare = if (isEnPassant) {
            move.to + if (movingSide == ChessSide.WHITE) -8 else 8
        } else {
            move.to
        }
        val captured = board[capturedSquare]
        val castle = typeOf(moving) == ChessPieceType.KING &&
            abs(fileOf(move.to) - fileOf(move.from)) == 2
        val rookFrom = if (castle) {
            rankOf(move.from) * 8 + if (fileOf(move.to) == 6) 7 else 0
        } else {
            -1
        }
        val rookTo = if (castle) {
            rankOf(move.from) * 8 + if (fileOf(move.to) == 6) 5 else 3
        } else {
            -1
        }
        val undo = Undo(
            from = move.from,
            to = move.to,
            movingCode = moving,
            capturedCode = captured,
            capturedSquare = capturedSquare,
            previousSide = sideToMove,
            previousCastlingMask = castlingMask,
            previousEnPassantSquare = enPassantSquare,
            previousHalfMoveClock = halfMoveClock,
            previousFullMoveNumber = fullMoveNumber,
            previousHash = hash,
            rookFrom = rookFrom,
            rookTo = rookTo
        )

        hash = hash xor metadataHash(
            sideToMove,
            castlingMask,
            enPassantSquare,
            halfMoveClock,
            fullMoveNumber
        )
        hash = hash xor pieceHash(move.from, moving)
        if (captured != 0) hash = hash xor pieceHash(capturedSquare, captured)
        board[move.from] = 0
        board[capturedSquare] = 0

        val placed = if (
            typeOf(moving) == ChessPieceType.PAWN &&
            rankOf(move.to) in setOf(0, 7)
        ) {
            encodePiece(ChessPiece(movingSide, move.promotion ?: ChessPieceType.QUEEN))
        } else {
            moving
        }
        board[move.to] = placed
        hash = hash xor pieceHash(move.to, placed)

        if (castle) {
            val rook = board[rookFrom]
            hash = hash xor pieceHash(rookFrom, rook)
            board[rookFrom] = 0
            board[rookTo] = rook
            hash = hash xor pieceHash(rookTo, rook)
        }

        castlingMask = updatedCastlingMask(move, moving, captured)
        enPassantSquare = if (
            typeOf(moving) == ChessPieceType.PAWN &&
            abs(rankOf(move.to) - rankOf(move.from)) == 2
        ) {
            (move.from + move.to) / 2
        } else {
            -1
        }
        halfMoveClock = if (typeOf(moving) == ChessPieceType.PAWN || captured != 0) {
            0
        } else {
            halfMoveClock + 1
        }
        if (sideToMove == ChessSide.BLACK) fullMoveNumber++
        sideToMove = sideToMove.other()
        hash = hash xor metadataHash(
            sideToMove,
            castlingMask,
            enPassantSquare,
            halfMoveClock,
            fullMoveNumber
        )
        return undo
    }

    fun unmakeMove(undo: Undo) {
        board[undo.to] = 0
        if (undo.rookFrom >= 0) {
            board[undo.rookFrom] = board[undo.rookTo]
            board[undo.rookTo] = 0
        }
        board[undo.from] = undo.movingCode
        if (undo.capturedCode != 0) board[undo.capturedSquare] = undo.capturedCode
        sideToMove = undo.previousSide
        castlingMask = undo.previousCastlingMask
        enPassantSquare = undo.previousEnPassantSquare
        halfMoveClock = undo.previousHalfMoveClock
        fullMoveNumber = undo.previousFullMoveNumber
        hash = undo.previousHash
    }

    fun capturedPieceValue(move: ChessMove): Int {
        val direct = board[move.to]
        if (direct != 0) return typeOf(direct).value
        val moving = board[move.from]
        return if (
            moving != 0 &&
            typeOf(moving) == ChessPieceType.PAWN &&
            move.to == enPassantSquare &&
            fileOf(move.from) != fileOf(move.to)
        ) {
            ChessPieceType.PAWN.value
        } else {
            0
        }
    }

    fun movingPieceValue(move: ChessMove): Int =
        board[move.from].takeIf { it != 0 }?.let(::typeOf)?.value ?: 0

    fun isInCheck(side: ChessSide): Boolean {
        val kingCode = encodePiece(ChessPiece(side, ChessPieceType.KING))
        val king = board.indexOf(kingCode)
        return king < 0 || isSquareAttacked(king, side.other())
    }

    fun isAutomaticDraw(): Boolean = halfMoveClock >= 100 || hasInsufficientMaterial()

    fun evaluate(side: ChessSide, profile: ChessEvaluationProfile): Int {
        val absolute = evaluateForWhite(profile)
        return if (side == ChessSide.WHITE) absolute else -absolute
    }

    private fun evaluateForWhite(profile: ChessEvaluationProfile): Int {
        var score = 0
        for (square in board.indices) {
            val code = board[square]
            if (code == 0) continue
            val type = typeOf(code)
            if (type == ChessPieceType.KING) continue
            val pieceSide = sideOf(code)
            val sign = if (pieceSide == ChessSide.WHITE) 1 else -1
            score += sign * type.value
            if (profile >= ChessEvaluationProfile.PIECE_SQUARE) {
                score += sign * pieceSquareValue(pieceSide, type, square)
            }
        }
        if (profile >= ChessEvaluationProfile.ACTIVITY) {
            score += pseudoMobility(ChessSide.WHITE) - pseudoMobility(ChessSide.BLACK)
            score += development(ChessSide.WHITE) - development(ChessSide.BLACK)
        }
        if (profile >= ChessEvaluationProfile.FULL) {
            score += pawnStructure(ChessSide.WHITE) - pawnStructure(ChessSide.BLACK)
            score += kingSafety(ChessSide.WHITE) - kingSafety(ChessSide.BLACK)
            score += bishopPair(ChessSide.WHITE) - bishopPair(ChessSide.BLACK)
            score += rookFiles(ChessSide.WHITE) - rookFiles(ChessSide.BLACK)
        }
        return score
    }

    private fun pieceSquareValue(side: ChessSide, type: ChessPieceType, square: Int): Int {
        val file = fileOf(square)
        val rawRank = rankOf(square)
        val rank = if (side == ChessSide.WHITE) rawRank else 7 - rawRank
        val centerDistance = abs(file * 2 - 7) + abs(rank * 2 - 7)
        return when (type) {
            ChessPieceType.PAWN -> rank * 7 - abs(file * 2 - 7)
            ChessPieceType.KNIGHT -> 32 - centerDistance * 4
            ChessPieceType.BISHOP -> 24 - centerDistance * 2
            ChessPieceType.ROOK -> rank * 2 - abs(file * 2 - 7)
            ChessPieceType.QUEEN -> 12 - centerDistance
            ChessPieceType.KING -> if (fullMoveNumber < 20) {
                -rank * 4 - centerDistance
            } else {
                24 - centerDistance * 3
            }
        }
    }

    private fun pseudoMobility(side: ChessSide): Int =
        pseudoLegalMoves(side, includeCastling = false, includeEnPassant = false).size * 2

    private fun development(side: ChessSide): Int {
        val homeRank = if (side == ChessSide.WHITE) 0 else 7
        var score = 0
        listOf(1, 6).forEach { file ->
            if (board[homeRank * 8 + file] != encodePiece(ChessPiece(side, ChessPieceType.KNIGHT))) {
                score += 8
            }
        }
        listOf(2, 5).forEach { file ->
            if (board[homeRank * 8 + file] != encodePiece(ChessPiece(side, ChessPieceType.BISHOP))) {
                score += 7
            }
        }
        return score
    }

    private fun pawnStructure(side: ChessSide): Int {
        val pawnCode = encodePiece(ChessPiece(side, ChessPieceType.PAWN))
        val counts = IntArray(8)
        for (square in board.indices) if (board[square] == pawnCode) counts[fileOf(square)]++
        var score = 0
        for (square in board.indices) {
            if (board[square] != pawnCode) continue
            val file = fileOf(square)
            val rank = rankOf(square)
            if (counts[file] > 1) score -= 12
            if (!((file > 0 && counts[file - 1] > 0) || (file < 7 && counts[file + 1] > 0))) {
                score -= 10
            }
            if (isPassedPawn(side, file, rank)) {
                val advance = if (side == ChessSide.WHITE) rank else 7 - rank
                score += 12 + advance * 5
            }
        }
        return score
    }

    private fun isPassedPawn(side: ChessSide, file: Int, rank: Int): Boolean {
        val enemyPawn = encodePiece(ChessPiece(side.other(), ChessPieceType.PAWN))
        for (enemyFile in maxOf(0, file - 1)..minOf(7, file + 1)) {
            for (enemyRank in 0..7) {
                val ahead = if (side == ChessSide.WHITE) enemyRank > rank else enemyRank < rank
                if (ahead && board[enemyRank * 8 + enemyFile] == enemyPawn) return false
            }
        }
        return true
    }

    private fun kingSafety(side: ChessSide): Int {
        val king = board.indexOf(encodePiece(ChessPiece(side, ChessPieceType.KING)))
        if (king < 0) return -10_000
        val shieldRank = rankOf(king) + if (side == ChessSide.WHITE) 1 else -1
        var score = if (isInCheck(side)) -45 else 0
        val pawn = encodePiece(ChessPiece(side, ChessPieceType.PAWN))
        if (shieldRank in 0..7) {
            for (file in maxOf(0, fileOf(king) - 1)..minOf(7, fileOf(king) + 1)) {
                if (board[shieldRank * 8 + file] == pawn) score += 12
            }
        }
        return score
    }

    private fun bishopPair(side: ChessSide): Int {
        val bishop = encodePiece(ChessPiece(side, ChessPieceType.BISHOP))
        return if (board.count { it == bishop } >= 2) 28 else 0
    }

    private fun rookFiles(side: ChessSide): Int {
        val rook = encodePiece(ChessPiece(side, ChessPieceType.ROOK))
        val ownPawn = encodePiece(ChessPiece(side, ChessPieceType.PAWN))
        val enemyPawn = encodePiece(ChessPiece(side.other(), ChessPieceType.PAWN))
        var score = 0
        for (square in board.indices) {
            if (board[square] != rook) continue
            val file = fileOf(square)
            val hasOwnPawn = (0..7).any { board[it * 8 + file] == ownPawn }
            val hasEnemyPawn = (0..7).any { board[it * 8 + file] == enemyPawn }
            score += when {
                !hasOwnPawn && !hasEnemyPawn -> 20
                !hasOwnPawn -> 10
                else -> 0
            }
        }
        return score
    }

    private fun pseudoLegalMoves(
        side: ChessSide,
        includeCastling: Boolean = true,
        includeEnPassant: Boolean = true
    ): List<ChessMove> {
        val moves = ArrayList<ChessMove>(64)
        for (from in board.indices) {
            val code = board[from]
            if (code == 0 || sideOf(code) != side) continue
            when (typeOf(code)) {
                ChessPieceType.PAWN -> addPawnMoves(
                    from,
                    side,
                    moves,
                    includeEnPassant
                )
                ChessPieceType.KNIGHT -> addLeaperMoves(from, side, KNIGHT_DELTAS, moves)
                ChessPieceType.BISHOP -> addSlidingMoves(from, side, DIAGONAL_DELTAS, moves)
                ChessPieceType.ROOK -> addSlidingMoves(from, side, ORTHOGONAL_DELTAS, moves)
                ChessPieceType.QUEEN -> addSlidingMoves(
                    from,
                    side,
                    ORTHOGONAL_DELTAS + DIAGONAL_DELTAS,
                    moves
                )
                ChessPieceType.KING -> {
                    addLeaperMoves(from, side, KING_DELTAS, moves)
                    if (includeCastling) addCastlingMoves(from, side, moves)
                }
            }
        }
        return moves
    }

    private fun addPawnMoves(
        from: Int,
        side: ChessSide,
        moves: MutableList<ChessMove>,
        includeEnPassant: Boolean
    ) {
        val direction = if (side == ChessSide.WHITE) 1 else -1
        val startRank = if (side == ChessSide.WHITE) 1 else 6
        val fromFile = fileOf(from)
        val oneRank = rankOf(from) + direction
        if (oneRank !in 0..7) return
        val one = oneRank * 8 + fromFile
        if (board[one] == 0) {
            addPawnDestination(from, one, moves)
            val two = (oneRank + direction) * 8 + fromFile
            if (rankOf(from) == startRank && board[two] == 0) moves += ChessMove(from, two)
        }
        for (delta in listOf(-1, 1)) {
            val toFile = fromFile + delta
            if (toFile !in 0..7) continue
            val to = oneRank * 8 + toFile
            val target = board[to]
            val validEnPassant = includeEnPassant &&
                to == enPassantSquare &&
                board[to + if (side == ChessSide.WHITE) -8 else 8] ==
                encodePiece(ChessPiece(side.other(), ChessPieceType.PAWN))
            if (
                (target != 0 && sideOf(target) != side && typeOf(target) != ChessPieceType.KING) ||
                validEnPassant
            ) {
                addPawnDestination(from, to, moves)
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
        from: Int,
        side: ChessSide,
        deltas: List<Pair<Int, Int>>,
        moves: MutableList<ChessMove>
    ) {
        for ((df, dr) in deltas) {
            val file = fileOf(from) + df
            val rank = rankOf(from) + dr
            if (file !in 0..7 || rank !in 0..7) continue
            val to = rank * 8 + file
            val target = board[to]
            if (target == 0 || (sideOf(target) != side && typeOf(target) != ChessPieceType.KING)) {
                moves += ChessMove(from, to)
            }
        }
    }

    private fun addSlidingMoves(
        from: Int,
        side: ChessSide,
        deltas: List<Pair<Int, Int>>,
        moves: MutableList<ChessMove>
    ) {
        for ((df, dr) in deltas) {
            var file = fileOf(from) + df
            var rank = rankOf(from) + dr
            while (file in 0..7 && rank in 0..7) {
                val to = rank * 8 + file
                val target = board[to]
                if (target == 0) {
                    moves += ChessMove(from, to)
                } else {
                    if (sideOf(target) != side && typeOf(target) != ChessPieceType.KING) {
                        moves += ChessMove(from, to)
                    }
                    break
                }
                file += df
                rank += dr
            }
        }
    }

    private fun addCastlingMoves(from: Int, side: ChessSide, moves: MutableList<ChessMove>) {
        val homeRank = if (side == ChessSide.WHITE) 0 else 7
        if (from != homeRank * 8 + 4 || isSquareAttacked(from, side.other())) return
        val kingBit = if (side == ChessSide.WHITE) WHITE_KING_SIDE else BLACK_KING_SIDE
        if (
            castlingMask and kingBit != 0 &&
            board[homeRank * 8 + 5] == 0 &&
            board[homeRank * 8 + 6] == 0 &&
            board[homeRank * 8 + 7] == encodePiece(ChessPiece(side, ChessPieceType.ROOK)) &&
            !isSquareAttacked(homeRank * 8 + 5, side.other()) &&
            !isSquareAttacked(homeRank * 8 + 6, side.other())
        ) {
            moves += ChessMove(from, homeRank * 8 + 6)
        }
        val queenBit = if (side == ChessSide.WHITE) WHITE_QUEEN_SIDE else BLACK_QUEEN_SIDE
        if (
            castlingMask and queenBit != 0 &&
            board[homeRank * 8 + 1] == 0 &&
            board[homeRank * 8 + 2] == 0 &&
            board[homeRank * 8 + 3] == 0 &&
            board[homeRank * 8] == encodePiece(ChessPiece(side, ChessPieceType.ROOK)) &&
            !isSquareAttacked(homeRank * 8 + 3, side.other()) &&
            !isSquareAttacked(homeRank * 8 + 2, side.other())
        ) {
            moves += ChessMove(from, homeRank * 8 + 2)
        }
    }

    private fun isSquareAttacked(square: Int, bySide: ChessSide): Boolean {
        val targetFile = fileOf(square)
        val targetRank = rankOf(square)
        val pawnRank = targetRank + if (bySide == ChessSide.WHITE) -1 else 1
        for (delta in listOf(-1, 1)) {
            val file = targetFile + delta
            if (
                file in 0..7 &&
                pawnRank in 0..7 &&
                board[pawnRank * 8 + file] ==
                encodePiece(ChessPiece(bySide, ChessPieceType.PAWN))
            ) return true
        }
        if (attackedByLeaper(square, bySide, ChessPieceType.KNIGHT, KNIGHT_DELTAS)) return true
        if (attackedByLeaper(square, bySide, ChessPieceType.KING, KING_DELTAS)) return true
        if (attackedOnRay(square, bySide, ORTHOGONAL_DELTAS, ChessPieceType.ROOK)) return true
        return attackedOnRay(square, bySide, DIAGONAL_DELTAS, ChessPieceType.BISHOP)
    }

    private fun attackedByLeaper(
        square: Int,
        side: ChessSide,
        type: ChessPieceType,
        deltas: List<Pair<Int, Int>>
    ): Boolean {
        val expected = encodePiece(ChessPiece(side, type))
        for ((df, dr) in deltas) {
            val file = fileOf(square) + df
            val rank = rankOf(square) + dr
            if (file in 0..7 && rank in 0..7 && board[rank * 8 + file] == expected) return true
        }
        return false
    }

    private fun attackedOnRay(
        square: Int,
        side: ChessSide,
        deltas: List<Pair<Int, Int>>,
        matching: ChessPieceType
    ): Boolean {
        for ((df, dr) in deltas) {
            var file = fileOf(square) + df
            var rank = rankOf(square) + dr
            while (file in 0..7 && rank in 0..7) {
                val code = board[rank * 8 + file]
                if (code != 0) {
                    if (
                        sideOf(code) == side &&
                        (typeOf(code) == matching || typeOf(code) == ChessPieceType.QUEEN)
                    ) return true
                    break
                }
                file += df
                rank += dr
            }
        }
        return false
    }

    private fun updatedCastlingMask(move: ChessMove, moving: Int, captured: Int): Int {
        var next = castlingMask
        if (typeOf(moving) == ChessPieceType.KING) {
            next = if (sideOf(moving) == ChessSide.WHITE) {
                next and (WHITE_KING_SIDE or WHITE_QUEEN_SIDE).inv()
            } else {
                next and (BLACK_KING_SIDE or BLACK_QUEEN_SIDE).inv()
            }
        }
        if (typeOf(moving) == ChessPieceType.ROOK) next = clearRookRight(next, move.from)
        if (captured != 0 && typeOf(captured) == ChessPieceType.ROOK) {
            next = clearRookRight(next, move.to)
        }
        return next
    }

    private fun hasInsufficientMaterial(): Boolean {
        val pieces = board.withIndex().filter { (_, code) ->
            code != 0 && typeOf(code) != ChessPieceType.KING
        }
        if (pieces.isEmpty()) return true
        if (pieces.size == 1) {
            return typeOf(pieces.single().value) in setOf(
                ChessPieceType.BISHOP,
                ChessPieceType.KNIGHT
            )
        }
        if (pieces.size == 2 && pieces.all { typeOf(it.value) == ChessPieceType.BISHOP }) {
            return pieces.map { (square) -> (fileOf(square) + rankOf(square)) and 1 }
                .distinct()
                .size == 1
        }
        return false
    }

    companion object {
        fun from(state: ChessState): ChessSearchPosition {
            val board = IntArray(64)
            var hash = 0L
            state.board.forEachIndexed { square, piece ->
                if (piece != null) {
                    val code = encodePiece(piece)
                    board[square] = code
                    hash = hash xor pieceHash(square, code)
                }
            }
            val castlingMask = encodeCastlingRights(state.castlingRights)
            val enPassant = state.enPassantSquare ?: -1
            hash = hash xor metadataHash(
                state.sideToMove,
                castlingMask,
                enPassant,
                state.halfMoveClock,
                state.fullMoveNumber
            )
            return ChessSearchPosition(
                board = board,
                sideToMove = state.sideToMove,
                castlingMask = castlingMask,
                enPassantSquare = enPassant,
                halfMoveClock = state.halfMoveClock,
                fullMoveNumber = state.fullMoveNumber,
                hash = hash
            )
        }

        private fun encodePiece(piece: ChessPiece): Int {
            val value = piece.type.ordinal + 1
            return if (piece.side == ChessSide.WHITE) value else -value
        }

        private fun decodePiece(code: Int): ChessPiece? {
            if (code == 0) return null
            return ChessPiece(sideOf(code), typeOf(code))
        }

        private fun sideOf(code: Int): ChessSide =
            if (code > 0) ChessSide.WHITE else ChessSide.BLACK

        private fun typeOf(code: Int): ChessPieceType =
            ChessPieceType.entries[abs(code) - 1]

        private fun pieceHash(square: Int, code: Int): Long =
            mixChessHash(square.toLong() * 17 + code + 16)

        private fun metadataHash(
            side: ChessSide,
            castlingMask: Int,
            enPassantSquare: Int,
            halfMoveClock: Int,
            fullMoveNumber: Int
        ): Long {
            var hash = mixChessHash(2_000L + castlingMask)
            if (side == ChessSide.BLACK) hash = hash xor SIDE_HASH
            if (enPassantSquare >= 0) {
                hash = hash xor mixChessHash(3_000L + fileOf(enPassantSquare))
            }
            hash = hash xor mixChessHash(4_000L + halfMoveClock.coerceAtMost(100))
            hash = hash xor mixChessHash(5_000L + fullMoveNumber.coerceAtMost(20))
            return hash
        }

        private fun encodeCastlingRights(rights: ChessCastlingRights): Int =
            (if (rights.whiteKingSide) WHITE_KING_SIDE else 0) or
                (if (rights.whiteQueenSide) WHITE_QUEEN_SIDE else 0) or
                (if (rights.blackKingSide) BLACK_KING_SIDE else 0) or
                (if (rights.blackQueenSide) BLACK_QUEEN_SIDE else 0)

        private fun decodeCastlingRights(mask: Int): ChessCastlingRights = ChessCastlingRights(
            whiteKingSide = mask and WHITE_KING_SIDE != 0,
            whiteQueenSide = mask and WHITE_QUEEN_SIDE != 0,
            blackKingSide = mask and BLACK_KING_SIDE != 0,
            blackQueenSide = mask and BLACK_QUEEN_SIDE != 0
        )

        private fun clearRookRight(mask: Int, square: Int): Int = when (square) {
            0 -> mask and WHITE_QUEEN_SIDE.inv()
            7 -> mask and WHITE_KING_SIDE.inv()
            56 -> mask and BLACK_QUEEN_SIDE.inv()
            63 -> mask and BLACK_KING_SIDE.inv()
            else -> mask
        }

        private const val WHITE_KING_SIDE = 1
        private const val WHITE_QUEEN_SIDE = 2
        private const val BLACK_KING_SIDE = 4
        private const val BLACK_QUEEN_SIDE = 8
        private const val SIDE_HASH = -3335678366873096957L

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
    }
}

internal fun positionHash(state: ChessState): Long = ChessSearchPosition.from(state).hash

private fun mixChessHash(input: Long): Long {
    var value = input + -7046029254386353131L
    value = (value xor (value ushr 30)) * -4658895280553007687L
    value = (value xor (value ushr 27)) * -7723592293110705685L
    return value xor (value ushr 31)
}
