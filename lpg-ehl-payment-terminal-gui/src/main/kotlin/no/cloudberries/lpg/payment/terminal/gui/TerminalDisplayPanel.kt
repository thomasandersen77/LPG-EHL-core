package no.cloudberries.lpg.payment.terminal.gui

import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text

/**
 * Custom JavaFX panel that visually resembles a payment terminal LCD display.
 *
 * Dark background with green/white text showing current display text,
 * amount, and status information. Uses a Canvas for custom rendering.
 */
class TerminalDisplayPanel : Pane() {

    private var displayText: String = "TERMINAL STENGT"
    private var amountText: String = ""
    private var statusText: String = "CLOSED"
    private var isSuccess: Boolean? = null

    private val canvas = Canvas()

    private val displayFont = Font.font("Monospaced", FontWeight.BOLD, 28.0)
    private val amountFont = Font.font("Monospaced", FontWeight.BOLD, 22.0)
    private val statusFont = Font.font("Monospaced", FontWeight.NORMAL, 14.0)

    companion object {
        // Ingenico-style: light blue LCD screen, cyan/white text
        private val BG_COLOR = Color.rgb(0, 60, 92)      // #003C5C - Ingenico LCD blue
        private val BORDER_COLOR = Color.rgb(20, 25, 30) // Dark chassis
        private val TEXT_GREEN = Color.rgb(0, 255, 130)  // Success - bright cyan-green
        private val TEXT_RED = Color.rgb(255, 80, 60)
        private val TEXT_YELLOW = Color.rgb(255, 220, 100)
        private val TEXT_DIM = Color.rgb(120, 180, 200)  // Status text
        private val SCANLINE_COLOR = Color.rgb(10, 50, 75)
    }

    init {
        prefWidth = 420.0
        prefHeight = 200.0
        minWidth = 350.0
        minHeight = 160.0

        style = "-fx-border-color: rgb(25,28,32); -fx-border-width: 4; -fx-border-radius: 6; " +
                "-fx-background-color: rgb(0,60,92); -fx-background-radius: 6;"

        children.add(canvas)

        // Bind canvas size to pane size
        canvas.widthProperty().bind(widthProperty())
        canvas.heightProperty().bind(heightProperty())

        // Redraw when size changes
        widthProperty().addListener { _, _, _ -> draw() }
        heightProperty().addListener { _, _, _ -> draw() }
    }

    fun updateDisplay(text: String, amount: String? = null, success: Boolean? = null) {
        Platform.runLater {
            displayText = text
            amountText = amount ?: amountText
            isSuccess = success
            draw()
        }
    }

    fun updateStatus(state: String) {
        Platform.runLater {
            statusText = state
            draw()
        }
    }

    fun reset() {
        Platform.runLater {
            displayText = "TERMINAL STENGT"
            amountText = ""
            statusText = "CLOSED"
            isSuccess = null
            draw()
        }
    }

    private fun draw() {
        val gc = canvas.graphicsContext2D
        val w = canvas.width
        val h = canvas.height

        if (w <= 0 || h <= 0) return

        // Clear background
        gc.fill = BG_COLOR
        gc.fillRect(0.0, 0.0, w, h)

        // Draw scanline effect (subtle)
        gc.stroke = SCANLINE_COLOR
        gc.lineWidth = 1.0
        var y = 0.0
        while (y < h) {
            gc.strokeLine(0.0, y, w, y)
            y += 3.0
        }

        val padding = 20.0

        // Main display text
        gc.font = displayFont
        val textColor = when (isSuccess) {
            true -> TEXT_GREEN
            false -> TEXT_RED
            null -> if (statusText == "READY" || statusText == "BUSY") TEXT_GREEN else TEXT_YELLOW
        }
        gc.fill = textColor

        val textWidth = computeTextWidth(displayFont, displayText)
        val textX = (w - textWidth) / 2.0
        val textY = 50.0
        gc.fillText(displayText, textX, textY)

        // Amount line
        if (amountText.isNotBlank()) {
            gc.font = amountFont
            gc.fill = TEXT_GREEN
            val amWidth = computeTextWidth(amountFont, amountText)
            val amX = (w - amWidth) / 2.0
            gc.fillText(amountText, amX, textY + 40.0)
        }

        // Status line (bottom)
        gc.font = statusFont
        gc.fill = TEXT_DIM
        gc.fillText("● $statusText", padding + 15.0, h - 12.0)

        // Draw dot indicator for status
        val dotColor = when (statusText) {
            "READY" -> TEXT_GREEN
            "BUSY" -> TEXT_YELLOW
            "OPEN" -> TEXT_YELLOW
            else -> TEXT_RED
        }
        gc.fill = dotColor
        gc.fillOval(padding, h - 24.0, 10.0, 10.0)
    }

    private fun computeTextWidth(font: Font, text: String): Double {
        val helper = Text(text)
        helper.font = font
        return helper.layoutBounds.width
    }
}
