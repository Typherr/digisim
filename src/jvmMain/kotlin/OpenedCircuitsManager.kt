import androidx.compose.runtime.*
import extensions.update
import java.io.File

class OpenedCircuitsManager {
    val openedCircuits = mutableStateListOf<OpenedCircuit>()

    var selectedCircuit by mutableStateOf<OpenedCircuit?>(null)
        private set

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
    }

    fun selectCircuit(circuit: OpenedCircuit) {
        selectedCircuit = circuit
    }

    fun closeCircuit(circuit: OpenedCircuit) {
        if (selectedCircuit == circuit) {
            var index = openedCircuits.indexOf(circuit) + 1
            if (!openedCircuits.indices.contains(index)) {
                index -= 1
            }
            openedCircuits.remove(circuit)
            index -= 1
            selectedCircuit = openedCircuits.getOrNull(index)
        }
        else {
            openedCircuits.remove(circuit)
        }
    }
}

internal val _Manager = compositionLocalOf { OpenedCircuitsManager() }
