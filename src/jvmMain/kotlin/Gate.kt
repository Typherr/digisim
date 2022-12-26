abstract class Gate(override val name: String, private val truthTable : HashMap<UInt, UInt>) : Component {
    override fun compute() {
        outputs = truthTable[inputs]
    }
}