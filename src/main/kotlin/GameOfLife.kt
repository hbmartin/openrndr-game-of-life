import kotlinx.coroutines.delay
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.grid
import org.openrndr.launch
import org.openrndr.shape.Rectangle
import kotlin.math.max

private const val COLUMNS = 100
private const val ROWS = COLUMNS
private const val UPDATE_ON_CLICK = false
private const val DELAY_CHANGE_ON_SCROLL: Long = 50
private const val MARGIN = 1.0
private const val GUTTER = 0.5
private const val WINDOW_SIZE = 1000

@Suppress("AvoidVarsExceptWithDelegate")
private var delayTimeMillis: Long = 300

fun main() =
    application {
        val controller =
            GolController(
                rows = COLUMNS,
                columns = ROWS,
                initialPattern = "A.A\$3.A\$3.A\$A2.A\$.3A!",
            )

        configure {
            width = WINDOW_SIZE
            height = WINDOW_SIZE
//            windowResizable = true
            title = "Game of Life"
        }

        program {
            if (UPDATE_ON_CLICK) {
                mouse.buttonUp.listen {
                    controller.update()
                }
            } else {
                launch {
                    controller.loopUpdate()
                }

                mouse.buttonUp.listen { event ->
                    grid.each { rowIndex, colIndex, rect ->
                        if (rect.contains(event.position)) {
                            controller.toggleCell(rowIndex = rowIndex, colIndex = colIndex)
                            return@each
                        }
                    }
                }
                mouse.dragged.listen { event ->
                    val targetPosition = event.position + event.dragDisplacement
                    grid.each { rowIndex, colIndex, rect ->
                        if (rect.contains(event.position) || rect.contains(targetPosition)) {
                            controller.toggleCell(rowIndex = rowIndex, colIndex = colIndex)
                        }
                    }
                }
            }
            mouse.scrolled.listen {
                if (it.rotation.y < 0) {
                    delayTimeMillis += DELAY_CHANGE_ON_SCROLL
                } else {
                    delayTimeMillis =
                        max(
                            delayTimeMillis - DELAY_CHANGE_ON_SCROLL,
                            DELAY_CHANGE_ON_SCROLL,
                        )
                }
            }
            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                drawer.rectangles {
                    grid.each { rowIndex, columnIndex, rect ->
                        if (controller[rowIndex to columnIndex]) {
                            rectangle(rect)
                        }
                    }
                }
            }
        }
    }

private val Program.grid
    get() = drawer.bounds.grid(COLUMNS, ROWS, MARGIN, MARGIN, GUTTER, GUTTER)

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
        } else {
            @Suppress("MagicNumber")
            delay(500L)
        }
    }
}
