package com.gxstxvx.limitednetwork.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.InetAddress

class DnsMessageTest {
    @Test
    fun parsesQuestionAndCreatesIpv4Override() {
        val query = query("blocked.example")
        val question = DnsMessage.question(query, query.size)

        assertNotNull(question)
        assertEquals("blocked.example", question?.name)
        assertEquals(1, question?.type)
        val response = DnsMessage.overrideResponse(
            query,
            query.size,
            question!!,
            InetAddress.getByName("10.0.0.8")
        )
        assertNotNull(response)
        assertEquals(1, ((response!![6].toInt() and 0xff) shl 8) or (response[7].toInt() and 0xff))
    }

    @Test
    fun createsServerFailureWithoutDroppingTheQuery() {
        val query = query("unavailable.example")
        val question = DnsMessage.question(query, query.size)!!

        val response = DnsMessage.serverFailureResponse(query, query.size, question)

        assertEquals(2, response[3].toInt() and 0x0f)
        assertEquals(query.size, response.size)
    }

    private fun query(hostname: String): ByteArray = ByteArrayOutputStream().apply {
        write(byteArrayOf(0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
        hostname.split('.').forEach { label ->
            write(label.length)
            write(label.toByteArray())
        }
        write(byteArrayOf(0, 0, 1, 0, 1))
    }.toByteArray()
}
