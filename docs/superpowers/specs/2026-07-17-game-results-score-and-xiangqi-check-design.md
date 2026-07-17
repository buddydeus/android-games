# Game Results, Score, and Xiangqi Check Design

## Scope

Enhance the in-game information rail for Gomoku, Othello, and Xiangqi without
changing the shell or `game-api`.

## Match Lifecycle

- Each `GameScreen` owns a session score initialized to `0 : 0`.
- Gomoku scores black on the left and white on the right.
- Othello scores black on the left and white on the right.
- Xiangqi scores red on the left and black on the right.
- A decisive result increments the winner exactly once.
- A draw leaves the score unchanged.
- A terminal game shows a `重新开始` button while retaining `退出游戏`.
- Restart resets the board, turn, selection, and result, but preserves the
  session score.
- Leaving the game screen discards the session score.

## Xiangqi Status

- During an active game, the rail renders `当前回合：红方` or
  `当前回合：黑方`.
- `当前回合：` uses the normal ink color.
- `红方` uses cinnabar red; `黑方` uses lacquer black.
- If the side whose turn it is has its general under attack, the rail shows a
  separate `将军` warning.
- A finished game hides the check warning and displays the result.

## Rule Support

`XiangqiRules.isInCheck(state, side)` locates that side's general and checks
whether an opposing legal attack can capture it. General-to-general attacks on
an open file are included so the warning also covers the flying-general rule.

## Testing

- Unit-test score recording for each game, including draws where applicable.
- Unit-test Xiangqi rook and flying-general check detection.
- Unit-test restart visibility from terminal state.
- Run each touched game module's unit tests, then `npm run verify`.
