import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = CircuitPairSerializer::class)
data class CircuitPair(val component: Component, val index: Int)

@Serializable
@SerialName("CircuitPair")
private data class CircuitPairSurrogate(val component: Component, val index: Int, val hashCode: Int)

object CircuitPairSerializer: KSerializer<CircuitPair> {
    private val instanceCache = mutableMapOf<Int, Component>()

    override val descriptor: SerialDescriptor = CircuitPairSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: CircuitPair) {
        val surrogate = CircuitPairSurrogate(value.component, value.index, value.component.hashCode())
        encoder.encodeSerializableValue(CircuitPairSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): CircuitPair {
        val surrogate = decoder.decodeSerializableValue(CircuitPairSurrogate.serializer())
        if (!instanceCache.containsKey(surrogate.hashCode)) {
            instanceCache[surrogate.hashCode] = surrogate.component
        }
        return CircuitPair(instanceCache[surrogate.hashCode]!!, surrogate.index)
    }
}

infix fun Component.index(index: Int) = CircuitPair(this, index)