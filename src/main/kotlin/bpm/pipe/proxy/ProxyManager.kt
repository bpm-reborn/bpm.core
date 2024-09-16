package bpm.pipe.proxy

import bpm.common.network.Listener
import bpm.common.network.Server
import bpm.common.packets.Packet
import bpm.pipe.PipeNetManager
import net.minecraft.core.BlockPos
import java.util.*


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
            val proxyOrigin = packet.proxyOrigin
            val absoluteProxiedState = packet.proxiedState
            absoluteProxiedState.absolutePos = proxyOrigin.offset(absoluteProxiedState.relativePos)
            //Filter out any pro
            proxy[absoluteProxiedState.absolutePos] = absoluteProxiedState
        } else if (packet is PacketProxyRequest) {
            val proxy = ProxyManager[packet.proxyOrigin] ?: return
            //Filter out any that have all none for the proxied type
            val blocks = proxy.proxiedBlocks.filter { it.key != packet.proxyOrigin }
            proxy.proxiedBlocks.clear()
            proxy.proxiedBlocks.putAll(blocks)
            Server.send(PacketProxyResponse(packet.proxyOrigin, proxy), from)
        } else if (packet is PacketProxiedStatesRequest) {
            val uuid = packet.workspaceUid
            //Find all the proxies states assosiated with the workspace
            val proxies = PipeNetManager.getProxies(uuid)
            //Send the proxies states to the client
            Server.send(PacketProxiedStatesResponse(uuid, proxies.toMutableList()), from)
        }
    }

}