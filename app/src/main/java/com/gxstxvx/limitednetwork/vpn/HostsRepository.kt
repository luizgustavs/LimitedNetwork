package com.gxstxvx.limitednetwork.vpn

import android.content.Context
import java.net.InetAddress
import java.util.Locale

class HostsRepository(context: Context) {
    private val entries: Map<String, InetAddress> = context.assets.open(HOSTS_FILE).bufferedReader().useLines { lines ->
        lines.mapNotNull(::parseLine).toMap()
    }

    fun resolve(hostname: String): InetAddress? = entries[hostname.lowercase(Locale.ROOT).trimEnd('.')]

    private fun parseLine(line: String): Pair<String, InetAddress>? {
        val values = line.substringBefore('#').trim().split(Regex("\\s+"))
        if (values.size < 2) return null
        return runCatching {
            values[1].lowercase(Locale.ROOT).trimEnd('.') to InetAddress.getByName(values[0])
        }.getOrNull()
    }

    private companion object {
        const val HOSTS_FILE = "hosts.txt"
    }
}
