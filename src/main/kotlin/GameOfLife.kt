import kotlinx.coroutines.delay
import org.openrndr.KEY_ESCAPE
import org.openrndr.KEY_SPACEBAR
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.OptionParameter
import org.openrndr.extra.shapes.grid
import org.openrndr.launch
import org.openrndr.shape.Rectangle

private const val COLUMNS = 500
private const val ROWS = COLUMNS
private const val DELAY_CHANGE_ON_SCROLL: Long = 50
private const val DEFAULT_DELAY: Long = 200
private const val MARGIN = 1.0
private const val GUTTER = 0.3
private const val WINDOW_SIZE = 1200
private val DEFAULT_PATTERN = Patterns.PERIOD_52_GLIDER_GUN

@Suppress("AvoidVarsExceptWithDelegate")
private var delayTimeMillis: Long = DEFAULT_DELAY

@Suppress("AvoidVarsExceptWithDelegate")
private var pattern = DEFAULT_PATTERN
private val CONTROLLER =
    GolController(
        rows = COLUMNS,
        columns = ROWS,
        initialPattern = pattern.value,
    )

fun main() =
    application {
        configure {
            width = WINDOW_SIZE
            height = WINDOW_SIZE
//            windowResizable = true
            title = "Game of Life"
        }

        program {
            launch { CONTROLLER.loopUpdate() }
            val gui = GUI()
            listenToMouseEvents(gui)

            gui.compartmentsCollapsedByDefault = false

            val settings =
                @Suppress("VarCouldBeVal", "AvoidVarsExceptWithDelegate")
                @Description("Settings")
                object {
                    @ColorParameter("Background", order = 0)
                    var color = ColorRGBa.PINK

                    @OptionParameter("\n", order = 99)
                    var pattern: Patterns = DEFAULT_PATTERN

                    @ActionParameter("Apply", order = 100)
                    fun doApply() {
                        CONTROLLER.reset(pattern)
                    }
                }
            gui.add(settings)
            gui.onChange { _, value ->
                (value as? Patterns)?.let {
                    pattern = it
                }
            }
            extend(gui)

            // note we can only change the visibility after the extend
            gui.visible = false

            keyboard.keyUp.listen {
                when (it.key) {
                    KEY_SPACEBAR -> delayTimeMillis = if (delayTimeMillis > 0) 0 else DEFAULT_DELAY
                    KEY_ESCAPE -> CONTROLLER.reset(pattern)
                    else -> when (it.name) {
                        "." -> gui.visible = !gui.visible
                        "," -> gui.visible = !gui.visible
                        "r" -> CONTROLLER.reset(Patterns.entries.random())
                    }
                }
            }

            extend {
                drawer.clear(settings.color)
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                drawer.rectangles {
                    grid.each { rowIndex, columnIndex, rect ->
                        if (CONTROLLER[rowIndex to columnIndex]) {
                            rectangle(rect)
                        }
                    }
                }
            }
        }
    }

@Suppress("LabeledExpression", "CognitiveComplexMethod")
private fun Program.listenToMouseEvents(gui: GUI) {
    mouse.buttonUp.listen { event ->
        if (gui.visible) return@listen
        grid.each { rowIndex, colIndex, rect ->
            if (rect.contains(event.position)) {
                CONTROLLER.turnOnCell(rowIndex = rowIndex, colIndex = colIndex)
                return@each
            }
        }
    }
    mouse.dragged.listen { event ->
        if (gui.visible) return@listen
        val targetPosition = event.position + event.dragDisplacement
        grid.each { rowIndex, colIndex, rect ->
            if (rect.contains(event.position) || rect.contains(targetPosition)) {
                CONTROLLER.turnOnCell(rowIndex = rowIndex, colIndex = colIndex)
            }
        }
    }
    mouse.scrolled.listen { event ->
        delayTimeMillis =
            if (event.rotation.y < 0) {
                delayTimeMillis + DELAY_CHANGE_ON_SCROLL
            } else {
                (delayTimeMillis - DELAY_CHANGE_ON_SCROLL).coerceAtLeast(DELAY_CHANGE_ON_SCROLL)
            }
    }
}

private val Program.grid
    get() =
        drawer.bounds.grid(
            columns = COLUMNS,
            rows = ROWS,
            marginX = MARGIN,
            marginY = MARGIN,
            gutterX = GUTTER,
            gutterY = GUTTER,
        )

private fun List<List<Rectangle>>.each(block: (Int, Int, Rectangle) -> Unit) {
    forEachIndexed { rowIndex, row ->
        row.forEachIndexed { colIndex, rect ->
            block(rowIndex, colIndex, rect)
        }
    }
}

private suspend fun GolController.loopUpdate() {
    while (true) {
        if (delayTimeMillis > 0) {
            delay(delayTimeMillis)
            update()

            // TODO: randomize grid if stasis  reached
        } else {
            @Suppress("MagicNumber")
            delay(250L)
        }
    }
}
