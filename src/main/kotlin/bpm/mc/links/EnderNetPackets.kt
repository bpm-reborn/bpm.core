package bpm.mc.links

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import java.util.*

class PacketEnderNetControllerAdded(
    var uuid: UUID = NetUtils.DefaultUUID, var worldPos: WorldPos = WorldPos(
        Level.OVERWORLD, BlockPos.ZERO, null
    )
) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(uuid)
        Serial.write(buffer, worldPos)
    }

    override fun deserialize(buffer: Buffer) {
        uuid = buffer.readUUID()
        worldPos = Serial.read(buffer) ?: throw NullPointerException()
    }
}

class PacketEnderNetControllerRemoved(var uuid: UUID = NetUtils.DefaultUUID) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(uuid)
    }

    override fun deserialize(buffer: Buffer) {
        uuid = buffer.readUUID()
    }
}

class PacketEnderNetControllerMoved(
    var uuid: UUID = NetUtils.DefaultUUID, var worldPos: WorldPos = WorldPos(
        Level.OVERWORLD, BlockPos.ZERO, null
    )
) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(uuid)
        Serial.write(buffer, worldPos)
    }

    override fun deserialize(buffer: Buffer) {
        uuid = buffer.readUUID()
        worldPos = Serial.read(buffer) ?: throw NullPointerException()
    }
}

class PacketEnderNetLinkAdded(
    var controllerUUID: UUID = NetUtils.DefaultUUID, var linkPos: WorldPos = WorldPos(
        Level.OVERWORLD, BlockPos.ZERO, null
    )
) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(controllerUUID)
        Serial.write(buffer, linkPos)
    }

    override fun deserialize(buffer: Buffer) {
        controllerUUID = buffer.readUUID()
        linkPos = Serial.read(buffer) ?: throw NullPointerException()
    }
}

class PacketEnderNetLinkRemoved(
    var controllerUUID: UUID = NetUtils.DefaultUUID, var linkPos: WorldPos = WorldPos(
        Level.OVERWORLD, BlockPos.ZERO, null
    )
) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(controllerUUID)
        Serial.write(buffer, linkPos)
    }

    override fun deserialize(buffer: Buffer) {
        controllerUUID = buffer.readUUID()
        linkPos = Serial.read(buffer) ?: throw NullPointerException()
    }
}

class PacketEnderNetLoad(var state: EnderNetState = EnderNetState()) : Packet {

    override fun serialize(buffer: Buffer) {
        Serial.write(buffer, state)
    }

    override fun deserialize(buffer: Buffer) {
        state = Serial.read(buffer) ?: throw NullPointerException()
    }
}