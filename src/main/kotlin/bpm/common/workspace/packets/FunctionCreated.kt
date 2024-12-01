package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import bpm.common.workspace.graph.Function

data class FunctionCreated(var function: Function = Function()) : Packet {

    override fun serialize(buffer: Buffer) {
        Serial.write<Function>(buffer, function)
    }

    override fun deserialize(buffer: Buffer) {
        function = Serial.read(buffer) ?: error("Failed to read function from buffer!")
    }
}