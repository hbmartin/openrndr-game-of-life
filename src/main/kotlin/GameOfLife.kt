import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.grid
import org.openrndr.launch
import org.openrndr.shape.Rectangle

private const val COLUMNS = 500
private const val ROWS = COLUMNS
private const val MARGIN = 1.0
private const val GUTTER = 0.3
private const val WINDOW_SIZE = 1200
private const val INFO_TEXT_POS = 20.0

fun main() =
    application {
        configure {
            width = WINDOW_SIZE
            height = WINDOW_SIZE
            title = "Game of Life"
        }

        program {
            val vm = GolViewModel(ROWS, COLUMNS)

            @Suppress("AvoidVarsExceptWithDelegate")
            var lastRender = 0.0

            launch { vm.loopUpdate() }

            extend {
                drawer.clear(vm.settings.color)
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                drawer.rectangles {
                    grid.each { rowIndex, columnIndex, rect ->
                        if (vm.isCellAlive(rowIndex, columnIndex)) {
                            rectangle(rect)
                        }
                    }
                }

                if (vm.settings.isInfoVisible) {
                    val fps = 1 / (seconds - lastRender)
                    drawer.text(
                        text =
                        "Generation: ${vm.generation}, " +
                            "Compute ms: ${vm.lastGenerationTime}, " +
                            "Expected ms: ${vm.delayTimeMillis}, " +
                            "FPS: %.0f".format(fps),
                        x = INFO_TEXT_POS,
                        y = INFO_TEXT_POS,
                    )
                }

                lastRender = seconds
            }

            extend(vm.gui)
            vm.gui.visible = false
            vm.listenToMouseEvents(this)
            vm.listenToKeyboardEvents(this)
        }
    }

internal val Program.grid
    get() =
        drawer.bounds.grid(
            columns = COLUMNS,
            rows = ROWS,
            marginX = MARGIN,
            marginY = MARGIN,
            gutterX = GUTTER,
            gutterY = GUTTER,
        )

internal fun List<List<Rectangle>>.each(block: (Int, Int, Rectangle) -> Unit) {
    forEachIndexed { rowIndex, row ->
        row.forEachIndexed { colIndex, rect ->
            block(rowIndex, colIndex, rect)
        }
    }
}
