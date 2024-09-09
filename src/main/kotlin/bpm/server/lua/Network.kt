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
        val result = PipeNetManager.getControllerPosition(uuid)
        return result
    }


    @JvmStatic
    fun getInputs(uuid: String): IItemHandler? {
        return getHandlers(uuid, setOf(ProxiedType.INPUT))
    }

    @JvmStatic
    fun getOutputs(uuid: String): IItemHandler? {
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


    private fun getHandlers(uuid: String, types: Set<ProxiedType>): IItemHandler? {
        val workspaceUUID = UUID.fromString(uuid)
        val network = PipeNetManager.getNetwork(workspaceUUID) ?: return null
        val proxies = PipeNetManager.getProxies(workspaceUUID)
        val world = PipeNetManager.getWorldForController(workspaceUUID) ?: return null

        val handlers = proxies.flatMap { proxyState ->
            proxyState.proxiedBlocks.map { (relativePos, proxiedState) ->
                val absolutePos = proxyState.origin.offset(relativePos)
                val facesWithHandlers = Direction.entries.associate { direction ->
                    direction to when {
                        proxiedState.getProxiedType(direction) in types -> getItemHandler(world, absolutePos, direction)
                        else -> null
                    }
                }
                absolutePos to facesWithHandlers
            }
        }.toMap()

        return CombinedItemHandler(handlers)
    }

    private fun getItemHandler(world: ServerLevel, pos: BlockPos, direction: Direction): IItemHandler? {
        return world.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction)
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

class CombinedItemHandler(private val handlers: Map<BlockPos, Map<Direction, IItemHandler?>>) : IItemHandler {

    private val flatHandlers = handlers.values.flatMap { it.values }.filterNotNull()
    private val slotMapping = mutableMapOf<Int, Pair<IItemHandler, Int>>()

    init {
        var slotIndex = 0
        for (handler in flatHandlers) {
            for (i in 0 until handler.slots) {
                slotMapping[slotIndex] = handler to i
                slotIndex++
            }
        }
    }

    override fun getSlots(): Int = slotMapping.size

    override fun getStackInSlot(slot: Int): ItemStack {
        val (handler, handlerSlot) = slotMapping[slot] ?: return ItemStack.EMPTY
        return handler.getStackInSlot(handlerSlot)
    }

    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
        val (handler, handlerSlot) = slotMapping[slot] ?: return stack
        return handler.insertItem(handlerSlot, stack, simulate)
    }

    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
        val (handler, handlerSlot) = slotMapping[slot] ?: return ItemStack.EMPTY
        return handler.extractItem(handlerSlot, amount, simulate)
    }

    override fun getSlotLimit(slot: Int): Int {
        val (handler, handlerSlot) = slotMapping[slot] ?: return 0
        return handler.getSlotLimit(handlerSlot)
    }

    override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
        val (handler, handlerSlot) = slotMapping[slot] ?: return false
        return handler.isItemValid(handlerSlot, stack)
    }

    fun getHandlerForSlot(slot: Int): Pair<BlockPos, Direction>? {
        val (handler, _) = slotMapping[slot] ?: return null
        for ((pos, facesMap) in handlers) {
            for ((direction, h) in facesMap) {
                if (h == handler) {
                    return pos to direction
                }
            }
        }
        return null
    }
}