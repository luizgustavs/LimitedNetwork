package com.gxstxvx.limitednetwork.vpn

import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

data class DnsQuestion(val name: String, val type: Int, val endOffset: Int)

object DnsMessage {
    fun question(packet: ByteArray, length: Int): DnsQuestion? {
        if (length < 12 || readU16(packet, 4) != 1) return null
        var offset = 12
        val labels = mutableListOf<String>()
        while (offset < length) {
            val labelLength = packet[offset].toInt() and 0xff
            if (labelLength == 0) {
                offset++
                break
            }
            if (labelLength > 63 || offset + 1 + labelLength > length) return null
            labels += packet.copyOfRange(offset + 1, offset + 1 + labelLength).toString(Charsets.US_ASCII)
            offset += labelLength + 1
        }
        if (offset + 4 > length) return null
        return DnsQuestion(labels.joinToString("."), readU16(packet, offset), offset + 4)
    }

    fun overrideResponse(query: ByteArray, length: Int, question: DnsQuestion, address: InetAddress): ByteArray? {
        val recordType = when (address) {
            is Inet4Address -> 1
            is Inet6Address -> 28
            else -> return null
        }
        if (question.type != recordType && question.type != 255) return null

        return ByteArrayOutputStream().apply {
            write(query, 0, 2)
            write(byteArrayOf(0x81.toByte(), 0x80.toByte()))
            writeU16(1)
            writeU16(1)
            writeU16(0)
            writeU16(0)
            write(query, 12, question.endOffset - 12)
            write(byteArrayOf(0xC0.toByte(), 0x0C))
            writeU16(recordType)
            writeU16(1)
            write(byteArrayOf(0, 0, 0, 60))
            writeU16(address.address.size)
            write(address.address)
        }.toByteArray()
    }

    fun serverFailureResponse(query: ByteArray, length: Int, question: DnsQuestion): ByteArray =
        ByteArrayOutputStream().apply {
            write(query, 0, 2)
            write(byteArrayOf(0x81.toByte(), 0x82.toByte()))
            writeU16(1)
            writeU16(0)
            writeU16(0)
            writeU16(0)
            write(query, 12, question.endOffset - 12)
        }.toByteArray()

    private fun ByteArrayOutputStream.writeU16(value: Int) {
        write((value ushr 8) and 0xff)
        write(value and 0xff)
    }

    private fun readU16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)
}
