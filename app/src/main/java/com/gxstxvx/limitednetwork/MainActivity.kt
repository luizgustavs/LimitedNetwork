package com.gxstxvx.limitednetwork

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.gxstxvx.limitednetwork.vpn.LocalDnsVpnService
import com.gxstxvx.limitednetwork.vpn.DnsRequestEvent
import com.gxstxvx.limitednetwork.vpn.VpnConnectionState
import com.gxstxvx.limitednetwork.vpn.VpnState
import com.gxstxvx.limitednetwork.ui.theme.LimitedNetworkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LimitedNetworkTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Black
                ) { innerPadding ->
                    VpnScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun VpnScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state by VpnState.state
    val requests = VpnState.requests
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            LocalDnsVpnService.start(context)
        } else {
            VpnState.permissionDenied()
        }
    }
    fun enableVpn() {
        val permissionIntent: Intent? = VpnService.prepare(context)
        if (permissionIntent != null) {
            vpnPermissionLauncher.launch(permissionIntent)
        } else {
            LocalDnsVpnService.start(context)
        }
    }

    VpnContent(
        state = state,
        requests = requests,
        onCheckedChange = { checked ->
            if (checked) enableVpn() else LocalDnsVpnService.stop(context)
        },
        modifier = modifier
    )
}

@Composable
private fun VpnContent(
    state: VpnConnectionState,
    requests: List<DnsRequestEvent>,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val checked = state == VpnConnectionState.Connected
    val enabled = state !is VpnConnectionState.Connecting &&
        state !is VpnConnectionState.Disconnecting
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 99.dp else 10.dp,
        label = "Posição do switch"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        RequestActivity(
            requests = requests,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Text(
                text = "Limited Network",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .size(width = 169.dp, height = 80.dp)
                    .background(
                        color = (if (checked) Color(0xFFFF2D9A) else Color(0xFF9E9E9E))
                            .copy(alpha = if (enabled) 1f else 0.6f),
                        shape = RoundedCornerShape(40.dp)
                    )
                    .toggleable(
                        value = checked,
                        enabled = enabled,
                        role = Role.Switch,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onValueChange = onCheckedChange
                    )
                    .semantics { contentDescription = "Ligar ou desligar VPN" }
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset, y = 10.dp)
                        .size(60.dp)
                        .background(
                            color = Color.White.copy(alpha = if (enabled) 1f else 0.8f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!enabled) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = Color.Black,
                            strokeWidth = 4.dp
                        )
                    }
                }
            }
            Text(
                text = state.label,
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RequestActivity(
    requests: List<DnsRequestEvent>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(requests, key = DnsRequestEvent::id) { request ->
            Text(
                text = request.address,
                color = if (request.blocked) {
                    Color.Red.copy(alpha = 0.30f)
                } else {
                    Color.White.copy(alpha = 0.14f)
                },
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VpnPreview() {
    LimitedNetworkTheme {
        VpnContent(
            state = VpnConnectionState.Connected,
            requests = listOf(
                DnsRequestEvent(1, "api.example.com", blocked = false),
                DnsRequestEvent(2, "ads.example.com", blocked = true)
            ),
            onCheckedChange = {}
        )
    }
}
