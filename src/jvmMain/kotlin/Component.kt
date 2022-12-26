
interface Component {
    // name of component
    val name : String

    // bitwise representation of all inputs
    var inputs : UInt?

    // bitwise representation of all outputs
    var outputs : UInt?

    // function to compute outputs of component, optionally given the inputs
    fun compute(){
        outputs = inputs
    }
}