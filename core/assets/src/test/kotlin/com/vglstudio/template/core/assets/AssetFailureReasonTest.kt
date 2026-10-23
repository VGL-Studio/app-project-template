package com.vglstudio.template.core.assets

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AssetFailureReasonTest {
    @Test
    fun unknownHost_isNetwork() {
        assertEquals(AssetFailureReason.NETWORK, UnknownHostException("assets.example.com").toFailureReason())
    }

    @Test
    fun connectException_isNetwork() {
        assertEquals(AssetFailureReason.NETWORK, ConnectException("refused").toFailureReason())
    }

    @Test
    fun socketTimeout_isNetwork() {
        assertEquals(AssetFailureReason.NETWORK, SocketTimeoutException("timeout").toFailureReason())
    }

    @Test
    fun networkErrorAsCause_isNetwork() {
        val wrapped = IOException("download failed", UnknownHostException("assets.example.com"))
        assertEquals(AssetFailureReason.NETWORK, wrapped.toFailureReason())
    }

    @Test
    fun http404_isNotFound() {
        assertEquals(AssetFailureReason.NOT_FOUND, HttpStatusException(404, "https://x/y").toFailureReason())
    }

    @Test
    fun http500_isOther() {
        assertEquals(AssetFailureReason.OTHER, HttpStatusException(500, "https://x/y").toFailureReason())
    }

    @Test
    fun plainIOException_isOther() {
        assertEquals(AssetFailureReason.OTHER, IOException("disk full").toFailureReason())
    }

    @Test
    fun logMessage_namesReasonAndErrorClass() {
        val error = UnknownHostException("assets.example.com")
        val message = downloadFailureMessage(AssetPath("images/v2/hero.webp"), error)

        assertEquals("Could not download images/v2/hero.webp: NETWORK (UnknownHostException)", message)
        assertFalse(message.contains("assets.example.com"))
    }
}
