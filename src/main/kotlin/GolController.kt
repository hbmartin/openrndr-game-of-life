import kotlin.random.Random

private const val RULE_SIZE = 9
private val NEIGHBOR_RANGE = -1..1

class GolController(
    private val rows: Int,
    private val columns: Int,
    private val birthRule: BooleanArray,
    private val surviveRule: BooleanArray,
    initialPattern: Array<BooleanArray>?,
) {
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

    @Suppress("AvoidVarsExceptWithDelegate")
    private var grid: Array<BooleanArray> =
        initialPattern?.let {
            centerPattern(it, rows, columns)
        } ?: randomGrid(rows, columns)

    init {
        require(columns > 0) { "Columns must be greater than 0" }
        require(rows > 0) { "Rows must be greater than 0" }
        require(birthRule.size == RULE_SIZE) { "Birth rule must be of size $RULE_SIZE" }
        require(surviveRule.size == RULE_SIZE) { "Survive rule must be of size $RULE_SIZE" }
    }

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
    ): Int =
        NEIGHBOR_RANGE
            .flatMap { y ->
                NEIGHBOR_RANGE.map { x ->
                    if (x == 0 && y == 0) {
                        0
                    } else {
                        val neighbourRowIndex = wrappedIndex(rowIndex + y, rows)
                        val neighbourColumnIndex = wrappedIndex(columnIndex + x, columns)

                        if (grid[neighbourRowIndex][neighbourColumnIndex]) {
                            1
                        } else {
                            0
                        }
                    }
                }
            }.sum()

    fun toggleCell(
        rowIndex: Int,
        colIndex: Int,
    ) {
        grid[rowIndex][colIndex] = !grid[rowIndex][colIndex]
    }

    operator fun get(coords: Pair<Int, Int>): Boolean = grid[coords.first][coords.second]

    override fun toString(): String =
        grid.joinToString("$") {
            it.joinToString("") { cell -> if (cell) "A" else "." }
        }
}

private fun String.toBirthRule(): BooleanArray {
    val rule = this.substringAfter("B").substringBefore("/")
    return BooleanArray(RULE_SIZE) { it.toString() in rule }
}

private fun String.toSurviveRule(): BooleanArray {
    val rule = this.substringAfter("S")
    return BooleanArray(RULE_SIZE) { it.toString() in rule }
}

private fun centerPattern(
    initialPattern: Array<BooleanArray>,
    rows: Int,
    columns: Int,
): Array<BooleanArray> {
    require(initialPattern.isNotEmpty()) { "Initial shape cannot be empty" }
    require(initialPattern.size <= rows) { "Initial shape is too tall (${initialPattern.size} > $rows)" }
    require(initialPattern[0].isNotEmpty()) { "Initial shape has 0 width columns" }

    val height = initialPattern.size
    val width = requireNotNull(initialPattern.maxOfOrNull { it.size })
    require(width <= columns) { "Initial shape is too wide ($width > $columns)" }
    val startRow = (rows - height) / 2
    val startColumn = (columns - width) / 2

    return Array(rows) { rowIndex ->
        BooleanArray(columns) { columnIndex ->
            if (rowIndex in startRow until startRow + height &&
                columnIndex in startColumn until startColumn + width
            ) {
                initialPattern[rowIndex - startRow]
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
@Suppress("AvoidMutableCollections")
private fun parsePattern(pattern: String): Array<BooleanArray> {
    require(pattern.isNotEmpty()) { "Pattern cannot be empty" }
    val illegalChar = pattern.find { it !in "Ao.b0123456789$!" }
    require(illegalChar == null) { "Illegal character in pattern: $illegalChar" }
    val rows = mutableListOf<BooleanArray>()
    val patternRows = pattern.split('$')
    for (pRow in patternRows) {
        if (pRow.isEmpty()) continue
        val parsed = mutableListOf<Boolean>()

        @Suppress("AvoidVarsExceptWithDelegate")
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
