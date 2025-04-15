package bpm.mc.selection

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import net.minecraft.core.BlockPos
import java.util.*

class PacketSelectionSync(
    var state: SelectionManager.SelectionState? = null,
) : Packet {

    override fun serialize(buffer: Buffer) {
        if (state == null) return
        Serial.write(buffer, state!!)
    }

    override fun deserialize(buffer: Buffer) {
        state = Serial.read(buffer)
    }
}