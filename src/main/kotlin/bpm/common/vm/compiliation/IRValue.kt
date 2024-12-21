package bpm.common.vm.compiliation

sealed class IRValue { data class String(val value: kotlin.String) : IRValue()
    data class Int(val value: kotlin.Int) : IRValue()
    data class Float(val value: kotlin.Float) : IRValue()
    data class Boolean(val value: kotlin.Boolean) : IRValue()
    object Null : IRValue()
}