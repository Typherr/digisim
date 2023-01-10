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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import java.awt.FileDialog
import java.io.File
import javax.swing.UIManager
import kotlin.math.roundToInt

val OpenedCircuits = compositionLocalOf { mutableStateListOf<OpenedCircuit>() }
val SelectedCircuit = compositionLocalOf<MutableState<OpenedCircuit?>> { mutableStateOf(null) }

@Composable
@Preview
fun App() {

    MaterialTheme {
//        Button(onClick = {
//            text = "Hello, Desktop!"
//        }) {
//            Text(text)
//        }

        //Entire screen
        val openedCircuits = OpenedCircuits.current
        val selectedCircuit = SelectedCircuit.current

        fun dropNewDisplayable(displayable: Displayable, x: Float, y: Float) {
            val sc = selectedCircuit.value!!
            val newSc = sc.editCircuit { cfc ->
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
            // TODO: Proper actual update
            selectedCircuit.value = newSc
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
                    val oc = OpenedCircuit()
                    openedCircuits.add(oc)
                    selectedCircuit.value = oc
                }) {
                    Text("New Circuit")
                }
                Text("or")
                Button(onClick = {
                    // TODO: Figure out how to open

                }) {
                    Text("Open Circuit")
                }
            }
        }
        else {
            Column {
                LazyRow {
                    items(openedCircuits) { oc ->
                        Button(enabled = oc != selectedCircuit.value, onClick = {
                            selectedCircuit.value = oc
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

                var cursorPosition by remember { mutableStateOf<Offset?>(null) }
                var hoveredDisplayable by remember { mutableStateOf<Displayable?>(null) }
                var pressedDisplayable by remember { mutableStateOf<Displayable?>(null) }
                Row(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(onPress =  {
                                pressedDisplayable = hoveredDisplayable
                                awaitRelease()
                                pressedDisplayable = null
                            })
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset : Offset ->
                                    cursorPosition = offset - Offset(300.0F, 0.0F)
                                },
                                onDrag = { _: PointerInputChange, offset: Offset ->
                                    cursorPosition = cursorPosition!! + offset
                                },
                                onDragEnd = {
                                    if (pressedDisplayable != null) {
                                        dropNewDisplayable(pressedDisplayable!!, cursorPosition!!.x, cursorPosition!!.y)
                                    }
                                    cursorPosition = null
                                    pressedDisplayable = null
                                },
                                onDragCancel = {
                                    if (pressedDisplayable != null) {
                                        dropNewDisplayable(pressedDisplayable!!, cursorPosition!!.x, cursorPosition!!.y)
                                    }
                                    cursorPosition = null
                                    pressedDisplayable = null
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
                                        hoveredDisplayable = creator()
                                    },
                                    onHoverEnd = {
                                        hoveredDisplayable = null
                                    }
                                )
                            }
                            //TODO: load components here
                        }
                        //TODO: state viewer

                    }

                    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                        if (pressedDisplayable != null && cursorPosition != null) {
                            // Draw component that is being added, but not yet part of the canvas
                            Text(
                                pressedDisplayable!!.name,
                                modifier = Modifier.offset {
                                    IntOffset(cursorPosition?.x?.toInt() ?: 0, cursorPosition?.y?.toInt() ?: 0)
                                }
                            )
                        }

                        // Draw displayables
                        for ((displayable, xy) in selectedCircuit.value!!.circuit.canvas) {
                            val (x, y) = xy
                            Text(
                                displayable.name,
                                modifier = Modifier.offset {
                                    IntOffset(x.toInt(), y.toInt())
                                }
                            )
                        }
                    }
                }
            }
        }
    }
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
        val selectedCircuit = remember { mutableStateOf<OpenedCircuit?>(null) }

        CompositionLocalProvider(SelectedCircuit provides selectedCircuit) {
            Window(onCloseRequest = ::exitApplication) {
                val openedCircuits = OpenedCircuits.current
                val selectedCircuit = SelectedCircuit.current

                var isOpenFileChooserOpen by remember { mutableStateOf(false) }
                var filesToSaveAs = remember { mutableStateListOf<OpenedCircuit>() }
                var fileBeingSaved by remember { mutableStateOf(0) }
                var fileToSaveBeforeClose by remember { mutableStateOf<OpenedCircuit?>(null) }

                fun closeFile(file: OpenedCircuit, forceClose: Boolean = false) {
                    if (!file.edited || forceClose) {
                        if (selectedCircuit.value == file) {
                            var index = openedCircuits.indexOf(file) + 1
                            if (!openedCircuits.indices.contains(index)) {
                                index -= 1
                            }
                            openedCircuits.remove(file)
                            index -= 1
                            selectedCircuit.value = openedCircuits.getOrNull(index)
                        }
                        else {
                            openedCircuits.remove(file)
                        }
                    }
                    else {
                        fileToSaveBeforeClose = file
                    }
                }

                if (isOpenFileChooserOpen) {
                    FileDialog(type = DialogType.OPEN) { files ->
                        isOpenFileChooserOpen = false
                        val selectedFile = files.firstOrNull()
                        if (selectedFile != null) {
                            try {
                                val openedCircuit = OpenedCircuit.open(selectedFile)
                                openedCircuits.add(openedCircuit)
                                selectedCircuit.value = openedCircuit
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
                            val newFile = oldFile.saveAs(savedFile)
                            val oldIndex = openedCircuits.indexOf(oldFile)
                            if (oldIndex != -1) {
                                openedCircuits[oldIndex] = newFile
                                if (selectedCircuit.value == oldFile) {
                                    selectedCircuit.value = newFile
                                }
                            }
                            filesToSaveAs.removeAt(fileBeingSaved)
                        }
                        else {
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
                                        }
                                        else {
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
                            val oc = OpenedCircuit()
                            openedCircuits.add(oc)
                            selectedCircuit.value = oc
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
                            enabled = SelectedCircuit.current.value?.edited == true,
                            shortcut = KeyShortcut(Key.S, ctrl = !isMacOs, meta = isMacOs)
                        ) {
                            val oldCircuit = selectedCircuit.value!!
                            if (oldCircuit.filename == null) {
                                // If file was never saved, do the same as `Save As`
                                filesToSaveAs.add(oldCircuit)
                                fileBeingSaved = 0
                            } else {
                                val newCircuit = oldCircuit.save()
                                val oldIndex = openedCircuits.indexOf(oldCircuit)
                                openedCircuits[oldIndex] = newCircuit
                                selectedCircuit.value = newCircuit
                            }
                        }
                        Item("Save As...",
                            mnemonic = 'A',
                            enabled = SelectedCircuit.current.value != null,
                            shortcut = KeyShortcut(Key.S, ctrl = !isMacOs, meta = isMacOs, shift = true)
                            ) {
                            filesToSaveAs.add(selectedCircuit.value!!)
                            fileBeingSaved = 0
                        }
                        Item(
                            "Save All",
                            mnemonic = 'l',
                            enabled = openedCircuits.isNotEmpty(),
                            shortcut = KeyShortcut(Key.S, ctrl = !isMacOs, meta = isMacOs, alt = true)
                        ) {
                            openedCircuits.replaceAll {
                                if (it.filename == null) {
                                    filesToSaveAs.add(it)
                                    it
                                }
                                else {
                                    val newCircuit = it.save()
                                    if (selectedCircuit.value == it) {
                                        selectedCircuit.value = newCircuit
                                    }
                                    newCircuit
                                }
                            }
                            if (filesToSaveAs.isNotEmpty()) {
                                fileBeingSaved = 0
                            }
                        }
                        Item(
                            "Close",
                            mnemonic = 'C',
                            enabled = selectedCircuit.value != null,
                            shortcut = KeyShortcut(Key.W, ctrl = !isMacOs, meta = isMacOs)
                        ) {
                            val oc = selectedCircuit.value!!
                            closeFile(oc)
                        }
                        if (!isMacOs) {
                            Separator()
                            Item("Quit", mnemonic = 'Q', shortcut = KeyShortcut(Key.F4, alt = true)) {
                                exitApplication()
                            }
                        }
                    }
                }
                App()
            }
        }
    }
}

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
