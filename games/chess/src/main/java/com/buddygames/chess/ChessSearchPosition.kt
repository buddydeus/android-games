package com.buddygames.chess

import kotlin.math.abs

internal class ChessSearchPosition private constructor(
    private var current: ChessState
) {
    data class Undo(val state: ChessState)

    val state: ChessState
        get() = current

    val hash: Long
        get() = positionHash(current)

    fun legalMoves(): List<ChessMove> = ChessRules.legalMoves(current)

    fun makeMove(move: ChessMove): Undo {
        val undo = Undo(current)
        current = current.applyUnchecked(move)
        return undo
    }

    fun unmakeMove(undo: Undo) {
        current = undo.state
    }

    fun capturedPieceValue(move: ChessMove): Int {
        val direct = current.board[move.to]
        if (direct != null) return direct.type.value
        val moving = current.board[move.from]
        return if (
            moving?.type == ChessPieceType.PAWN &&
            move.to == current.enPassantSquare &&
            fileOf(move.from) != fileOf(move.to)
        ) {
            ChessPieceType.PAWN.value
        } else {
            0
        }
    }

    fun movingPieceValue(move: ChessMove): Int =
        current.board[move.from]?.type?.value ?: 0

    fun evaluate(side: ChessSide, profile: ChessEvaluationProfile): Int {
        val absolute = evaluateForWhite(profile)
        return if (side == ChessSide.WHITE) absolute else -absolute
    }

    private fun evaluateForWhite(profile: ChessEvaluationProfile): Int {
        var score = 0
        current.board.forEachIndexed { square, piece ->
            if (piece == null || piece.type == ChessPieceType.KING) return@forEachIndexed
            val sign = if (piece.side == ChessSide.WHITE) 1 else -1
            score += sign * piece.type.value
            if (profile >= ChessEvaluationProfile.PIECE_SQUARE) {
                score += sign * pieceSquareValue(piece, square)
            }
        }
        if (profile >= ChessEvaluationProfile.ACTIVITY) {
            score += mobility(ChessSide.WHITE) - mobility(ChessSide.BLACK)
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

    private fun pieceSquareValue(piece: ChessPiece, square: Int): Int {
        val file = fileOf(square)
        val rawRank = rankOf(square)
        val rank = if (piece.side == ChessSide.WHITE) rawRank else 7 - rawRank
        val centerDistance = abs(file * 2 - 7) + abs(rank * 2 - 7)
        return when (piece.type) {
            ChessPieceType.PAWN -> rank * 7 - abs(file * 2 - 7)
            ChessPieceType.KNIGHT -> 32 - centerDistance * 4
            ChessPieceType.BISHOP -> 24 - centerDistance * 2
            ChessPieceType.ROOK -> rank * 2 - abs(file * 2 - 7)
            ChessPieceType.QUEEN -> 12 - centerDistance
            ChessPieceType.KING -> if (current.fullMoveNumber < 20) {
                -rank * 4 - centerDistance
            } else {
                24 - centerDistance * 3
            }
        }
    }

    private fun mobility(side: ChessSide): Int {
        val stateForSide = if (current.sideToMove == side) current else current.copy(sideToMove = side)
        return ChessRules.legalMoves(stateForSide).size * 2
    }

    private fun development(side: ChessSide): Int {
        val homeRank = if (side == ChessSide.WHITE) 0 else 7
        var score = 0
        listOf(1, 6).forEach { file ->
            if (current.board[homeRank * 8 + file] != ChessPiece(side, ChessPieceType.KNIGHT)) {
                score += 8
            }
        }
        listOf(2, 5).forEach { file ->
            if (current.board[homeRank * 8 + file] != ChessPiece(side, ChessPieceType.BISHOP)) {
                score += 7
            }
        }
        return score
    }

    private fun pawnStructure(side: ChessSide): Int {
        val pawnSquares = current.board.indices.filter {
            current.board[it] == ChessPiece(side, ChessPieceType.PAWN)
        }
        val counts = IntArray(8)
        pawnSquares.forEach { counts[fileOf(it)]++ }
        var score = 0
        pawnSquares.forEach { square ->
            val file = fileOf(square)
            val rank = rankOf(square)
            if (counts[file] > 1) score -= 12
            val hasNeighbor = (file > 0 && counts[file - 1] > 0) ||
                (file < 7 && counts[file + 1] > 0)
            if (!hasNeighbor) score -= 10
            if (isPassedPawn(side, file, rank)) {
                val advance = if (side == ChessSide.WHITE) rank else 7 - rank
                score += 12 + advance * 5
            }
        }
        return score
    }

    private fun isPassedPawn(side: ChessSide, file: Int, rank: Int): Boolean {
        val enemy = side.other()
        for (enemyFile in maxOf(0, file - 1)..minOf(7, file + 1)) {
            for (enemyRank in 0..7) {
                val ahead = if (side == ChessSide.WHITE) enemyRank > rank else enemyRank < rank
                if (
                    ahead &&
                    current.board[enemyRank * 8 + enemyFile] ==
                    ChessPiece(enemy, ChessPieceType.PAWN)
                ) {
                    return false
                }
            }
        }
        return true
    }

    private fun kingSafety(side: ChessSide): Int {
        val king = current.board.indexOf(ChessPiece(side, ChessPieceType.KING))
        if (king < 0) return -10_000
        val kingFile = fileOf(king)
        val kingRank = rankOf(king)
        val shieldRank = kingRank + if (side == ChessSide.WHITE) 1 else -1
        var score = if (ChessRules.isInCheck(current, side)) -45 else 0
        if (shieldRank in 0..7) {
            for (file in maxOf(0, kingFile - 1)..minOf(7, kingFile + 1)) {
                if (
                    current.board[shieldRank * 8 + file] ==
                    ChessPiece(side, ChessPieceType.PAWN)
                ) {
                    score += 12
                }
            }
        }
        return score
    }

    private fun bishopPair(side: ChessSide): Int =
        if (current.board.count { it == ChessPiece(side, ChessPieceType.BISHOP) } >= 2) 28 else 0

    private fun rookFiles(side: ChessSide): Int {
        var score = 0
        current.board.forEachIndexed { square, piece ->
            if (piece != ChessPiece(side, ChessPieceType.ROOK)) return@forEachIndexed
            val file = fileOf(square)
            val ownPawn = (0..7).any { rank ->
                current.board[rank * 8 + file] == ChessPiece(side, ChessPieceType.PAWN)
            }
            val enemyPawn = (0..7).any { rank ->
                current.board[rank * 8 + file] == ChessPiece(side.other(), ChessPieceType.PAWN)
            }
            score += when {
                !ownPawn && !enemyPawn -> 20
                !ownPawn -> 10
                else -> 0
            }
        }
        return score
    }

    companion object {
        fun from(state: ChessState): ChessSearchPosition = ChessSearchPosition(state)
    }
}

internal fun positionHash(state: ChessState): Long {
    var hash = 0x6a09e667f3bcc909L
    state.board.forEachIndexed { square, piece ->
        if (piece != null) {
            val code = piece.type.ordinal + 1 + piece.side.ordinal * ChessPieceType.entries.size
            hash = hash xor mixChessHash(square.toLong() * 17 + code)
        }
    }
    if (state.sideToMove == ChessSide.BLACK) hash = hash xor -3335678366873096957L
    val rights = state.castlingRights
    if (rights.whiteKingSide) hash = hash xor 0x243f6a8885a308d3L
    if (rights.whiteQueenSide) hash = hash xor 0x13198a2e03707344L
    if (rights.blackKingSide) hash = hash xor -0x5c0c8034d3ac7d6bL
    if (rights.blackQueenSide) hash = hash xor 0x452821e638d01377L
    state.enPassantSquare?.let { hash = hash xor mixChessHash(1_000L + fileOf(it)) }
    return hash
}

private fun mixChessHash(input: Long): Long {
    var value = input + -7046029254386353131L
    value = (value xor (value ushr 30)) * -4658895280553007687L
    value = (value xor (value ushr 27)) * -7723592293110705685L
    return value xor (value ushr 31)
}
