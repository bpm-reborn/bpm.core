package bpm.server.lua

import bpm.pipe.PipeNetManager
import bpm.pipe.proxy.ProxiedType
import net.minecraft.server.level.ServerLevel
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler
import java.util.*

import net.minecraft.world.item.ItemStack
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

object Network : LuaBuiltin {

    @JvmStatic
    fun getControllerPosition(uuid: String): BlockPos? {
        val uuid = UUID.fromString(uuid)
        return PipeNetManager.getControllerPosition(uuid)
    }

    @JvmStatic
    fun getInputs(uuid: String): CombinedItemHandler? {
        return getHandlers(uuid, setOf(ProxiedType.INPUT))
    }

    @JvmStatic
    fun getOutputs(uuid: String): CombinedItemHandler? {
        return getHandlers(uuid, setOf(ProxiedType.OUTPUT))
    }

    @JvmStatic
    fun transferItem(from: IItemHandler, fromSlot: Int, to: IItemHandler, toSlot: Int, maxAmount: Int): Int {
        val extracted = from.extractItem(fromSlot, maxAmount, true)
        if (extracted.isEmpty) return 0

        val inserted = to.insertItem(toSlot, extracted, true)
        val amountToTransfer = extracted.count - inserted.count

        if (amountToTransfer > 0) {
            val actualExtracted = from.extractItem(fromSlot, amountToTransfer, false)
            val actualInserted = to.insertItem(toSlot, actualExtracted, false)
            return actualExtracted.count - actualInserted.count
        }

        return 0
    }

    private fun getHandlers(uuid: String, types: Set<ProxiedType>): CombinedItemHandler? {
        val workspaceUUID = UUID.fromString(uuid)
        val proxies = PipeNetManager.getProxies(workspaceUUID)
        val world = PipeNetManager.getWorldForController(workspaceUUID) ?: return null

        val handlers = proxies.flatMap { proxyState ->
            proxyState.proxiedBlocks.mapNotNull { (relativePos, proxiedState) ->
                val absolutePos = proxyState.origin.offset(relativePos)
                val facesWithHandlers = Direction.entries.mapNotNull { direction ->
                    when {
                        proxiedState.getProxiedType(direction) in types -> {
                            val handler = getItemHandler(world, absolutePos, direction)
                            if (handler != null) direction to handler else null
                        }
                        else -> null
                    }
                }.toMap()

                if (facesWithHandlers.isNotEmpty()) absolutePos to facesWithHandlers else null
            }
        }.toMap()

        return if (handlers.isNotEmpty()) CombinedItemHandler(handlers) else null
    }

    private fun getItemHandler(world: ServerLevel, pos: BlockPos, direction: Direction): IItemHandler? {
        return world.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction)?.let { handler ->
            if (handler.slots > 0) handler else null
        }
    }

    @JvmStatic
    fun extractItem(handler: IItemHandler, slot: Int, amount: Int, simulate: Boolean): ItemStack {
        return handler.extractItem(slot, amount, simulate)
    }

    @JvmStatic
    fun insertItem(handler: IItemHandler, slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
        return handler.insertItem(slot, stack, simulate)
    }

    override val name: String = "Network"
}

class CombinedItemHandler(private val handlers: Map<BlockPos, Map<Direction, IItemHandler>>) : IItemHandler {
    private val slotMapping = mutableMapOf<Int, Triple<BlockPos, Direction, Int>>()

    init {
        var slotIndex = 0
        for ((pos, facesMap) in handlers) {
            for ((direction, handler) in facesMap) {
                for (i in 0 until handler.slots) {
                    slotMapping[slotIndex] = Triple(pos, direction, i)
                    slotIndex++
                }
            }
        }
    }

    override fun getSlots(): Int = slotMapping.size

    override fun getStackInSlot(slot: Int): ItemStack {
        val (pos, direction, handlerSlot) = slotMapping[slot] ?: return ItemStack.EMPTY
        return handlers[pos]?.get(direction)?.getStackInSlot(handlerSlot) ?: ItemStack.EMPTY
    }

    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
        val (pos, direction, handlerSlot) = slotMapping[slot] ?: return stack
        return handlers[pos]?.get(direction)?.insertItem(handlerSlot, stack, simulate) ?: stack
    }

    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
        val (pos, direction, handlerSlot) = slotMapping[slot] ?: return ItemStack.EMPTY
        return handlers[pos]?.get(direction)?.extractItem(handlerSlot, amount, simulate) ?: ItemStack.EMPTY
    }

    override fun getSlotLimit(slot: Int): Int {
        val (pos, direction, handlerSlot) = slotMapping[slot] ?: return 0
        return handlers[pos]?.get(direction)?.getSlotLimit(handlerSlot) ?: 0
    }

    override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
        val (pos, direction, handlerSlot) = slotMapping[slot] ?: return false
        return handlers[pos]?.get(direction)?.isItemValid(handlerSlot, stack) ?: false
    }

    fun getHandlerForSlot(slot: Int): Pair<BlockPos, Direction>? {
        val (pos, direction, _) = slotMapping[slot] ?: return null
        return pos to direction
    }
}