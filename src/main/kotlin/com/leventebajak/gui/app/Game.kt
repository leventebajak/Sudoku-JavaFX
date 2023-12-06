package com.leventebajak.gui.app

import com.leventebajak.data.GameData
import com.leventebajak.data.save
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.layout.GridPane
import javafx.scene.text.Font.font
import javafx.scene.text.FontWeight
import javafx.stage.Screen
import javafx.stage.Stage
import java.io.File
import kotlin.time.Duration.Companion.nanoseconds

/**
 * The main game class.
 *
 * @param data The game data to use.
 */
class Game(private val data: GameData) : Application() {

    companion object {
        /**
         * The path to the autosave file. The file is used to store the game state when the game is closed.
         */
        const val AUTOSAVE_FILE_PATH = "autosave.data"

        private const val HEIGHT = 720.0
        private const val WIDTH = HEIGHT

        private const val CELL_SIZE = HEIGHT * 0.07
    }

    private lateinit var mainScene: Scene

    private lateinit var timer: AnimationTimer

    private val sudokuGrid = createSudokuGrid()

    override fun start(mainStage: Stage) {
        mainStage.title = "Sudoku"
        mainStage.isResizable = false
        mainStage.icons.add(Image(javaClass.getResource("/icon.png")!!.toString()))

        // Save the game when the window is closed
        mainStage.onCloseRequest = EventHandler { data.save(AUTOSAVE_FILE_PATH) }

        val root = Group()
        mainScene = Scene(root, WIDTH, HEIGHT)
        mainScene.stylesheets.add(javaClass.getResource("/styles.css")!!.toExternalForm())

        sudokuGrid.layoutX = (WIDTH - sudokuGrid.prefWidth) / 2
        sudokuGrid.layoutY = (HEIGHT - sudokuGrid.prefHeight) / 2
        root.children.add(sudokuGrid)

        // Back to menu button
        with(Button("Back to menu")) {
            prefWidth = HEIGHT * 0.15
            prefHeight = HEIGHT * 0.05
            layoutX = HEIGHT * 0.01
            layoutY = HEIGHT * 0.01
            font = font("Arial", FontWeight.BOLD, HEIGHT * 0.0175)
            onAction = EventHandler {
                data.save(AUTOSAVE_FILE_PATH)
                Menu().start(mainStage)
            }
            root.children.add(this)
        }

        // Timer label
        val timerLabel = Label(data.secondsElapsed.toTimerString()).apply {
            font = font("Arial", FontWeight.BOLD, HEIGHT * 0.05)
            prefWidth = WIDTH
            style = "-fx-alignment: center;"
            layoutY = HEIGHT * 0.1
            root.children.add(this)
        }

        // Timer
        var lastNano = System.nanoTime()
        timer = object : AnimationTimer() {
            override fun handle(currentNano: Long) {
                (currentNano - lastNano).nanoseconds.inWholeSeconds.toInt().let {
                    if (it != 0) {
                        data.secondsElapsed += it
                        timerLabel.text = data.secondsElapsed.toTimerString()
                        lastNano = currentNano
                    }
                }
            }
        }.also { it.start() }

        // Center the window
        with(Screen.getPrimary().visualBounds) {
            mainStage.x = (width - WIDTH) / 2
            mainStage.y = (height - HEIGHT) / 2
        }

        mainStage.scene = mainScene
        mainStage.show()
    }

