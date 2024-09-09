package bpm.client.runtime

import bpm.client.runtime.ClientRuntime.Key

object Keyboard {

    private var state: Set<Key> = HashSet()
    private var lastState: Set<Key> = HashSet()

    internal fun update() {
        val currentState = HashSet<Key>()
        Key.entries.forEach { key ->
            if (Platform.isKeyDown(key)) {
                currentState.add(key)
            }
        }

        lastState = state
        state = currentState
    }

    fun isKeyDown(key: Key) : Boolean = state.contains(key)
    fun isKeyUp(key: Key) : Boolean = !state.contains(key)
    fun isKeyPressed(key: Key) : Boolean = state.contains(key) && !lastState.contains(key)
    fun isKeyReleased(key: Key) : Boolean = !state.contains(key) && lastState.contains(key)

}