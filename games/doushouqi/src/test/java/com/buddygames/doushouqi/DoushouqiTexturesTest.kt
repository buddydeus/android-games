package com.buddygames.doushouqi

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoushouqiTexturesTest {
    @Test
    fun loaderRejectsWrongBoundsBeforeFullDecode() {
        val file = temporaryFile()
        val decoder = RecordingDecoder(
            bounds = DoushouqiTextureMetadata(
                width = DoushouqiVisuals.BOARD_TEXTURE_SIZE - 1,
                height = DoushouqiVisuals.BOARD_TEXTURE_SIZE,
                mimeType = "image/png",
            ),
        )

        assertNull(
            loadDoushouqiTexture(
                file,
                DoushouqiVisuals.BOARD_TEXTURE_SIZE,
                DoushouqiVisuals.BOARD_TEXTURE_SIZE,
                decoder,
            ),
        )
        assertFalse(decoder.didFullyDecode)
    }

    @Test
    fun loaderRejectsNonPngBeforeFullDecode() {
        val file = temporaryFile()
        val decoder = RecordingDecoder(
            bounds = DoushouqiTextureMetadata(
                width = DoushouqiVisuals.PIECE_TEXTURE_SIZE,
                height = DoushouqiVisuals.PIECE_TEXTURE_SIZE,
                mimeType = "image/jpeg",
            ),
        )

        assertNull(
            loadDoushouqiTexture(
                file,
                DoushouqiVisuals.PIECE_TEXTURE_SIZE,
                DoushouqiVisuals.PIECE_TEXTURE_SIZE,
                decoder,
            ),
        )
        assertFalse(decoder.didFullyDecode)
    }

    @Test
    fun loaderRejectsFullDecodeWithChangedDimensions() {
        val file = temporaryFile()
        val expected = DoushouqiVisuals.PIECE_TEXTURE_SIZE
        val decoder = RecordingDecoder(
            bounds = DoushouqiTextureMetadata(expected, expected, "image/png"),
            decoded = DoushouqiTextureMetadata(expected, expected - 1, "image/png"),
        )

        assertNull(loadDoushouqiTexture(file, expected, expected, decoder))
        assertTrue(decoder.didFullyDecode)
    }

    private class RecordingDecoder(
        private val bounds: DoushouqiTextureMetadata,
        private val decoded: DoushouqiTextureMetadata = bounds,
    ) : DoushouqiTextureDecoder<DoushouqiTextureMetadata> {
        var didFullyDecode = false
            private set

        override fun decodeBounds(file: File): DoushouqiTextureMetadata = bounds

        override fun decode(file: File): DoushouqiTextureMetadata {
            didFullyDecode = true
            return decoded
        }

        override fun dimensions(texture: DoushouqiTextureMetadata): Pair<Int, Int> =
            texture.width to texture.height
    }

    private fun temporaryFile(): File =
        File.createTempFile("doushouqi-texture-", ".image").apply { deleteOnExit() }
}
