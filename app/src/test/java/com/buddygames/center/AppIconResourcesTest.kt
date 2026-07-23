package com.buddygames.center

import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIconResourcesTest {
    @Test
    fun gameCenterUsesCurrentIndependentVersion() {
        val buildScript = repositoryRoot().resolve("app/build.gradle.kts").readText()

        assertTrue(buildScript.contains("""versionCode = 4"""))
        assertTrue(buildScript.contains("""versionName = "0.0.4""""))
    }

    @Test
    fun packagedApplicationNameIsGameCenter() {
        val app = repositoryRoot().resolve("app/src/main")
        val manifest = app.resolve("AndroidManifest.xml").readText()
        val strings = app.resolve("res/values/strings.xml")

        assertTrue(manifest.contains("""android:label="@string/app_name""""))
        assertTrue("The application name must be declared in strings.xml", strings.isFile)
        assertTrue(strings.readText().contains("""<string name="app_name">游戏中心</string>"""))
    }

    @Test
    fun logoSvgEmbedsTheExactApprovedArtwork() {
        val logoSvg = repositoryRoot().resolve("logo.svg")
        assertTrue("logo.svg must exist at the repository root", logoSvg.isFile)

        val svg = logoSvg.readText()
        val encodedPng = requireNotNull(
            Regex("""href="data:image/png;base64,([^"]+)"""")
                .find(svg)
                ?.groupValues
                ?.get(1)
        ) {
            "logo.svg must embed its approved PNG so the SVG is self-contained"
        }
        val png = Base64.getDecoder().decode(encodedPng)
        val image = requireNotNull(ImageIO.read(ByteArrayInputStream(png))) {
            "The embedded logo artwork must be a readable PNG"
        }

        assertEquals(1254, image.width)
        assertEquals(1254, image.height)
        assertEquals(APPROVED_LOGO_SHA256, png.sha256())
    }

    @Test
    fun manifestUsesCompleteLegacyAndAdaptiveLauncherIconResources() {
        val root = repositoryRoot()
        val app = root.resolve("app/src/main")
        val manifest = app.resolve("AndroidManifest.xml").readText()

        assertTrue(manifest.contains("""android:icon="@mipmap/ic_launcher""""))
        assertTrue(manifest.contains("""android:roundIcon="@mipmap/ic_launcher_round""""))
        assertTrue(app.resolve("res/mipmap-anydpi-v26/ic_launcher.xml").isFile)
        assertTrue(app.resolve("res/mipmap-anydpi-v26/ic_launcher_round.xml").isFile)
        assertTrue(app.resolve("res/drawable-nodpi/ic_launcher_foreground.png").isFile)
        assertTrue(app.resolve("res/values/ic_launcher_background.xml").isFile)

        listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi").forEach { density ->
            val directory = app.resolve("res/mipmap-$density")
            assertTrue(directory.resolve("ic_launcher.png").isFile)
            assertTrue(directory.resolve("ic_launcher_round.png").isFile)
        }
    }

    private tailrec fun repositoryRoot(
        directory: File = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
    ): File {
        if (directory.resolve("settings.gradle.kts").isFile) return directory
        return repositoryRoot(
            requireNotNull(directory.parentFile) {
                "Could not locate the repository root from ${System.getProperty("user.dir")}"
            }
        )
    }

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString(separator = "") { "%02x".format(it) }

    private companion object {
        const val APPROVED_LOGO_SHA256 =
            "7610f3f7206b10b2be58f399e0891fd34ef48408692fbb0ebe13a8cfca4a94f5"
    }
}
