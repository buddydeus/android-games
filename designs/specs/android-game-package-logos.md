# Android Game Package Logos

## Goal

为五子棋、黑白棋、象棋和国际象棋建立一套可独立随游戏包更新的圆形 Logo。游戏中心仅读取清单 `icon` 指向的 PNG，不包含任何棋种识别或绘制逻辑。

## Visual Direction

采用“冷瓷棋章”视觉家族：圆形哑光瓷釉徽章、冷灰绿双层外圈、偏白内盘、深墨色主体、轻微压印与接触阴影。四枚 Logo 使用相同视角、边缘厚度、光线和留白，通过中心棋种符号区分。

这是介于纯平面图标和写实棋子之间的轻拟物风格。它延续首页的矿物灰、瓷面按钮与圆形内嵌槽，但不复制棋盘纹理，也不依赖文字。

## Shared Tokens

| Token | Value | Use |
| --- | --- | --- |
| Canvas | `1024 x 1024` PNG | Package source asset |
| Badge diameter | 86% of canvas | Leaves safe circular margin |
| Outer rim | `#6F8783`, `#AEBDB9` | Shared cool mineral identity |
| Inner porcelain | `#F2F5F1` | Clear contrast behind motifs |
| Primary ink | `#202A2C` | Main game symbol |
| White piece | `#F8F8F3` with dark rim | Legibility on porcelain |
| Xiangqi accent | `#B44339` | Only game-specific accent |
| Lighting | Upper-left soft studio light | Shared dimensional cue |

## Game Motifs

- **五子棋:** 五枚黑白棋子沿左下到右上的短斜线排列，形成明确的“五连”。
- **黑白棋:** 三枚交叠翻转棋片，依次为黑、半翻转、白，强调翻面机制。
- **象棋:** 红色圆棋子叠在极简九宫斜线与河界横线之上，不使用中文文字。
- **国际象棋:** 单一深墨色骑士侧面剪影，轮廓厚实、方向明确。

## Asset Rules

- 无文字、字母、数字、边框外装饰、水印和品牌标记。
- 主体必须完整落在圆形徽章内，缩小到 64dp 后仍能区分。
- 不使用透明度表达关键轮廓；黑白主体必须有清晰边界。
- 输出保留完整方形 PNG，游戏中心的圆形裁切负责隐藏四角。
- 文件固定为 `games/<gameId>/package/assets/icon.png`。
- 每个游戏清单的 `icon` 固定为 `assets/icon.png`。

## Preview Index

| Game | Preview | Package asset |
| --- | --- | --- |
| 五子棋 | `designs/previews/android-game-logo-gomoku.png` | `games/gomoku/package/assets/icon.png` |
| 黑白棋 | `designs/previews/android-game-logo-othello.png` | `games/othello/package/assets/icon.png` |
| 象棋 | `designs/previews/android-game-logo-xiangqi.png` | `games/xiangqi/package/assets/icon.png` |
| 国际象棋 | `designs/previews/android-game-logo-chess.png` | `games/chess/package/assets/icon.png` |

## Acceptance

- 四个 PNG 均可解码、尺寸一致且为正方形。
- 四个游戏 zip 均包含 `assets/icon.png`，不再依赖 `icon.txt`。
- 首页名称仍来自 `displayName`，排列仍由成功启动频率决定。
- 仅更新四个游戏版本；游戏中心壳版本保持 `0.0.3`。