    /**
     * Creates the Sudoku grid, which is a [GridPane] with 81 [TextField]s.
     *
     * The [TextField]s are styled and have listeners for input and focus.
     *
     * @return The created Sudoku grid.
     */
    private fun createSudokuGrid() = GridPane().apply {
        prefWidth = CELL_SIZE * 9
        prefHeight = CELL_SIZE * 9

        repeat(9) { row ->
            repeat(9) { col ->
                with(TextField()) {
                    text = data[row, col].value.toString().let { if (it == "0") "" else it }
                    isEditable = data[row, col].isEditable()
                    if (!isEditable)
                        styleClass.add("non-editable")
                    prefWidth = CELL_SIZE
                    prefHeight = CELL_SIZE

                    // Only allow digits and replace the old value with the new one
                    textProperty().addListener { _, oldValue, newValue ->
                        if (newValue.isEmpty())
                            return@addListener
                        text = try {
                            newValue.last().digitToInt().also {
                                // Update the game data
                                cellChanged(row, col, it)
                            }.toString().let { if (it == "0") "" else it }
                        } catch (_: IllegalArgumentException) {
                            oldValue
                        } catch (_: NoSuchElementException) {
                            ""
                        }
                    }

                    // Deselect the cell's text if it's focused, but it's not editable
                    focusedProperty().addListener { _, _, isNowFocused ->
                        if (isNowFocused) Platform.runLater { if (isFocused && !isEditable) deselect() }
                    }

                    // Move the focus to the next cell when WASD or an arrow key or is pressed
                    onKeyPressed = EventHandler { event ->
                        var newColIndex = col
                        var newRowIndex = row

                        when (event.code) {
                            KeyCode.LEFT, KeyCode.A -> if (col > 0) newColIndex--
                            KeyCode.RIGHT, KeyCode.D -> if (col < 8) newColIndex++
                            KeyCode.UP, KeyCode.W -> if (row > 0) newRowIndex--
                            KeyCode.DOWN, KeyCode.S -> if (row < 8) newRowIndex++
                            else -> return@EventHandler
                        }

                        children.firstOrNull {
                            GridPane.getColumnIndex(it) == newColIndex && GridPane.getRowIndex(it) == newRowIndex
                        }?.requestFocus()
                    }

                    font = font("Arial", FontWeight.BOLD, CELL_SIZE * 0.5)
                    styleClass.add("sudoku-cell")

                    // Set the borders based on the cell's position
                    val borderWidth = HEIGHT * 0.002
                    style = when {
                        row % 3 == 0 && col % 3 == 0 -> "-fx-border-width: $borderWidth 0 0 $borderWidth;"
                        row % 3 == 0 && col % 3 == 2 -> "-fx-border-width: $borderWidth $borderWidth 0 0;"
                        row % 3 == 2 && col % 3 == 0 -> "-fx-border-width: 0 0 $borderWidth $borderWidth;"
                        row % 3 == 2 && col % 3 == 2 -> "-fx-border-width: 0 $borderWidth $borderWidth 0;"
                        row % 3 == 0 -> "-fx-border-width: $borderWidth 0 0 0;"
                        row % 3 == 2 -> "-fx-border-width: 0 0 $borderWidth 0;"
                        col % 3 == 0 -> "-fx-border-width: 0 0 0 $borderWidth;"
                        col % 3 == 2 -> "-fx-border-width: 0 $borderWidth 0 0;"
                        else -> "-fx-border-width: 0 0 0 0;"
                    }

                    add(this, col, row)
                }
            }
        }
    }

    /**
     * Updates the game [data] when a cell's value is changed.
     * Also checks if the game is solved.
     *
     * @param row The row of the changed cell.
     * @param col The column of the changed cell.
     * @param newValue The new value of the changed cell.
     */
    private fun cellChanged(row: Int, col: Int, newValue: Int) {
        data[row, col].value = newValue
        if (data.solved()) {
            timer.stop()
            with(Alert(Alert.AlertType.INFORMATION)) {
                height = 75.0
                width = 200.0
                graphic = null
                title = "Sudoku Solved"
                headerText = null
                contentText = "Congratulations! You solved the Sudoku in ${data.secondsElapsed.toTimerString()}."
                showAndWait()
            }

            // Delete autosave file
            runCatching { File(AUTOSAVE_FILE_PATH).delete() }

            Menu().start(mainScene.window as Stage)
        }
    }
}

/**
 * Converts an integer to the format of "mm:ss".
 *
 * @return A string in the format of "mm:ss".
 */
private fun Int.toTimerString() = String.format("%02d:%02d", this / 60, this % 60)