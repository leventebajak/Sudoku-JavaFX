package com.leventebajak.gui.app

import com.leventebajak.data.GameData
import com.leventebajak.sudokugenerator.SudokuGenerator.Difficulty
import com.leventebajak.sudokugenerator.SudokuGenerator.generate
import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.MenuButton
import javafx.scene.control.MenuItem
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.text.Font.font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.stage.Screen
import javafx.stage.Stage

/**
 * The main menu class.
 */
class Menu : Application() {

    companion object {
        private const val HEIGHT = 720.0
        private const val WIDTH = HEIGHT
    }

    private lateinit var mainScene: Scene

    override fun start(mainStage: Stage) {
        mainStage.title = "Sudoku - Menu"
        mainStage.isResizable = false

        val root = Group()
        mainScene = Scene(root, WIDTH, HEIGHT)
        mainScene.stylesheets.add(javaClass.getResource("/styles.css")!!.toExternalForm())
        mainStage.scene = mainScene
        mainStage.onCloseRequest = EventHandler { Platform.exit() }

        // Icon to be displayed in the taskbar and this window
        val icon = Image(javaClass.getResource("/icon.png")!!.toString())
        mainStage.icons.add(icon)

        // Image
        with(ImageView(icon)) {
            fitWidth = HEIGHT * 0.4
            fitHeight = HEIGHT * 0.4
            layoutX = WIDTH / 2 - fitWidth / 2
            layoutY = HEIGHT / 2 - fitHeight / 2 - HEIGHT * 0.2
            root.children.add(this)
        }

        // Title
        with(Text("SUDOKU")) {
            font = font("Arial", FontWeight.BOLD, HEIGHT * 0.07)
            layoutX = WIDTH / 2 - boundsInLocal.width / 2
            layoutY = HEIGHT / 2 - boundsInLocal.height / 2 + HEIGHT * 0.15
            root.children.add(this)
        }

        // Load autosave
        val data = GameData.load(Game.AUTOSAVE_FILE_PATH)

        // Continue game button
        with(Button("Continue game")) {
            prefWidth = HEIGHT * 0.3
            prefHeight = HEIGHT * 0.05
            layoutX = WIDTH / 2 - prefWidth / 2
            layoutY = HEIGHT / 2 - prefHeight / 2 + HEIGHT * 0.2
            isDisable = data == null
            setOnAction { Game(data!!).start(mainStage) }
            root.children.add(this)
        }

        // New game button
        with(MenuButton("New game")) {
            prefWidth = HEIGHT * 0.3
            prefHeight = HEIGHT * 0.05
            layoutX = WIDTH / 2 - prefWidth / 2
            layoutY = HEIGHT / 2 - prefHeight / 2 + HEIGHT * 0.3
            styleClass.add("new-game-button")
            items.addAll(
                Difficulty.entries.map { difficulty ->
                    MenuItem(
                        difficulty.name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }).apply {
                        style = "-fx-pref-width: ${prefWidth * 0.95}; -fx-alignment: center;"
                        setOnAction { Game(GameData(generate(difficulty))).start(mainStage) }
                    }
                }
            )
            root.children.add(this)
        }

        // Centering the window
        with(Screen.getPrimary().visualBounds) {
            mainStage.x = (width - WIDTH) / 2
            mainStage.y = (height - HEIGHT) / 2
        }

        mainStage.show()
    }
}
