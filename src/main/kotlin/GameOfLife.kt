import kotlinx.coroutines.delay
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.grid
import org.openrndr.launch
import org.openrndr.math.Vector2
import kotlin.math.max

private const val COLUMNS = 100
private const val ROWS = COLUMNS
private const val UPDATE_ON_CLICK = false
private const val DELAY_CHANGE_ON_SCROLL: Long = 50
private const val MARGIN = 1.0
private const val GUTTER = 0.5
private var delayTimeMillis: Long = 300

suspend fun GolController.loopUpdate() {
    while (true) {
        delay(delayTimeMillis)
        update()
    }
}

fun main() =
    application {
        val controller =
            GolController(
                rows = COLUMNS,
                columns = ROWS,
                initialPattern = "A.A\$3.A\$3.A\$A2.A\$.3A!",
            )

        configure {
            width = 1000
            height = 1000
            windowResizable = true
            title = "Game of Life"
        }

        val isDragging: MutableList<Vector2> = mutableListOf()

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
                    println("mouse.buttonUp: $event")
                    isDragging.add(event.position)
                    println(isDragging)
                    drawer.bounds
                        .grid(COLUMNS, ROWS, MARGIN, MARGIN, GUTTER, GUTTER)
                        .mapIndexed { rowIndex, row ->
                            row.mapIndexed { colIndex, rect ->
                                if (isDragging.any { rect.contains(it) }) {
                                    controller.toggleCell(colIndex, rowIndex)
                                }
                            }
                        }
                    isDragging.clear()
                }
                mouse.dragged.listen {
                    println("mouse.draggylicious: $it")
                    isDragging.apply {
                        add(it.position)
                        add(it.position + it.dragDisplacement)
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
                    drawer.bounds
                        .grid(COLUMNS, ROWS, MARGIN, MARGIN, GUTTER, GUTTER)
                        .forEachIndexed { rowIndex, row ->
                            row.forEachIndexed { columnIndex, rect ->
                                if (controller.getCell(columnIndex, rowIndex)) {
                                    rectangle(rect)
                                }
                            }
                        }
                }
            }
        }
    }
