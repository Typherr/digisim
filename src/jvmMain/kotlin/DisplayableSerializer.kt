import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
@SerialName("Displayable")
private class DisplayableSurrogate(val name: String, val properties: Map<String, String>)

object DisplayableSerializer : KSerializer<Displayable> {
    override val descriptor: SerialDescriptor = DisplayableSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Displayable) {
        val surrogate = DisplayableSurrogate(value.name, value.properties)
        encoder.encodeSerializableValue(DisplayableSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Displayable {
        val surrogate = decoder.decodeSerializableValue(DisplayableSurrogate.serializer())
        return constructors[surrogate.name]!!(surrogate.properties)
    }

    val constructors = mapOf<String, (Map<String, String>) -> Displayable>(
        "Label" to {
            val comp = Label()
            for ((k, v) in it) {
                comp.properties[k] = v
            }
            comp
        },

        "Input" to {
            val comp = InputPin()
            for ((k, v) in it) {
                comp.properties[k] = v
            }
            comp
        },
        "Output" to { OutputPin() },

        "Buffer" to { Buffer() },
        "NOT Gate" to { NotGate() },
        "AND Gate" to { AndGate() },
        "NAND Gate" to { NandGate() },
        "OR Gate" to { OrGate() },
        "NOR Gate" to { NorGate() },
        "XOR Gate" to { XorGate() },
        "XNOR Gate" to { XnorGate() },
    )
}