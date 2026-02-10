package no.cloudberries.lpg.pls.sim

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.util.Duration
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("PlsGui")

/**
 * Simple JavaFX GUI for PLS Simulator.
 * Black background, status text, and a red button that toggles Start/Stop (deadman switch simulation).
 */
class PlsGui(private val plsState: PlsState) {

    private val btn = Button("START").apply {
        style = """
            -fx-background-color: #C62828;
            -fx-text-fill: white;
            -fx-font-size: 18px;
            -fx-font-weight: bold;
            -fx-padding: 20 60;
            -fx-min-width: 200;
            -fx-min-height: 80;
        """.trimIndent()
    }

    private val statusLabel = Label("Venter på UNBLOCK fra tjenesten...").apply {
        style = "-fx-text-fill: rgb(180,180,180); -fx-font-size: 14;"
    }

    private val volumeLabel = Label("0.00 L").apply {
        style = "-fx-text-fill: rgb(200,200,200); -fx-font-size: 24; -fx-font-weight: bold;"
    }

    fun show(stage: Stage) {
        stage.title = "PLS Simulator - Dødmannsknapp"
        stage.isResizable = false

        val root = VBox(20.0).apply {
            alignment = Pos.CENTER
            padding = Insets(30.0)
            style = "-fx-background-color: black;"
        }

        val titleLabel = Label("Pumpe-simulator").apply {
            style = "-fx-text-fill: rgb(140,140,140); -fx-font-size: 16;"
        }

        root.children.addAll(titleLabel, statusLabel, volumeLabel, btn)
        stage.scene = Scene(root, 320.0, 280.0)

        btn.setOnAction {
            val state = plsState.getState()
            when (state) {
                DispenserState.AUTHORIZED -> {
                    plsState.simulateNozzleLift(true)
                    btn.text = "STOPP"
                    btn.style = """
                        -fx-background-color: #2E7D32;
                        -fx-text-fill: white;
                        -fx-font-size: 18px;
                        -fx-font-weight: bold;
                        -fx-padding: 20 60;
                        -fx-min-width: 200;
                        -fx-min-height: 80;
                    """.trimIndent()
                    log.info("▶️ GUI: Start fylling")
                }
                DispenserState.PUMPING -> {
                    plsState.simulateNozzleLift(false)
                    btn.text = "START"
                    btn.style = """
                        -fx-background-color: #C62828;
                        -fx-text-fill: white;
                        -fx-font-size: 18px;
                        -fx-font-weight: bold;
                        -fx-padding: 20 60;
                        -fx-min-width: 200;
                        -fx-min-height: 80;
                    """.trimIndent()
                    log.info("⏹️ GUI: Stopp fylling")
                }
                else -> {
                    statusLabel.text = "Tilstand: $state - venter på UNBLOCK"
                }
            }
        }

        // Update status/volume periodically
        val handler = EventHandler<javafx.event.ActionEvent> {
            val s = plsState.getState()
            statusLabel.text = when (s) {
                DispenserState.IDLE -> "IDLE - Venter på UNBLOCK fra tjenesten"
                DispenserState.AUTHORIZED -> "KLAR - Klikk START for å fylle"
                DispenserState.PUMPING -> "FYLLER - Klikk STOPP for å stoppe"
                else -> "Tilstand: $s"
            }
            val volL = plsState.getVolumeMl() / 1000.0
            val priceKr = plsState.getPrice() / 100.0
            val amountKr = volL * priceKr
            volumeLabel.text = "%.2f L  ·  %.2f kr".format(volL, amountKr)
        }
        val keyFrame = KeyFrame(Duration.millis(200.0), handler)
        Timeline(keyFrame).apply {
            cycleCount = Timeline.INDEFINITE
            play()
        }

        stage.setOnCloseRequest {
            PlsGuiApp.onGuiClosed?.invoke()  // Trigger shutdown
            Platform.exit()
        }

        stage.show()
        log.info("PLS GUI åpnet")
    }
}

/**
 * JavaFX Application entry point for PLS GUI.
 * PlsState must be set via PlsGuiApp.state before launch.
 */
class PlsGuiApp : Application() {
    override fun start(stage: Stage) {
        val state = PlsGuiApp.state
        if (state != null) {
            PlsGui(state).show(stage)
        } else {
            log.warn("PlsState not set - GUI cannot start")
        }
    }

    companion object {
        @Volatile
        var state: PlsState? = null

        @Volatile
        var onGuiClosed: (() -> Unit)? = null
    }
}
