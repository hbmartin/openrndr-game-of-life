import kotlinx.coroutines.delay
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.grid
import org.openrndr.launch
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

private const val COLUMNS = 100
private const val ROWS = COLUMNS

suspend fun GolController.loopUpdate(delayTimeMillis: Long = 500) {
    while (true) {
        delay(delayTimeMillis)
        val timeTaken =
            measureTime {
                update()
            }
        println("update: $timeTaken")
    }
}

fun main() =
    application {
        val controller =
            GolController(
                rows = COLUMNS + 2,
                columns = ROWS + 2,
                initialPattern = "A.A\$3.A\$3.A\$A2.A\$.3A!",
            )

        configure {
            width = 1000
            height = 1000
        }

        program {
            launch {
                controller.loopUpdate()
            }
            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                val (grid, gridTime) =
                    measureTimedValue {
                        drawer.bounds
                            .grid(
                                COLUMNS,
                                ROWS,
                                1.0,
                                1.0,
                                1.0,
                                1.0,
                            )
                    }
//                println("gridTime: $gridTime")
                val rectTime =
                    measureTime {
                        val rectangles =
                            grid.mapIndexed { rowIndex, row ->
                                row.mapIndexedNotNull { columnIndex, rect ->
                                    if (controller.getCell(columnIndex + 1, rowIndex + 1)) {
                                        rect
                                    } else {
                                        null
                                    }
                                }
                            }
                        drawer.fill = ColorRGBa.WHITE
                        drawer.rectangles(rectangles.flatten())
                    }
//                println("rectTime: $rectTime")
            }
        }
    }
