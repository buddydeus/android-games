package com.buddygames.doushouqi

internal fun pos(row: Int, column: Int) = DoushouqiPosition(row, column)

internal fun green(animal: DoushouqiAnimal) =
    DoushouqiPiece(DoushouqiSide.PINE_GREEN, animal)

internal fun red(animal: DoushouqiAnimal) =
    DoushouqiPiece(DoushouqiSide.VERMILION, animal)

internal fun move(from: DoushouqiPosition, to: DoushouqiPosition) =
    DoushouqiMove(from, to)

internal fun stateOf(
    vararg pieces: Pair<DoushouqiPosition, DoushouqiPiece>,
): DoushouqiState = stateOf(
    sideToMove = DoushouqiSide.PINE_GREEN,
    pieces = pieces,
)

internal fun stateOf(
    sideToMove: DoushouqiSide,
    vararg pieces: Pair<DoushouqiPosition, DoushouqiPiece>,
): DoushouqiState = DoushouqiState.fromPieces(
    sideToMove = sideToMove,
    pieces = linkedMapOf(*pieces),
)

internal fun legal(state: DoushouqiState): List<DoushouqiMove> =
    DoushouqiRules.legalMoves(state)
