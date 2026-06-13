package com.gxstxvx.limitednetwork.vpn

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class UdpPayload(
    val sourceAddress: ByteArray,
    val destinationAddress: ByteArray,
    val sourcePort: Int,
    val destinationPort: Int,
    val bytes: ByteArray
)

object Ipv4UdpPacket {
    fun parse(packet: ByteArray, length: Int): UdpPayload? {
        if (length < 28 || (packet[0].toInt() ushr 4) != 4 || packet[9].toInt() != 17) return null
        val ipHeaderLength = (packet[0].toInt() and 0x0f) * 4
        if (ipHeaderLength < 20 || length < ipHeaderLength + 8) return null
        val udpLength = readU16(packet, ipHeaderLength + 4)
        if (udpLength < 8 || ipHeaderLength + udpLength > length) return null
        return UdpPayload(
            packet.copyOfRange(12, 16),
            packet.copyOfRange(16, 20),
            readU16(packet, ipHeaderLength),
            readU16(packet, ipHeaderLength + 2),
            packet.copyOfRange(ipHeaderLength + 8, ipHeaderLength + udpLength)
        )
    }

    fun response(request: UdpPayload, payload: ByteArray): ByteArray {
        val totalLength = 20 + 8 + payload.size
        val result = ByteArray(totalLength)
        val buffer = ByteBuffer.wrap(result).order(ByteOrder.BIG_ENDIAN)
        buffer.put(0x45.toByte()).put(0).putShort(totalLength.toShort())
        buffer.putShort(0).putShort(0).put(64).put(17).putShort(0)
        buffer.put(request.destinationAddress).put(request.sourceAddress)
        buffer.putShort(request.destinationPort.toShort()).putShort(request.sourcePort.toShort())
        buffer.putShort((8 + payload.size).toShort()).putShort(0).put(payload)
        putChecksum(result, 10, checksum(result, 0, 20))
        val pseudoHeader = ByteBuffer.allocate(12 + 8 + payload.size).order(ByteOrder.BIG_ENDIAN)
            .put(request.destinationAddress).put(request.sourceAddress)
            .put(0).put(17).putShort((8 + payload.size).toShort())
            .put(result, 20, 8 + payload.size).array()
        putChecksum(result, 26, checksum(pseudoHeader, 0, pseudoHeader.size))
        return result
    }

    private fun readU16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)

    private fun putChecksum(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value ushr 8).toByte()
        bytes[offset + 1] = value.toByte()
    }

    private fun checksum(bytes: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var index = offset
        while (index + 1 < offset + length) {
            sum += readU16(bytes, index).toLong()
            index += 2
        }
        if (index < offset + length) sum += (bytes[index].toInt() and 0xff).toLong() shl 8
        while (sum ushr 16 != 0L) sum = (sum and 0xffff) + (sum ushr 16)
        return sum.inv().toInt() and 0xffff
    }
}
