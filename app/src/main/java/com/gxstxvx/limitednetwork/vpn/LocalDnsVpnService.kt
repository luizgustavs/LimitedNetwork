package com.gxstxvx.limitednetwork.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gxstxvx.limitednetwork.MainActivity
import com.gxstxvx.limitednetwork.R
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class LocalDnsVpnService : VpnService() {
    private var tunnel: ParcelFileDescriptor? = null
    private var worker: Thread? = null
    private var upstreamDnsServers: List<InetAddress> = emptyList()
    private val running = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand action=${intent?.action} running=${running.get()}")
        if (intent?.action == ACTION_STOP) {
            VpnState.update(VpnConnectionState.Disconnecting)
            shutdown()
            return Service.START_NOT_STICKY
        }
        if (running.get()) return Service.START_STICKY

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        VpnState.update(VpnConnectionState.Connecting)
        startVpn()
        return Service.START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        shutdown()
        super.onDestroy()
    }

    override fun onRevoke() {
        Log.w(TAG, "onRevoke")
        shutdown()
        super.onRevoke()
    }

    private fun startVpn() {
        try {
            upstreamDnsServers = findUpstreamDnsServers()
            tunnel = Builder()
                .setSession(getString(R.string.app_name))
                .setMtu(MTU)
                .addAddress(VPN_ADDRESS, 24)
                .addDnsServer(VPN_DNS_ADDRESS)
                .addRoute(VPN_DNS_ADDRESS, 32)
                .setBlocking(true)
                .establish() ?: error("Não foi possível criar o túnel VPN")
            running.set(true)
            worker = Thread(::runDnsProxy, "limited-network-dns").also { it.start() }
            VpnState.update(VpnConnectionState.Connected)
        } catch (error: Exception) {
            VpnState.update(VpnConnectionState.Error(error.message ?: "Falha ao iniciar VPN"))
            shutdown(updateState = false)
        }
    }

    private fun runDnsProxy() {
        try {
            val descriptor = tunnel?.fileDescriptor ?: error("Túnel indisponível")
            val input = FileInputStream(descriptor)
            val output = FileOutputStream(descriptor)
            val hosts = HostsRepository(this)
            val buffer = ByteArray(MTU)
            VpnState.clearRequests()
            while (running.get()) {
                val length = input.read(buffer)
                if (length <= 0) continue
                val request = Ipv4UdpPacket.parse(buffer, length) ?: continue
                if (request.destinationPort != DNS_PORT) continue
                val question = DnsMessage.question(request.bytes, request.bytes.size) ?: continue
                val blockedAddress = hosts.resolve(question.name)
                VpnState.recordRequest(question.name, blockedAddress != null)
                val dnsResponse = try {
                    blockedAddress?.let {
                        DnsMessage.overrideResponse(request.bytes, request.bytes.size, question, it)
                    } ?: forwardDns(request.bytes)
                    ?: DnsMessage.serverFailureResponse(request.bytes, request.bytes.size, question)
                } catch (error: Exception) {
                    Log.w(TAG, "Falha ao processar consulta DNS para ${question.name}", error)
                    DnsMessage.serverFailureResponse(request.bytes, request.bytes.size, question)
                }
                output.write(Ipv4UdpPacket.response(request, dnsResponse))
                output.flush()
            }
        } catch (error: Exception) {
            val unexpectedFailure = running.getAndSet(false)
            if (unexpectedFailure) {
                Log.e(TAG, "O túnel VPN foi interrompido", error)
                VpnState.update(VpnConnectionState.Error("O filtro DNS foi interrompido"))
            }
        } finally {
            shutdown(updateState = false)
        }
    }

    private fun forwardDns(query: ByteArray): ByteArray? {
        for (upstream in upstreamDnsServers) {
            val response = runCatching {
                DatagramSocket().use { socket ->
                    check(protect(socket)) { "Não foi possível proteger a consulta DNS" }
                    socket.soTimeout = DNS_TIMEOUT_MS
                    socket.send(DatagramPacket(query, query.size, upstream, DNS_PORT))
                    val bytes = ByteArray(MAX_DNS_PACKET)
                    val packet = DatagramPacket(bytes, bytes.size)
                    socket.receive(packet)
                    bytes.copyOf(packet.length)
                }
            }.onFailure { error ->
                Log.w(TAG, "DNS ${upstream.hostAddress} indisponível", error)
            }.getOrNull()
            if (response != null) return response
        }
        return null
    }

    private fun findUpstreamDnsServers(): List<InetAddress> {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val networkDns = connectivityManager.activeNetwork
            ?.let(connectivityManager::getLinkProperties)
            ?.dnsServers
            .orEmpty()
        return (networkDns + FALLBACK_DNS.map(InetAddress::getByName)).distinctBy { it.hostAddress }
    }

    @Synchronized
    private fun shutdown(updateState: Boolean = true) {
        Log.i(TAG, "shutdown updateState=$updateState")
        running.set(false)
        tunnel?.close()
        tunnel = null
        worker?.interrupt()
        worker = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        if (updateState) VpnState.update(VpnConnectionState.Disconnected)
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "VPN", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Limited Network")
        .setContentText("Filtro DNS ativo")
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    companion object {
        private const val ACTION_START = "com.gxstxvx.limitednetwork.START"
        private const val ACTION_STOP = "com.gxstxvx.limitednetwork.STOP"
        private const val CHANNEL_ID = "vpn_status"
        private const val NOTIFICATION_ID = 1001
        private const val VPN_ADDRESS = "10.7.0.1"
        private const val VPN_DNS_ADDRESS = "10.7.0.2"
        private const val DNS_PORT = 53
        private const val DNS_TIMEOUT_MS = 2_000
        private const val MAX_DNS_PACKET = 4096
        private const val MTU = 1500
        private const val TAG = "LocalDnsVpnService"
        private val FALLBACK_DNS = listOf("1.1.1.1", "8.8.8.8")

        fun start(context: Context) {
            VpnState.update(VpnConnectionState.Connecting)
            ContextCompat.startForegroundService(
                context,
                Intent(context, LocalDnsVpnService::class.java).setAction(ACTION_START)
            )
        }

        fun stop(context: Context) {
            VpnState.update(VpnConnectionState.Disconnecting)
            context.startService(
                Intent(context, LocalDnsVpnService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
