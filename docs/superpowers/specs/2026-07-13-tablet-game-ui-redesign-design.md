# Android Pad Game UI Redesign

## Goal

Bring the existing offline Android Pad game center in line with the supplied high-fidelity web prototypes while preserving the stable shell and independently loadable game-package architecture.

## Scope

The redesign covers seven Compose screens:

- The game-center home screen in `app`.
- A menu screen and a game screen for each of Gomoku, Othello, and Xiangqi.

Game rules, built-in package installation, zip import, dex loading, and the `game-api` public contract remain unchanged.

## Navigation

The application keeps a linear navigation model:

`home -> game menu -> game board`

- Selecting a game loads its plugin and opens its menu.
- Selecting single-player or two-player starts the existing mode in the game board.
- Exiting a game board returns directly to the game-center home screen.
- Exiting a game menu returns to the game-center home screen.
- Game boards do not expose a return-to-menu action or a restart action. A completed game remains visible until the player exits.

## Shared Visual Language

- The home screen uses a warm paper background (`#F6F7F1`), ivory surfaces (`#FFFFFC`), ink text, and restrained blue primary actions (`#274C77`).
- Home content has `28.dp` page padding, `18.dp` grid gaps, and a three-column-first adaptive game grid with a `260.dp` minimum card width.
- Cards use a `14.dp` corner radius, clear focus borders, a game-specific icon, availability status, display name, version, board size, and package id.
- Touch targets are at least `48.dp` in both dimensions. Press feedback may change elevation, color, or scale without shifting layout.
- Typography uses Android serif fallbacks for display titles and system sans-serif text for controls and metadata. No external fonts are introduced.
- The app stays light-only. It does not gain login, network services, store features, rankings, settings, a bottom tab bar, or a dark theme.

## Home Screen

The top bar contains a board-inspired mark, the title `游戏中心`, and a primary `导入游戏包` action. Below it, a section heading shows `已安装游戏`, `本地棋类`, and the installed game count.

Imported-package feedback becomes an inline message bar with semantic success, warning, or error colors. The existing Android document picker and repository validation remain the source of truth. The UI distinguishes successful imports from load/import failures and does not imply validation that the repository has not performed.

Each installed game tile is entirely clickable and shows its native game identity:

| Game | Board metadata | Accent |
| --- | --- | --- |
| Gomoku | `15 x 15` | Ink `#2D3438` with wood |
| Othello | `8 x 8` | Green `#2F6F56` |
| Xiangqi | `9 x 10` | Vermilion `#9B2F2F` with wood |

## Game Menus

Each plugin owns its own menu implementation so packages remain independently updateable. The three menus share the same composition: a game mark, display title, and vertically centered buttons for `单人模式`, `双人对战`, and `退出游戏`.

Menus are full-screen and omit top bars, rules explanations, metrics, and extra controls. Their backgrounds use the game’s material language:

- Gomoku: pale wooden grid with black and white stones.
- Othello: dark wood frame and green board surface.
- Xiangqi: pale wooden court with vermilion detail.

## Game Boards

Landscape game boards use a responsive two-pane layout:

- The board is the primary pane.
- A narrow paper-and-bronze-style information rail contains only historical score, current turn, and `退出游戏`.
- On constrained width, the rail moves below the board and lays its three items out horizontally.

The board remains a distinct physical object with a dark wood frame, inset play surface, border line, and shadow. Game backgrounds may suggest wood, bamboo, lacquer, or paper but must not become a large chessboard pattern. Technology-themed gradients, cartoon pieces, and decorative character art are out of scope.

Existing game state and AI remain authoritative. The presentation derives historical score from existing state where available; MVP sessions begin at `0 : 0` and show the current winner count only after a finished round. The information rail does not add Othello disc counters, rules text, restart controls, or back-to-menu controls.

## Accessibility and Resilience

- Every tile and button has a Compose semantic label derived from the visible Chinese copy.
- Focused interactive elements use a visible outline with at least 3:1 contrast against their background.
- The layouts fit 10–12 inch landscape tablets first and reduce to two/one home-grid columns or a below-board information rail on narrower screens.
- User-facing import errors retain the repository-provided error detail.

## Validation

- Add focused unit tests for any extracted, non-Compose visual metadata or state-mapping helpers.
- Run module tests for `app`, `games:gomoku`, `games:othello`, and `games:xiangqi`.
- Run `npm run verify` after all UI changes.
- Use `HEADLESS=1 npm run start` and UIAutomator/screenshot inspection to verify the home screen, all menus, all boards, mode navigation, exit navigation, and a legal move in each game.

## Non-Goals

- Changing `game-api` or `CURRENT_SHELL_API`.
- Changing package archive contents, zip installation, `GamePackageRepository`, `DexGamePluginLoader`, or root packaging tasks.
- Adding complete Xiangqi rule coverage beyond the existing MVP engine.
