import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.olive.oliveProgram

private const val WINDOW_SIZE = 800

fun main() =
    application {
        configure {
            width = WINDOW_SIZE
            height = WINDOW_SIZE
        }
        oliveProgram {
            extend {
                drawer.clear(ColorRGBa.PINK)
            }
        }
    }
