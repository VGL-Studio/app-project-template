package com.vglstudio.template.core.assets

import com.vglstudio.template.core.logging.LogEntry
import com.vglstudio.template.core.logging.LogLevel
import com.vglstudio.template.core.logging.RecordingAppLogger
import java.io.IOException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DownloadFailureLogTest {
    private val path = AssetPath("images/v2/hero.webp")

    @Test
    fun wrappedError_namesEveryClassOfTheCauseChain() {
        val error = IOException("fetch failed", UnknownHostException("assets.example.com"))

        assertEquals(
            "Could not download images/v2/hero.webp: NETWORK (IOException <- UnknownHostException)",
            downloadFailureMessage(path, error),
        )
    }

    @Test
    fun httpError_addsTheStatus() {
        val message =
            downloadFailureMessage(path, HttpStatusException(404, "https://assets.example.com/images/v2/hero.webp"))

        assertEquals("Could not download images/v2/hero.webp: NOT_FOUND (HttpStatusException, status=404)", message)
        assertFalse(message.contains("assets.example.com"))
    }

    @Test
    fun longCauseChain_isCut() {
        var error: Throwable = UnknownHostException("h")
        repeat(8) { error = IOException("wrap", error) }

        val classes = downloadFailureMessage(path, error as IOException).substringAfter("(").substringBefore(")")

        assertEquals(5, classes.split(" <- ").size)
    }

    @Test
    fun logger_writesOneWarningWithTheErrorUnderTheAssetsTag() {
        val logger = RecordingAppLogger()
        val error = UnknownHostException("assets.example.com")

        downloadFailureLogger(logger)(path, error)

        assertEquals(
            listOf(
                LogEntry(
                    LogLevel.WARN,
                    "Assets",
                    "Could not download images/v2/hero.webp: NETWORK (UnknownHostException)",
                    error,
                ),
            ),
            logger.entries,
        )
    }
}
