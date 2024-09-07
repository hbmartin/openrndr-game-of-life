import kotlinx.coroutines.delay
import org.openrndr.KEY_ESCAPE
import org.openrndr.KEY_SPACEBAR
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.dialogs.openFileDialog
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.OptionParameter
import java.io.File
import java.time.Instant.now
import kotlin.system.measureTimeMillis

private const val DEFAULT_DELAY: Long = 200
private const val DELAY_CHANGE_ON_SCROLL: Long = 50
private val DEFAULT_PATTERN = Patterns.PERIOD_52_GLIDER_GUN

class GolViewModel(
    rows: Int,
    columns: Int,
) {
    @Suppress("AvoidVarsExceptWithDelegate")
    var delayTimeMillis: Long = DEFAULT_DELAY
        private set

    @Suppress("AvoidVarsExceptWithDelegate")
    private var pattern = DEFAULT_PATTERN

    @Suppress("AvoidVarsExceptWithDelegate")
    var lastGenerationTime: Long = 0
        private set

    private val controller =
        GolController(
            rows = rows,
            columns = columns,
            initialPattern = pattern.value,
        )

    val generation: ULong
        get() = controller.generation

    val gui = GUI()

    val settings =
        @Suppress("VarCouldBeVal", "AvoidVarsExceptWithDelegate")
        @Description("Settings")
        object : Settings {
            @ColorParameter("Background", order = 0)
            override var color = ColorRGBa.PINK

            @BooleanParameter("Show Info", order = 1)
            override var isInfoVisible = false

            @OptionParameter("\n", order = 99)
            var pattern: Patterns = DEFAULT_PATTERN
        }

    init {
        createSettingsGui()
    }

    private fun createSettingsGui(): GUI {
        gui.compartmentsCollapsedByDefault = false
        gui.add(settings)
        gui.onChange { _, value ->
            (value as? Patterns)?.let {
                pattern = it
                controller.reset(pattern)
            }
        }

        return gui
    }

    fun loadFile() {
        openFileDialog(supportedExtensions = listOf("RLE" to listOf("rle"))) { file ->
            controller.reset(file.readText())
        }
    }

    @Suppress("LabeledExpression", "CognitiveComplexMethod")
    fun listenToMouseEvents(program: Program) {
        program.mouse.buttonUp.listen { event ->
            if (gui.visible) return@listen
            program.grid.each { rowIndex, colIndex, rect ->
                if (rect.contains(event.position)) {
                    controller.turnOnCell(rowIndex = rowIndex, colIndex = colIndex)
                    return@each
                }
            }
        }
        program.mouse.dragged.listen { event ->
            if (gui.visible) return@listen
            val targetPosition = event.position + event.dragDisplacement
            program.grid.each { rowIndex, colIndex, rect ->
                if (rect.contains(event.position) || rect.contains(targetPosition)) {
                    controller.turnOnCell(rowIndex = rowIndex, colIndex = colIndex)
                }
            }
        }
        program.mouse.scrolled.listen { event ->
            delayTimeMillis =
                if (event.rotation.y < 0) {
                    delayTimeMillis + DELAY_CHANGE_ON_SCROLL
                } else {
                    (delayTimeMillis - DELAY_CHANGE_ON_SCROLL).coerceAtLeast(DELAY_CHANGE_ON_SCROLL)
                }
        }
    }

    @Suppress("MultilineExpressionWrapping")
    fun listenToKeyboardEvents(program: Program) {
        program.keyboard.keyUp.listen {
            when (it.key) {
                KEY_SPACEBAR -> delayTimeMillis = if (delayTimeMillis > 0) 0 else DEFAULT_DELAY
                KEY_ESCAPE -> controller.reset(pattern)
                else -> when (it.name) {
                    "," -> gui.visible = !gui.visible
                    "." -> gui.visible = !gui.visible
                    "c" -> controller.reset(null)
                    "i" -> settings.isInfoVisible = !settings.isInfoVisible
                    "l" -> loadFile()
                    "q" -> program.application.exit()
                    "r" -> controller.reset(Patterns.entries.random())
                    "s" -> File("ogol-${now().toEpochMilli()}.rle").writeText(controller.toString())
                    else -> Unit
                }
            }
        }
    }

    suspend fun loopUpdate() {
        while (true) {
            if (delayTimeMillis > 0) {
                delay(delayTimeMillis)
                lastGenerationTime = measureTimeMillis {
                    controller.update()
                }
            } else {
                @Suppress("MagicNumber")
                delay(250L)
            }
        }
    }

    fun isCellAlive(
        row: Int,
        column: Int,
    ) = controller[row to column]
}
