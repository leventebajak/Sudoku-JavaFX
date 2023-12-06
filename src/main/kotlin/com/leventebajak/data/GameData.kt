package com.leventebajak.data

import com.leventebajak.sudokugenerator.Board.Companion.NUMBERS
import com.leventebajak.sudokugenerator.Sudoku
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Represents the state of a Sudoku game. This class is [Serializable].
 *
 * @constructor Creates a new Sudoku game state from the given cells.
 *
 * @property cells The cells of the Sudoku game.
 * @property secondsElapsed The number of seconds elapsed since the start of the game.
 */
@Serializable
class GameData(private val cells: Array<Array<SudokuCell>>) {
    var secondsElapsed = 0

    init {
        require(cells.size == 9) { "Sudoku must have 9 rows" }
        require(cells.all { row -> row.size == 9 }) { "Sudoku must have 9 columns" }
        require(cells.all { row -> row.all { cell -> cell.value in NUMBERS } }) { "Cell values must be in $NUMBERS" }
    }

    /**
     * Creates a new Sudoku game state from the given [Sudoku].
     */
    constructor(sudoku: Sudoku) : this(
        Array(9) { row ->
            Array(9) { col ->
                SudokuCell(sudoku.clues[row, col], sudoku.solutions[0][row, col])
            }
        }
    ) {
        require(sudoku.solutions.size == 1) { "Sudoku must have exactly one solution" }
    }

    /**
     * Returns the cell at the given [row] and [column].
     */
    operator fun get(row: Int, column: Int) = cells[row][column]

    /**
     * Sets the cell at the given [row] and [column] to the given [value]. The cell must be editable.
     *
     * @throws IllegalArgumentException if the given [value] is not in [NUMBERS].
     */
    operator fun set(row: Int, column: Int, value: Int) {
        require(value in NUMBERS) { "Value must be in $NUMBERS" }
        cells[row][column].value = value
    }

    /**
     * Returns whether all cells are solved.
     */
    fun solved() = cells.all { row -> row.all { cell -> cell.isSolved() } }

    companion object {
        /**
         * Loads a [GameData] from the given [fileName].
         *
         * @return the loaded [GameData] or `null` if the file could not be loaded.
         * @see [GameData.save]
         */
        fun load(fileName: String): GameData? {
            runCatching { return Json.decodeFromString(serializer(), File(fileName).readText()) }
            return null
        }
    }
}

/**
 * Saves this [GameData] to the given [fileName].
 * @see [GameData.load]
 */
fun GameData.save(fileName: String) = File(fileName).writeText(Json.encodeToString(GameData.serializer(), this))