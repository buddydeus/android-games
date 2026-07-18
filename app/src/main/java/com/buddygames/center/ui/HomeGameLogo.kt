package com.buddygames.center.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LogoWell = Color(0xFFE5ECE9)
private val LogoBorder = Color(0xFFAEBDB9)
private val LogoRim = Color.White.copy(alpha = 0.72f)
private val LogoInk = Color(0xFF242A2C)
private val LogoPorcelain = Color(0xFFF5F2E9)
private val XiangqiRed = Color(0xFFA33B33)
private val PieceShadow = Color(0xFF2F4243).copy(alpha = 0.18f)

@Composable
internal fun HomeGameLogoMark(
    logo: HomeGameLogo,
    fallbackSymbol: String,
    logoSizeDp: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(LogoWell)
            .border(1.dp, LogoBorder, CircleShape)
            .padding(2.dp)
            .border(1.dp, LogoRim, CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (logo) {
            HomeGameLogo.Gomoku -> GomokuLogo(Modifier.fillMaxSize())
            HomeGameLogo.Xiangqi -> XiangqiLogo(
                glyphSizeSp = xiangqiGlyphSizeSp(logoSizeDp),
                modifier = Modifier.fillMaxSize()
            )
            HomeGameLogo.Chess -> ChessLogo(
                glyphSizeSp = (logoSizeDp * 0.58f).toInt().coerceIn(30, 64),
                modifier = Modifier.fillMaxSize()
            )
            HomeGameLogo.Othello -> OthelloLogo(Modifier.fillMaxSize())
            HomeGameLogo.Generic -> Text(
                text = fallbackSymbol.trim().take(1).ifEmpty { "游" },
                color = LogoInk,
                fontSize = xiangqiGlyphSizeSp(logoSizeDp).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChessLogo(
    glyphSizeSp: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = "♞",
            color = LogoInk,
            fontSize = glyphSizeSp.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GomokuLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radius = size.minDimension * 0.105f
        val outline = 1.4.dp.toPx()
        val shadowOffset = 1.4.dp.toPx()
        val centers = listOf(
            Offset(size.width * 0.18f, size.height * 0.78f),
            Offset(size.width * 0.34f, size.height * 0.63f),
            Offset(size.width * 0.50f, size.height * 0.50f),
            Offset(size.width * 0.66f, size.height * 0.35f),
            Offset(size.width * 0.82f, size.height * 0.20f)
        )

        centers.forEachIndexed { index, center ->
            drawCircle(
                color = PieceShadow,
                radius = radius,
                center = center + Offset(shadowOffset, shadowOffset)
            )
            if (index % 2 == 0) {
                drawCircle(color = LogoInk, radius = radius, center = center)
            } else {
                drawCircle(color = LogoInk, radius = radius + outline, center = center)
                drawCircle(color = LogoPorcelain, radius = radius, center = center)
            }
        }
    }
}

@Composable
private fun XiangqiLogo(
    glyphSizeSp: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.39f
            drawCircle(
                color = XiangqiRed,
                radius = radius,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = XiangqiRed,
                radius = radius * 0.82f,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        Text(
            text = "象",
            color = XiangqiRed,
            fontSize = glyphSizeSp.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OthelloLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radius = size.minDimension * 0.215f
        val outline = 1.5.dp.toPx()
        val shadowOffset = 1.2.dp.toPx()
        val blackCenter = Offset(size.width * 0.31f, size.height * 0.45f)
        val whiteCenter = Offset(size.width * 0.53f, size.height * 0.52f)
        val splitCenter = Offset(size.width * 0.72f, size.height * 0.59f)

        listOf(blackCenter, whiteCenter, splitCenter).forEach { center ->
            drawCircle(
                color = PieceShadow,
                radius = radius,
                center = center + Offset(shadowOffset, shadowOffset)
            )
        }

        drawCircle(color = LogoInk, radius = radius, center = blackCenter)

        drawCircle(color = LogoInk, radius = radius + outline, center = whiteCenter)
        drawCircle(color = LogoPorcelain, radius = radius, center = whiteCenter)

        drawCircle(color = LogoPorcelain, radius = radius, center = splitCenter)
        drawArc(
            color = LogoInk,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(splitCenter.x - radius, splitCenter.y - radius),
            size = Size(radius * 2f, radius * 2f)
        )
        drawCircle(
            color = LogoInk,
            radius = radius,
            center = splitCenter,
            style = Stroke(width = outline)
        )
    }
}
