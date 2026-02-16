package no.cloudberries.lpg.payment.terminal.gui

import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.ArcType
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text

/**
 * Custom JavaFX panel that visually resembles an Ingenico payment terminal:
 * LCD display at top, contactless symbol, keypad, chip slot with green glow.
 * Dark chassis, matte black; display shows current text, amount, status.
 */
class TerminalDisplayPanel : Pane() {

    private var displayText: String = "TERMINAL STENGT"
    private var amountText: String = ""
    private var statusText: String = "CLOSED"
    private var isSuccess: Boolean? = null

    private val canvas = Canvas()

    private val displayFont = Font.font("Monospaced", FontWeight.BOLD, 24.0)
    private val amountFont = Font.font("Monospaced", FontWeight.BOLD, 20.0)
    private val statusFont = Font.font("Monospaced", FontWeight.NORMAL, 12.0)

    companion object {
        private val BG_COLOR = Color.rgb(0, 60, 92)      // Ingenico LCD blue
        private val CHASSIS_COLOR = Color.rgb(25, 28, 32)
        private val TEXT_GREEN = Color.rgb(0, 255, 130)
        private val TEXT_RED = Color.rgb(255, 80, 60)
        private val TEXT_YELLOW = Color.rgb(255, 220, 100)
        private val TEXT_DIM = Color.rgb(120, 180, 200)
        private val SCANLINE_COLOR = Color.rgb(10, 50, 75)
        private val CONTACTLESS_COLOR = Color.rgb(100, 180, 220)
        private val CHIP_SLOT_COLOR = Color.rgb(0, 200, 100)  // Green glow
    }

    init {
        prefWidth = 380.0
        prefHeight = 320.0
        minWidth = 320.0
        minHeight = 280.0

        style = "-fx-border-color: rgb(25,28,32); -fx-border-width: 6; -fx-border-radius: 8; " +
                "-fx-background-color: rgb(18,18,20); -fx-background-radius: 8;"

        children.add(canvas)

        canvas.widthProperty().bind(widthProperty())
        canvas.heightProperty().bind(heightProperty())

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

        val displayH = (h * 0.42).toDouble().coerceIn(120.0, 160.0)
        val padding = 16.0

        // Chassis background (matte black)
        gc.fill = CHASSIS_COLOR
        gc.fillRect(0.0, 0.0, w, h)

        // --- LCD display area (top) ---
        gc.fill = BG_COLOR
        gc.fillRoundRect(padding, padding, w - 2 * padding, displayH, 6.0, 6.0)

        gc.stroke = SCANLINE_COLOR
        gc.lineWidth = 1.0
        var y = padding + 2
        while (y < padding + displayH - 2) {
            gc.strokeLine(padding + 2, y, w - padding - 2, y)
            y += 3.0
        }

        gc.font = displayFont
        val textColor = when (isSuccess) {
            true -> TEXT_GREEN
            false -> TEXT_RED
            null -> if (statusText == "READY" || statusText == "BUSY") TEXT_GREEN else TEXT_YELLOW
        }
        gc.fill = textColor

        val textWidth = computeTextWidth(displayFont, displayText)
        val textX = (w - textWidth) / 2.0
        val textY = padding + 36.0
        gc.fillText(displayText, textX, textY)

        if (amountText.isNotBlank()) {
            gc.font = amountFont
            gc.fill = TEXT_GREEN
            val amWidth = computeTextWidth(amountFont, amountText)
            gc.fillText(amountText, (w - amWidth) / 2.0, textY + 34.0)
        }

        gc.font = statusFont
        gc.fill = TEXT_DIM
        gc.fillText("● $statusText", padding + 12.0, padding + displayH - 8.0)
        val dotColor = when (statusText) {
            "READY" -> TEXT_GREEN
            "BUSY" -> TEXT_YELLOW
            "OPEN" -> TEXT_YELLOW
            else -> TEXT_RED
        }
        gc.fill = dotColor
        gc.fillOval(padding + 2, padding + displayH - 20.0, 8.0, 8.0)

        // --- Contactless symbol (three curves + dot) ---
        val cx = w / 2.0
        val cy = padding + displayH + 22.0
        gc.stroke = CONTACTLESS_COLOR
        gc.lineWidth = 3.0
        gc.strokeArc(cx - 24, cy - 20, 18.0, 18.0, 220.0, 100.0, ArcType.OPEN)
        gc.strokeArc(cx - 14, cy - 12, 14.0, 14.0, 220.0, 100.0, ArcType.OPEN)
        gc.strokeArc(cx - 6, cy - 6, 8.0, 8.0, 220.0, 100.0, ArcType.OPEN)
        gc.fill = CONTACTLESS_COLOR
        gc.fillOval(cx - 3, cy - 3, 6.0, 6.0)

        // --- Keypad area (simplified 4x4 grid) ---
        val keyTop = cy + 18.0
        val keySize = 32.0
        val keyGap = 6.0
        val keys = listOf(
            "1", "2", "3",
            "4", "5", "6",
            "7", "8", "9",
            "|+|", "0", "|-|"
        )
        gc.fill = Color.rgb(45, 48, 52)
        for (row in 0 until 4) {
            for (col in 0 until 3) {
                val idx = row * 3 + col
                if (idx < keys.size) {
                    val kx = padding + 24 + col * (keySize + keyGap)
                    val ky = keyTop + row * (keySize + keyGap)
                    gc.fillRoundRect(kx, ky, keySize, keySize, 4.0, 4.0)
                }
            }
        }
        // Function keys (right column): orange X, yellow, green enter
        val fnY = keyTop
        gc.fill = Color.rgb(200, 100, 50)
        gc.fillRoundRect(w - padding - 52, fnY, 42.0, keySize, 4.0, 4.0)
        gc.fill = Color.rgb(220, 180, 60)
        gc.fillRoundRect(w - padding - 52, fnY + keySize + keyGap, 42.0, keySize, 4.0, 4.0)
        gc.fill = Color.rgb(50, 160, 80)
        gc.fillRoundRect(w - padding - 52, fnY + 2 * (keySize + keyGap), 42.0, keySize, 4.0, 4.0)

        // --- Chip slot (bottom, green glow) ---
        val slotY = h - 28.0
        gc.fill = Color.rgb(0, 50, 25)
        gc.fillRoundRect(padding + 20, slotY, w - 2 * padding - 40, 18.0, 4.0, 4.0)
        gc.fill = Color.rgb(0, 180, 90, 0.5)
        gc.fillRoundRect(padding + 22, slotY + 2, w - 2 * padding - 44, 14.0, 3.0, 3.0)

        // Branding
        gc.fill = Color.rgb(120, 120, 120)
        gc.font = Font.font("Monospaced", FontWeight.NORMAL, 10.0)
        gc.fillText("ingenico", padding + 12, h - 6)
    }

    private fun computeTextWidth(font: Font, text: String): Double {
        val helper = Text(text)
        helper.font = font
        return helper.layoutBounds.width
    }
}
