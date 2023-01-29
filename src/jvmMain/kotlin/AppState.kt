import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppState {
    // TODO: Deselect when switching circuits in app
    var selectedDisplayable by mutableStateOf<Displayable?>(null)
    var mouseMode by mutableStateOf(MouseMode.Select)
    var continuousSimulation by mutableStateOf(true)
    var stopSimulationOnRestart by mutableStateOf(false)
    var hovered by mutableStateOf<Hoverable?>(null)
}

enum class MouseMode {
    Select, Interact
}
