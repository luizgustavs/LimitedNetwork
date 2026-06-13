package com.gxstxvx.limitednetwork.vpn

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import android.os.Handler
import android.os.Looper

data class DnsRequestEvent(
    val id: Long,
    val address: String,
    val blocked: Boolean
)

sealed interface VpnConnectionState {
    data object Disconnected : VpnConnectionState
    data object Connecting : VpnConnectionState
    data object Connected : VpnConnectionState
    data object Disconnecting : VpnConnectionState
    data class Error(val message: String) : VpnConnectionState

    val label: String
        get() = when (this) {
            Disconnected -> "VPN desligada"
            Connecting -> "Iniciando..."
            Connected -> "VPN ligada - Filtro DNS ativo"
            Disconnecting -> "Parando..."
            is Error -> message
        }
}

object VpnState {
    private const val MAX_VISIBLE_REQUESTS = 100
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutableState = mutableStateOf<VpnConnectionState>(VpnConnectionState.Disconnected)
    val state: State<VpnConnectionState> = mutableState
    private val mutableRequests = mutableStateListOf<DnsRequestEvent>()
    val requests: List<DnsRequestEvent> = mutableRequests
    private var nextRequestId = 0L

    fun update(value: VpnConnectionState) {
        mutableState.value = value
    }

    fun permissionDenied() {
        update(VpnConnectionState.Error("Permissão de VPN não concedida"))
    }

    fun recordRequest(address: String, blocked: Boolean) {
        mainHandler.post {
            mutableRequests += DnsRequestEvent(
                id = nextRequestId++,
                address = address,
                blocked = blocked
            )
            while (mutableRequests.size > MAX_VISIBLE_REQUESTS) {
                mutableRequests.removeAt(0)
            }
        }
    }

    fun clearRequests() {
        mainHandler.post { mutableRequests.clear() }
    }
}
