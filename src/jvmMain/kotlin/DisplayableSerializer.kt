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
        "Node" to { Node().also { comp ->
            for ((k, v) in it) {
                comp.properties[k] = v
            }
        } },

        "Label" to {
            Label().also { comp ->
                for ((k, v) in it) {
                    comp.properties[k] = v
                }
            }
        },

        "Input" to {
            InputPin().also { comp ->
                for ((k, v) in it) {
                    comp.properties[k] = v
                }
            }
        },
        "Output" to { OutputPin().also { comp ->
            for ((k, v) in it) {
                comp.properties[k] = v
            }
        } },

        "Buffer" to { Buffer().also { comp ->
            for ((k, v) in it) {
                comp.properties[k] = v
            }
        } },
        "NOT Gate" to { NotGate().also { comp ->
            for ((k, v) in it) {
                comp.properties[k] = v
            }
        } },
        "AND Gate" to { AndGate().also { comp ->
            for ((k, v) in it) {
                comp.properties[k] = v
            }
        } },
        "NAND Gate" to { NandGate().also { comp ->
            for ((k, v) in it) {
                comp.properties[k] = v
            }
        } },
        "OR Gate" to { OrGate().also { comp ->
            for ((k, v) in it) {
                comp.properties[k] = v
            }
        } },
        "NOR Gate" to { NorGate().also { comp ->
            for ((k, v) in it) {
                comp.properties[k] = v
            }
        } },
        "XOR Gate" to { XorGate().also { comp ->
            for ((k, v) in it) {
                comp.properties[k] = v
            }
        } },
        "XNOR Gate" to { XnorGate().also { comp ->
            for ((k, v) in it) {
                comp.properties[k] = v
            }
        } },
    )
}