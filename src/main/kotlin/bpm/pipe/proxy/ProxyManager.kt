package bpm.pipe.proxy

import bpm.common.network.Listener
import bpm.common.network.Server
import bpm.common.packets.Packet
import net.minecraft.core.BlockPos
import java.util.*

//Todo: make this serializable by providing a serializer
object ProxyManager : Listener {
    private val proxies = mutableMapOf<BlockPos, ProxyState>()
    operator fun get(pos: BlockPos): ProxyState? = proxies[pos]
    operator fun set(pos: BlockPos, state: ProxyState) = let { proxies[pos] = state }

    fun remove(pos: BlockPos) = proxies.remove(pos)

    fun clear() = proxies.clear()

    fun contains(pos: BlockPos) = proxies.containsKey(pos)

    fun isEmpty() = proxies.isEmpty()


    fun forEach(action: (BlockPos, ProxyState) -> Unit) = proxies.forEach(action)

    val keys get() = proxies.keys
    val values get() = proxies.values
    val entries get() = proxies.entries
    val iterator get() = proxies.iterator()
    val size get() = proxies.size


    override fun onPacket(packet: Packet, from: UUID) {
        if (packet is PacketProxyUpdate) {
            val proxy = ProxyManager[packet.proxyOrigin] ?: return
            proxy[packet.proxiedState.relativePos] = packet.proxiedState
        } else if (packet is PacketProxyRequest) {
            val proxy = ProxyManager[packet.proxyOrigin] ?: return
            Server.send(PacketProxyResponse(packet.proxyOrigin, proxy), from)
        }
    }
}