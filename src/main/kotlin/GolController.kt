import kotlin.random.Random

class GolController(
    private val rows: Int,
    private val columns: Int,
    private val birthRule: BooleanArray,
    private val surviveRule: BooleanArray,
    initialPattern: Array<BooleanArray>?,
) {
    private var grid: Array<BooleanArray> =
        initialPattern?.let {
            centerShape(it, rows, columns)
        } ?: randomGrid(rows, columns)

    constructor(
        rows: Int,
        columns: Int,
        initialPattern: String? = null,
        rule: String = "B3/S23",
    ) : this(
        rows = rows,
        columns = columns,
        birthRule = rule.toBirthRule(),
        surviveRule = rule.toSurviveRule(),
        initialPattern = initialPattern?.let { parsePattern(it) },
    )

    init {
        require(columns > 0) { "Columns must be greater than 0" }
        require(rows > 0) { "Rows must be greater than 0" }
        require(birthRule.size == 9) { "Birth rule must be of size 9" }
        require(surviveRule.size == 9) { "Survive rule must be of size 9" }
        println("birthRule: ${birthRule.joinToString()}")
        println("surviveRule: ${surviveRule.joinToString()}")
    }

    fun getCell(
        columnIndex: Int,
        rowIndex: Int,
    ): Boolean = grid[rowIndex][columnIndex]

    fun update() {
        // n.b. if trying to avoid new Array allocation, be sure to not to update current grid for calculations
        grid =
            Array(rows) { rowIndex ->
                BooleanArray(columns) { columnIndex ->
                    val cell = grid[rowIndex][columnIndex]
                    val liveNeighbours = countLiveNeighbours(rowIndex, columnIndex)

                    if (cell) {
                        surviveRule[liveNeighbours]
                    } else {
                        birthRule[liveNeighbours]
                    }
                }
            }
    }

    private fun countLiveNeighbours(
        rowIndex: Int,
        columnIndex: Int,
    ): Int {
        var count = 0
        for (y in -1..1) {
            for (x in -1..1) {
                if (x == 0 && y == 0) continue
                val neighbourRowIndex = wrappedIndex(rowIndex + y, rows)
                val neighbourColumnIndex = wrappedIndex(columnIndex + x, columns)

                if (grid[neighbourRowIndex][neighbourColumnIndex]) {
                    count++
                }
            }
        }
        return count
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (row in grid) {
            for (cell in row) {
                sb.append(if (cell) 'A' else '.')
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    fun toggleCell(
        colIndex: Int,
        rowIndex: Int,
    ) {
        grid[rowIndex][colIndex] = !grid[rowIndex][colIndex]
    }
}

private fun String.toBirthRule(): BooleanArray {
    val rule = this.substringAfter("B").substringBefore("/")
    return BooleanArray(9) { it.toString() in rule }
}

private fun String.toSurviveRule(): BooleanArray {
    val rule = this.substringAfter("S")
    return BooleanArray(9) { it.toString() in rule }
}

private fun centerShape(
    initialShape: Array<BooleanArray>,
    rows: Int,
    columns: Int,
): Array<BooleanArray> {
    require(initialShape.isNotEmpty()) { "Initial shape cannot be empty" }
    require(initialShape.size <= rows) { "Initial shape is too tall (${initialShape.size} > $rows)" }
    require(initialShape[0].isNotEmpty()) { "Initial shape has 0 width columns" }
    val height = initialShape.size
    val width = requireNotNull(initialShape.map { it.size }.maxOrNull())
    require(width <= columns) { "Initial shape is too wide ($width > $columns)" }
    val startRow = (rows - height) / 2
    val startColumn = (columns - width) / 2
    return Array(rows) { rowIndex ->
        BooleanArray(columns) { columnIndex ->
            if (rowIndex in startRow until startRow + height &&
                columnIndex in startColumn until startColumn + width
            ) {
                initialShape[rowIndex - startRow]
                    .getOrElse(columnIndex - startColumn) { false }
            } else {
                false
            }
        }
    }
}

private fun randomGrid(
    rows: Int,
    columns: Int,
): Array<BooleanArray> =
    Array(rows) {
        BooleanArray(columns) {
            Random.nextBoolean()
        }
    }

private fun wrappedIndex(
    i: Int,
    upperBound: Int,
): Int =
    when (i) {
        upperBound -> 0
        -1 -> upperBound - 1
        else -> i
    }

// Function to convert RLE syntax to a 2D array of booleans
// eg. A.A$3.A$3.A$A2.A$.3A!
private fun parsePattern(pattern: String): Array<BooleanArray> {
    require(pattern.isNotEmpty()) { "Pattern cannot be empty" }
    val illegalChar = pattern.find { it !in "Ao.b0123456789$!" }
    require(illegalChar == null) { "Illegal character in pattern: $illegalChar" }
    val rows = mutableListOf<BooleanArray>()
    val patternRows = pattern.split('$')
    for (pRow in patternRows) {
        if (pRow.isEmpty()) continue
        val parsed = mutableListOf<Boolean>()
        var multiplier = 1
        for (char in pRow) {
            when {
                char.isDigit() -> multiplier = char.toString().toInt()
                char == '.' || char == 'b' -> {
                    repeat(multiplier) {
                        parsed.add(false)
                    }
                    multiplier = 1
                }
                char == 'A' || char == 'o' -> {
                    repeat(multiplier) {
                        parsed.add(true)
                    }
                    multiplier = 1
                }
            }
        }
        rows.add(parsed.toBooleanArray())
    }
    return rows.toTypedArray()
}
