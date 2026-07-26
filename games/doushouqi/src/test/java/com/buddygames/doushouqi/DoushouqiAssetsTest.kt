package com.buddygames.doushouqi

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoushouqiAssetsTest {
    @Test
    fun packageIconIsReadableCircularSafePng() {
        val image = ImageIO.read(
            repositoryRoot().resolve("games/doushouqi/package/assets/icon.png"),
        )
        assertNotNull(image)
        assertEquals(1024, image.width)
        assertEquals(1024, image.height)
        assertTrue(cornersAreTransparentOrCanvasSafe(image))
        assertTrue(subjectCoverage(image) in 0.20f..0.72f)
    }

    private fun cornersAreTransparentOrCanvasSafe(image: BufferedImage): Boolean {
        val corners = listOf(
            image.getRGB(0, 0),
            image.getRGB(image.width - 1, 0),
            image.getRGB(0, image.height - 1),
            image.getRGB(image.width - 1, image.height - 1),
        )
        if (corners.all { color -> color ushr 24 == 0 }) return true
        val reference = corners.first()
        return corners.all { color ->
            listOf(16, 8, 0).all { shift ->
                kotlin.math.abs(
                    ((reference shr shift) and 0xFF) - ((color shr shift) and 0xFF),
                ) <= 12
            }
        }
    }

    private fun subjectCoverage(image: BufferedImage): Float {
        val background = image.getRGB(0, 0)
        var subject = 0
        var samples = 0
        for (y in 0 until image.height step 8) {
            for (x in 0 until image.width step 8) {
                samples++
                val color = image.getRGB(x, y)
                val differs = listOf(16, 8, 0).any { shift ->
                    kotlin.math.abs(
                        ((background shr shift) and 0xFF) -
                            ((color shr shift) and 0xFF),
                    ) > 24
                }
                if (differs) subject++
            }
        }
        return subject.toFloat() / samples
    }

    private tailrec fun repositoryRoot(
        directory: File = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile,
    ): File {
        if (directory.resolve("settings.gradle.kts").isFile) return directory
        return repositoryRoot(requireNotNull(directory.parentFile))
    }
}
