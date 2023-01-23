import androidx.compose.runtime.*
import extensions.update
import java.io.File

class OpenedCircuitsManager {
    val openedCircuits = mutableStateListOf<OpenedCircuit>()
    val simulators = mutableStateListOf<Pair<Simulator, Int>>()

    var selectedCircuit by mutableStateOf<OpenedCircuit?>(null)
        private set

    val selectedSimulation: Pair<Simulator, Int>?
        get() = if (selectedCircuit == null) null else simulators[openedCircuits.indexOf(selectedCircuit!!)]

    fun modifyCircuit(oldCircuit: OpenedCircuit, modifier: (OpenedCircuit) -> OpenedCircuit) {
        val index = openedCircuits.indexOf(oldCircuit)
        if (index == -1) {
            return
        }
        modifyCircuit(index, modifier)
    }
    fun modifyCircuit(index: Int, modifier: (OpenedCircuit) -> OpenedCircuit) {
        val oldCircuit = openedCircuits.getOrNull(index) ?: return
        val replaceSelected = oldCircuit == selectedCircuit
        val newCircuit = modifier(oldCircuit)
        openedCircuits[index] = newCircuit
        selectedCircuit = if (replaceSelected) newCircuit else selectedCircuit
    }
    fun modifyCircuits(modifier: (OpenedCircuit) -> OpenedCircuit) {
        val indexOfSelected = if (selectedCircuit == null) -1 else openedCircuits.indexOf(selectedCircuit)
        openedCircuits.replaceAll(modifier)
        selectedCircuit = openedCircuits.getOrNull(indexOfSelected)
    }

    fun newCircuit() {
        val circuit = OpenedCircuit()
        openCircuit(circuit)
    }

    fun openCircuit(filename: String) = openCircuit(OpenedCircuit.open(filename))
    fun openCircuit(file: File) = openCircuit(OpenedCircuit.open(file))
    fun openCircuit(circuit: OpenedCircuit) {
        openedCircuits.add(circuit)
        selectedCircuit = circuit
        simulators.add(Simulator(circuit.circuit.graph) to 0)
    }

    fun selectCircuit(circuit: OpenedCircuit) {
        selectedCircuit = circuit
    }

    fun closeCircuit(circuit: OpenedCircuit) {
        val index = openedCircuits.indexOf(circuit)
        if (index == -1) {
            return
        }
        if (selectedCircuit == circuit) {
            var newIndex = index + 1
            if (!openedCircuits.indices.contains(newIndex)) {
                newIndex -= 1
            }
            openedCircuits.remove(circuit)
            newIndex -= 1
            selectedCircuit = openedCircuits.getOrNull(newIndex)
        }
        else {
            openedCircuits.remove(circuit)
        }
        simulators.removeAt(index)
    }
}

internal val _Manager = compositionLocalOf { OpenedCircuitsManager() }
