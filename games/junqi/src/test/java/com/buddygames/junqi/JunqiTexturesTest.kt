package com.buddygames.junqi

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JunqiTexturesTest {
    @Test
    fun loaderRejectsEachAssetWhenBoundsDimensionsDoNotMatchItsContract() {
        val files = JunqiVisuals.textureContracts.associateWith { temporaryFile() }
        val decoder = RecordingTextureDecoder(
            bounds = files.map { (contract, file) ->
                file.path to when (contract) {
                    JunqiVisuals.BOARD_TEXTURE_CONTRACT -> JunqiTextureMetadata(
                    width = JunqiVisuals.BOARD_TEXTURE_WIDTH - 1,
                    height = JunqiVisuals.BOARD_TEXTURE_HEIGHT,
                    mimeType = "image/png",
                    )
                    JunqiVisuals.ICON_TEXTURE_CONTRACT -> JunqiTextureMetadata(
                    width = JunqiVisuals.ICON_TEXTURE_WIDTH,
                    height = JunqiVisuals.ICON_TEXTURE_HEIGHT - 1,
                    mimeType = "image/png",
                    )
                    JunqiVisuals.SHELF_TEXTURE_CONTRACT -> JunqiTextureMetadata(
                    width = JunqiVisuals.SHELF_TEXTURE_WIDTH,
                    height = JunqiVisuals.SHELF_TEXTURE_HEIGHT + 1,
                    mimeType = "image/png",
                    )
                    else -> error("Unexpected Junqi texture contract: $contract")
                }
            }.toMap(),
        )

        JunqiVisuals.textureContracts.forEach { contract ->
            assertNull(loadJunqiTexture(files.getValue(contract), contract, decoder))
        }

        assertFalse(decoder.didFullyDecode)
    }

    @Test
    fun loaderRejectsDecodableNonPngBeforeFullDecodeAndReturnsFallbackNull() {
        val contract = JunqiVisuals.BOARD_TEXTURE_CONTRACT
        val file = temporaryFile()
        val decoder = RecordingTextureDecoder(
            bounds = mapOf(
                file.path to JunqiTextureMetadata(
                    width = contract.width,
                    height = contract.height,
                    mimeType = "image/jpeg",
                ),
            ),
        )

        assertNull(loadJunqiTexture(file, contract, decoder))
        assertFalse(decoder.didFullyDecode)
    }

    @Test
    fun loaderRejectsAFullDecodeWhoseDimensionsNoLongerMatchVerifiedBounds() {
        val contract = JunqiVisuals.SHELF_TEXTURE_CONTRACT
        val file = temporaryFile()
        val decoder = RecordingTextureDecoder(
            bounds = mapOf(
                file.path to JunqiTextureMetadata(
                    width = contract.width,
                    height = contract.height,
                    mimeType = "image/png",
                ),
            ),
            decoded = mapOf(
                file.path to JunqiTextureMetadata(
                    width = contract.width - 1,
                    height = contract.height,
                    mimeType = "image/png",
                ),
            ),
        )

        assertNull(loadJunqiTexture(file, contract, decoder))
        assertTrue(decoder.didFullyDecode)
    }

    private class RecordingTextureDecoder(
        private val bounds: Map<String, JunqiTextureMetadata>,
        private val decoded: Map<String, JunqiTextureMetadata> = bounds,
    ) : JunqiTextureDecoder<JunqiTextureMetadata> {
        var didFullyDecode = false
            private set

        override fun decodeBounds(file: File): JunqiTextureMetadata? = bounds[file.path]

        override fun decode(file: File): JunqiTextureMetadata? {
            didFullyDecode = true
            return decoded[file.path]
        }

        override fun dimensions(texture: JunqiTextureMetadata): Pair<Int, Int> = texture.width to texture.height
    }

    private fun temporaryFile(): File = File.createTempFile("junqi-texture-", ".image").apply {
        deleteOnExit()
    }
}
