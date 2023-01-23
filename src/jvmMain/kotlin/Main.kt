import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import androidx.compose.ui.window.WindowPosition.PlatformDefault.y
import java.awt.FileDialog
import java.io.File
import java.util.*
import javax.swing.UIManager
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import extensions.toOffset

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App(manager: OpenedCircuitsManager) {
    MaterialTheme {
        //Entire screen
        val openedCircuits = manager.openedCircuits

        fun dropNewDisplayable(displayable: Displayable, x: Float, y: Float) {
            manager.modifyCircuit(manager.selectedCircuit!!) { circuit ->
                circuit.editCircuit { cfc ->
                    cfc
                        .let { cfc ->
                            if (displayable is Component) {
                                cfc.editGraph {
                                    it.createVertex(displayable index 0)
                                    it
                                }
                            }
                            else {
                                cfc.editDisplayables {
                                    it + displayable
                                }
                            }
                        }
                        .editCanvas {
                            it + (displayable to (x to y))
                        }
                }
            }
        }

        fun dropNewWire(from: CircuitPair, to: CircuitPair) {
            manager.modifyCircuit(manager.selectedCircuit!!) { circuit ->
                circuit.editCircuit { cfc ->
                    cfc.editGraph {
                        it.addEdge(from, to)
                        it
                    }
                }
            }
        }

        // Tabs
        // ----------+--------
        // Left menu | Canvas

        if (openedCircuits.isEmpty()) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                Button(onClick = {
                   manager.newCircuit()
                }) {
                    Text("New Circuit")
                }
                Text("or")
                var ofd = openFileDialog.current
                Button(onClick = {
                    ofd.value = true
                }) {
                    Text("Open Circuit")
                }
            }
        }
        else {
            Column {
                LazyRow {
                    items(openedCircuits) { oc ->
                        Button(enabled = oc != manager.selectedCircuit, onClick = {
                            manager.selectCircuit(oc)
                        }) {
                            Text(oc.filename ?: "New Circuit")
                            // TODO: Figure out a way to save unsaved files from here
                            //       until then, don't show X, and just close via File > Close (Ctrl/Cmd + W)
//                            IconButton(modifier = Modifier.size(12.dp), onClick = {
//                                if (!oc.edited) {
//                                    if (selectedCircuit.value == oc) {
//                                        var index = openedCircuits.indexOf(oc) + 1
//                                        if (!openedCircuits.indices.contains(index)) {
//                                            index -= 1
//                                        }
//                                        openedCircuits.remove(oc)
//                                        index -= 1
//                                        selectedCircuit.value = openedCircuits.getOrNull(index)
//                                    }
//                                    else {
//                                        openedCircuits.remove(oc)
//                                    }
//                                }
//                                else {
//                                    // TODO: First prompt save
//                                }
//                            }) {
//                                Icon(Icons.Filled.Close, "Close Circuit")
//                            }
                        }
                    }
                }

                val displayablePositions = remember { mutableStateMapOf<Displayable, LayoutCoordinates>() }

                var panningOffset by remember { mutableStateOf(Offset(0.0f, 0.0f)) }
                var cursorPosition by remember { mutableStateOf<Offset?>(null) }
                var hovered by remember { mutableStateOf<Hoverable?>(null) }
                var pressed by remember { mutableStateOf<Hoverable?>(null) }
                var cachedSnapSink by remember { mutableStateOf<Pair<Pair<Component, Int>, Pair<Offset, Float>>?>(null) }
                val snapSink = manager.selectedCircuit!!.circuit.canvas
                    .asSequence()
                    .filter { it.key is Component }
                    .map { it.key as Component to it.value }
                    .filter { it.first.inputNames.isNotEmpty() && displayablePositions.containsKey(it.first) }
                    .flatMap { it.first.inputPositions.mapIndexed { index, pos -> ((it.first to index) to it.second) } }
                    .map { it.first to Offset(
                        (it.second.first + it.first.first.inputPositions[it.first.second].first * displayablePositions[it.first.first]!!.size.width).toFloat(),
                        (it.second.second + it.first.first.inputPositions[it.first.second].second * displayablePositions[it.first.first]!!.size.height).toFloat()
                    ) }
                    .map {
                        val cursorInCanvas = cursorPosition?.minus(panningOffset) ?: Offset.Zero
                        it.first to (it.second to (
                                sqrt(((cursorInCanvas.x) - it.second.x).pow(2) + (cursorInCanvas.y - it.second.y).pow(2))
                        ))
                    }
                    .filter {
                        it.second.second < 20f
                    }
                    .sortedBy {
                        it.second.second
                    }
//                    .map { it.first to it.second.first }
                    .firstOrNull()
                if (snapSink != null) {
                    cachedSnapSink = snapSink
                }
                Row(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(onPress =  { offset ->
                                pressed = hovered?.withPressOffset(offset.copy(x = offset.x - 300.dp.toPx()))
                                awaitRelease()
                                if (pressed is HoveredDisplayable && (pressed as HoveredDisplayable).displayable.isClickable) {
                                    (pressed as HoveredDisplayable).displayable.onClick()
                                }
                                pressed = null
                            })
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset : Offset ->
                                    cursorPosition = offset - Offset(300.0F, 0.0F)
                                },
                                onDrag = { _: PointerInputChange, offset: Offset ->
                                    cursorPosition = cursorPosition!! + offset
                                    if (pressed == null) {
                                        panningOffset += offset
                                    }
                                },
                                onDragEnd = {
                                    if (pressed != null) {
                                        val position = cursorPosition!! - panningOffset
                                        val (x, y) = position
                                        if (pressed is HoveredNewDisplayable) {
                                            dropNewDisplayable((pressed as HoveredNewDisplayable).displayable, x, y)
                                        }
                                        else if (pressed is HoveredSource && cachedSnapSink != null && cachedSnapSink!!.second.second < 20f) {
                                            dropNewWire(
                                                ((pressed as HoveredSource).displayable as Component) index (pressed as HoveredSource).sourceIndex,
                                                cachedSnapSink!!.first.first index cachedSnapSink!!.first.second
                                            )
                                        }
                                    }
                                    cursorPosition = null
                                    pressed = null
                                },
                                onDragCancel = {
                                    if (pressed != null) {
                                        val position = cursorPosition!! - panningOffset
                                        val (x, y) = position
                                        if (pressed is HoveredNewDisplayable) {
                                            dropNewDisplayable((pressed as HoveredNewDisplayable).displayable, x, y)
                                        }
                                        else if (pressed is HoveredSource && cachedSnapSink != null && cachedSnapSink!!.second.second < 20f) {
                                            dropNewWire(
                                                ((pressed as HoveredSource).displayable as Component) index (pressed as HoveredSource).sourceIndex,
                                                cachedSnapSink!!.first.first index cachedSnapSink!!.first.second
                                            )
                                        }
                                    }
                                    cursorPosition = null
                                    pressed = null
                                }
                            )
                        }
                ) {

                    //Left menu
                    Column(
                        modifier = Modifier
                            .width(300.dp)
                            .background(Color.Black.copy(alpha = 0.05F))
                            .fillMaxSize()
                    ){
                        //Toolbox
                        LazyColumn {
                            items(DisplayableSerializer.constructors.entries.map { it.key to { it.value(mapOf()) } }) { (name, creator) ->
                                ToolboxComponent(
                                    name,
                                    creator,
                                    onHoverStart = {
                                        hovered = HoveredNewDisplayable(creator())
                                    },
                                    onHoverEnd = {
                                        hovered = null
                                    }
                                )
                            }
                            //TODO: load components here
                        }
                        //TODO: state viewer

                    }

                    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                        val simulation = manager.simulators[manager.openedCircuits.indexOf(manager.selectedCircuit!!)]

                        LaunchedEffect(simulation.second) {
                            simulation.first.simulationStep()
                            manager.simulators[manager.openedCircuits.indexOf(manager.selectedCircuit!!)] = simulation.first to simulation.second + 1
                        }

                        if (pressed is HoveredNewDisplayable && cursorPosition != null) {
                            // Draw component that is being added, but not yet part of the canvas
//                            val offsettedPosition = (cursorPosition ?: Offset(0.0f, 0.0f)) + panningOffset
                            val offsettedPosition = (cursorPosition ?: Offset(0.0f, 0.0f))
                            Text(
                                (pressed as HoveredNewDisplayable).displayable.name,
                                modifier = Modifier.offset {
                                    IntOffset(offsettedPosition.x.toInt(), offsettedPosition.y.toInt())
                                }
                            )
                        }

                        if (pressed is HoveredSource && (pressed as HoveredSource).pressOffset != null && cursorPosition != null) {
                            val from = (pressed as HoveredSource).pressOffset!!
                            val to = snapSink?.second?.first?.plus(panningOffset) ?: cursorPosition!!
                            Wire(
                                from,
                                to,
                                color = if (snapSink != null) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.4f),
                                modifier = Modifier.offset {
                                    IntOffset(minOf(from.x, to.x).toInt(), minOf(from.y, to.y).toInt())
                                }
                            )
                        }

                        // Draw displayables
                        for ((displayable, xy) in manager.selectedCircuit!!.circuit.canvas) {
                            val (x, y) = Offset(xy.first, xy.second) + panningOffset
                            when (displayable) {
                                is Gate, is InputPin, is OutputPin -> {
                                    // HERE
                                    MeasureUnconstrainedViewSize(
                                        viewToMeasure = {
                                            Image(
                                                painterResource("/${displayable.name}.svg"),
                                                displayable.name
                                            )
                                        },
                                    ) { w, h ->
                                        Box {
                                            Image(
                                                painterResource("/${displayable.name}.svg"),
                                                displayable.name,
                                                modifier = Modifier.offset {
                                                    IntOffset(x.toInt(), y.toInt())
                                                }.let {
                                                    if (displayable.isClickable) {
                                                        it.onClick {
                                                            displayable.onClick()
                                                            val simulation = manager.selectedSimulation?.first
                                                            if (simulation != null && displayable is Component) {
                                                                simulation.simulateComponent(displayable)
                                                            }
                                                        }
                                                    }
                                                    else {
                                                        it
                                                    }
                                                }.onGloballyPositioned {
                                                    displayablePositions[displayable] = it
                                                }
                                            )
                                            for ((index, i) in (displayable as Component).inputNames.zip((displayable as Component).inputPositions).withIndex()) {
                                                val (inputName, inputPosition) = i
                                                Box(
                                                    modifier = Modifier
                                                        .offset {
                                                            IntOffset(
                                                                (x + inputPosition.first * w.toPx() - 5.dp.toPx()).toInt(),
                                                                (y + inputPosition.second * h.toPx() - 5.dp.toPx()).toInt(),
                                                            )
                                                        }
                                                        .width(10.dp)
                                                        .height(10.dp)
                                                        .let {
                                                            if (pressed is HoveredSource) {
                                                                it.background(Color.Cyan)
                                                            }
                                                            else {
                                                                it
                                                            }
                                                        }
                                                        .onPointerEvent(PointerEventType.Enter) {
                                                            if (pressed is HoveredSource) {
                                                                hovered = HoveredSink(displayable, index)
                                                            }
                                                        }
                                                        .onPointerEvent(PointerEventType.Exit) {
                                                            if (hovered is HoveredSink) {
                                                                hovered = null
                                                            }
                                                        }

                                                )
                                            }
                                            for ((index, o) in displayable.outputNames.zip(displayable.outputPositions).withIndex()) {
                                                val (outputName, outputPosition) = o
                                                var isHovered by remember { mutableStateOf(false) }
                                                Box(
                                                    modifier = Modifier
                                                        .offset {
                                                            IntOffset(
                                                                (x + outputPosition.first * w.toPx() - 5.dp.toPx()).toInt(),
                                                                (y + outputPosition.second * h.toPx() - 5.dp.toPx()).toInt(),
                                                            )
                                                        }
                                                        .width(10.dp)
                                                        .height(10.dp)
                                                        .let {
                                                            if (isHovered) {
                                                                it.background(Color.Magenta)
                                                            }
                                                            else {
                                                                it
                                                            }
                                                        }
                                                        .onPointerEvent(PointerEventType.Enter) {
                                                            if (pressed == null) {
                                                                isHovered = true
                                                                hovered = HoveredSource(displayable, index)
                                                            }
                                                        }
                                                        .onPointerEvent(PointerEventType.Exit) {
                                                            isHovered = false
                                                            if (hovered is HoveredSource) {
                                                                hovered = null
                                                            }
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                is Label -> {
                                    Text(
                                        displayable.properties["text"] ?: "Label",
                                        modifier = Modifier.offset {
                                            IntOffset(x.toInt(), y.toInt())
                                        }
                                    )
                                }

                                else -> {
                                    Text(
                                        displayable.name,
                                        modifier = Modifier.offset {
                                            IntOffset(x.toInt(), y.toInt())
                                        }.also {
                                            if (displayable.isClickable) {
                                                it.onClick {
                                                    displayable.onClick()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }


                        // Draw wires
                        for ((source, sinks) in manager.selectedCircuit!!.circuit.graph.adjacencyMap) {
                            val (sourceComp, sourceIdx) = source.data
                            if (!displayablePositions.containsKey(sourceComp)) continue
                            for ((sinkComp, sinkIdx) in sinks.map { it.data }) {
                                if (!displayablePositions.containsKey(sinkComp)) continue
                                val from = manager.selectedCircuit!!.circuit.canvas[sourceComp]!!.toOffset() + Offset(
                                    (sourceComp.outputPositions[sourceIdx].first * displayablePositions[sourceComp]!!.size.width).toFloat(),
                                    (sourceComp.outputPositions[sourceIdx].second * displayablePositions[sourceComp]!!.size.height).toFloat(),
                                )
                                val to = manager.selectedCircuit!!.circuit.canvas[sinkComp]!!.toOffset() + Offset(
                                    (sinkComp.inputPositions[sinkIdx].first * displayablePositions[sinkComp]!!.size.width).toFloat(),
                                    (sinkComp.inputPositions[sinkIdx].second * displayablePositions[sinkComp]!!.size.height).toFloat(),
                                )

                                // Check if can color based on simulation
                                var color = Color.Black
                                val simulation = manager.selectedSimulation?.first
                                if (simulation != null) {
                                    if (simulation.knownValues.containsKey(sourceComp)) {
                                        val value = simulation.knownValues[sourceComp]!![sourceIdx]
                                        if (value == 0) {
                                            color = Color.Red
                                        }
                                        else if (value == 1) {
                                            color = Color.Green
                                        }
                                        else if (value != null && value > 1) {
                                            color = Color.Blue
                                        }
                                    }
                                }

                                Wire(
                                    from,
                                    to,
                                    color = color,
                                    modifier = Modifier.offset {
                                        IntOffset(
                                            minOf(from.x, to.x).toInt() + panningOffset.x.toInt(),
                                            minOf(from.y, to.y).toInt() + panningOffset.y.toInt()
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

abstract sealed class Hoverable {
    abstract fun withPressOffset(offset: Offset): Hoverable
}
data class HoveredNewDisplayable(val displayable: Displayable): Hoverable() {
    override fun withPressOffset(offset: Offset): Hoverable = this
}
data class HoveredDisplayable(val displayable: Displayable): Hoverable() {
    override fun withPressOffset(offset: Offset): Hoverable = this
}
data class HoveredSource(val displayable: Displayable, val sourceIndex: Int, val pressOffset: Offset? = null): Hoverable() {
    override fun withPressOffset(offset: Offset): Hoverable = copy(pressOffset = offset)
}
data class HoveredSink(val displayable: Displayable, val sinkIndex: Int): Hoverable() {
    override fun withPressOffset(offset: Offset): Hoverable = this
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ToolboxComponent(name: String, creator: () -> Displayable, onHoverStart: (() -> Unit)? = null, onHoverEnd: (() -> Unit)? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .hoverable(interactionSource)
            .onPointerEvent(PointerEventType.Enter) {
                onHoverStart?.invoke()
            }
            .onPointerEvent(PointerEventType.Exit) {
                onHoverEnd?.invoke()
            }
            .let {
                if (isHovered) {
                    it.background(Color.Black.copy(alpha = 0.05F))
                }
                else {
                    it
                }
            }
            .padding(8.dp)
            .fillMaxWidth()
    ) {
//        Image(painter = painterResource(resourcePath = ""), contentDescription = name,
//            modifier = Modifier.clickable { println("Button Clicked!") })
        Text(name)
    }
}

@Composable
fun MultipleDraggableObject(letter: String, bgColor: Color) {

    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }


    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = offsetX.value.roundToInt(),
                    y = offsetY.value.roundToInt()
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX.value += dragAmount.x
                    offsetY.value += dragAmount.y
                }
            }
            .size(80.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            fontSize = 30.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
fun main() {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (_: Exception) {
        // Ignore theme change exception
    }
    application {
        val manager = remember { OpenedCircuitsManager() }
        val openedCircuits = manager.openedCircuits
        val selectedCircuit = manager.selectedCircuit

        Window(
            onCloseRequest = ::exitApplication,
            title = if (selectedCircuit != null) "${selectedCircuit.filename ?: "New Circuit"}${if (selectedCircuit.edited) "*" else ""} - DigiSim" else "DigiSim"
        ) {
//            var isOpenFileChooserOpen by remember { mutableStateOf(false) }
            var isOpenFileChooserOpen by openFileDialog.current
            var filesToSaveAs = remember { mutableStateListOf<OpenedCircuit>() }
            var fileBeingSaved by remember { mutableStateOf(0) }
            var fileToSaveBeforeClose by remember { mutableStateOf<OpenedCircuit?>(null) }

            fun closeFile(file: OpenedCircuit, forceClose: Boolean = false) {
                if (!file.edited || forceClose) {
                    manager.closeCircuit(file)
                } else {
                    fileToSaveBeforeClose = file
                }
            }

            if (isOpenFileChooserOpen) {
                FileDialog(type = DialogType.OPEN) { files ->
                    isOpenFileChooserOpen = false
                    val selectedFile = files.firstOrNull()
                    if (selectedFile != null) {
                        try {
                            manager.openCircuit(selectedFile)
                        } catch (e: Exception) {
                            println("Error in opening file: $e")
                            // TODO: Maybe show error about opening failed?
                        }
                    }
                }
            }

            if (filesToSaveAs.isNotEmpty() && filesToSaveAs.indices.contains(fileBeingSaved)) {
                FileDialog(type = DialogType.SAVE) {
                    if (it.isNotEmpty()) {
                        val savedFile = it.first()
                        val oldFile = filesToSaveAs[fileBeingSaved]
                        manager.modifyCircuit(oldFile) { file ->
                            file.saveAs(savedFile)
                        }
                        filesToSaveAs.removeAt(fileBeingSaved)
                    } else {
                        if (fileBeingSaved != filesToSaveAs.size) {
                            fileBeingSaved++
                        } else {
                            fileBeingSaved = -1
                        }
                    }
                }
            }

            if (fileToSaveBeforeClose != null) {
                AlertDialog(
                    title = {
                        Text("Are you sure you want to close the circuit?")
                    },
                    text = {
                        Text("The circuit has not been saved. Do you want to save the circuit before saving?")
                    },
                    onDismissRequest = {

                    },
                    buttons = {
                        // Stolen from AlertDialog(confirmButton = ..., dismissButton = ...)
                        Box(Modifier.fillMaxWidth().padding(all = 8.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Button(modifier = Modifier.padding(8.dp), onClick = {
                                    fileToSaveBeforeClose = null
                                }) {
                                    Text("Cancel")
                                }

                                Button(modifier = Modifier.padding(8.dp), onClick = {
                                    closeFile(fileToSaveBeforeClose!!, true)
                                    fileToSaveBeforeClose = null
                                }) {
                                    Text("No")
                                }
                                Button(modifier = Modifier.padding(8.dp), onClick = {
                                    if (fileToSaveBeforeClose!!.filename == null) {
                                        filesToSaveAs.add(fileToSaveBeforeClose!!)
                                    } else {
                                        fileToSaveBeforeClose!!.save()
                                    }
                                    closeFile(fileToSaveBeforeClose!!, true)
                                    fileToSaveBeforeClose = null
                                }) {
                                    Text("Yes")
                                }
                            }
                        }
                    },
                )
            }

            MenuBar {
                val isMacOs = System.getProperty("os.name", "").contains("mac os x", ignoreCase = true)
                Menu("File", 'F') {
                    Item("New", mnemonic = 'N', shortcut = KeyShortcut(Key.N, ctrl = !isMacOs, meta = isMacOs)) {
                        manager.newCircuit()
                    }
                    Item(
                        "Open...",
                        mnemonic = 'O',
                        shortcut = KeyShortcut(Key.O, ctrl = !isMacOs, meta = isMacOs)
                    ) {
                        isOpenFileChooserOpen = true
                    }
                    Item(
                        "Save",
                        mnemonic = 'S',
                        enabled = manager.selectedCircuit?.edited == true,
                        shortcut = KeyShortcut(Key.S, ctrl = !isMacOs, meta = isMacOs)
                    ) {
                        val oldCircuit = selectedCircuit!!
                        if (oldCircuit.filename == null) {
                            // If file was never saved, do the same as `Save As`
                            filesToSaveAs.add(oldCircuit)
                            fileBeingSaved = 0
                        } else {
                            manager.modifyCircuit(selectedCircuit!!) { circuit ->
                                circuit.save()
                            }
                        }
                    }
                    Item(
                        "Save As...",
                        mnemonic = 'A',
                        enabled = selectedCircuit != null,
                        shortcut = KeyShortcut(Key.S, ctrl = !isMacOs, meta = isMacOs, shift = true)
                    ) {
                        filesToSaveAs.add(selectedCircuit!!)
                        fileBeingSaved = 0
                    }
                    Item(
                        "Save All",
                        mnemonic = 'l',
                        enabled = openedCircuits.isNotEmpty(),
                        shortcut = KeyShortcut(Key.S, ctrl = !isMacOs, meta = isMacOs, alt = true)
                    ) {
                        manager.modifyCircuits {
                            if (it.filename == null) {
                                filesToSaveAs.add(it)
                                it
                            } else {
                                it.save()
                            }
                        }
                        if (filesToSaveAs.isNotEmpty()) {
                            fileBeingSaved = 0
                        }
                    }
                    Item(
                        "Close",
                        mnemonic = 'C',
                        enabled = selectedCircuit != null,
                        shortcut = KeyShortcut(Key.W, ctrl = !isMacOs, meta = isMacOs)
                    ) {
                        val oc = selectedCircuit!!
                        closeFile(oc)
                    }
                    if (!isMacOs) {
                        Separator()
                        Item("Quit", mnemonic = 'Q', shortcut = KeyShortcut(Key.F4, alt = true)) {
                            exitApplication()
                        }
                    }
                }
                Menu("Simulator", 'S') {
                    Item("Next Step", enabled = manager.selectedCircuit != null) {
                        val simulation = manager.selectedSimulation!!
                        simulation.first.simulationStep()
                        manager.simulators[manager.openedCircuits.indexOf(manager.selectedCircuit!!)] = simulation.first to simulation.second + 1
                    }
                }
            }
            App(manager)
        }
    }
}

val openFileDialog = compositionLocalOf { mutableStateOf(false) }

@Composable
private fun FrameWindowScope.FileDialog(
    title: String? = null,
    type: DialogType,
    multipleSelection: Boolean = false,
    onCloseRequest: (result: Array<File>) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(this.window, title ?: "Choose a file", if (type == DialogType.OPEN) LOAD else SAVE) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    onCloseRequest(files)
                }
            }
        }.apply {
            isMultipleMode = multipleSelection
        }
    },
    dispose = FileDialog::dispose
)

enum class DialogType {
    OPEN, SAVE
}
