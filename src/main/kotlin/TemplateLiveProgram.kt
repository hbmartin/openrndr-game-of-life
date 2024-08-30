import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.olive.oliveProgram

/**
 *  This is a template for a live program.
 *
 *  It uses oliveProgram {} instead of program {}. All code inside the
 *  oliveProgram {} can be changed while the program is running.
 */

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
