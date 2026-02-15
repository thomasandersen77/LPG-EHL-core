package no.cloudberries.lpg.payment.terminal.gui

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.Duration
import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.model.domain.Scenario
import no.cloudberries.lpg.payment.terminal.sim.model.response.EventEnvelope
import no.cloudberries.lpg.payment.terminal.sim.service.EventStoreListener
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalEventStore
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalStateManager
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Main GUI window for the Payment Terminal Simulator (JavaFX).
 *
 * Shows a visual payment terminal display with real-time event updates,
 * scenario selection, terminal controls, and an event log.
 */
class TerminalGuiFrame(
    private val eventStore: TerminalEventStore,
    private val stateManager: TerminalStateManager,
    private val config: SimulatorConfig,
    private val port: Int
) : EventStoreListener {

    private val log = LoggerFactory.getLogger(TerminalGuiFrame::class.java)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    // UI components
    private val displayPanel = TerminalDisplayPanel()
    private val scenarioCombo = ComboBox(FXCollections.observableArrayList(Scenario.entries))
    private val amountField = TextField("1500").apply {
        promptText = "Beløp (kr)"
        tooltip = Tooltip("Maks beløp å reservere (f.eks. 1000 eller 1500)")
    }
    private val stateLabel = Label("CLOSED")
    private val portLabel = Label("Port: $port")
    private val eventLog = TextArea()
    private val btnOpen = Button("Åpne Terminal")
    private val btnClose = Button("Lukk Terminal")
    private val btnTrekkeKort = Button("Trekke kort")
    private val btnPurchase = Button("Test Kjøp (100 kr)")
    private val btnClear = Button("Tøm Logg")

    private val httpClient = HttpClient.newHttpClient()
    private lateinit var stage: Stage

    fun show(stage: Stage) {
        this.stage = stage
        stage.title = "Payment Terminal Simulator — :$port"
        stage.scene = Scene(buildUI(), 520.0, 640.0)
        stage.minWidth = 500.0
        stage.minHeight = 600.0

        stage.setOnCloseRequest {
            eventStore.removeListener(this)
        }

        // Subscribe to events
        eventStore.addListener(this)

        // Periodic state refresh (200ms)
        val refreshTimeline = Timeline(KeyFrame(Duration.millis(200.0), { refreshState() }))
        refreshTimeline.cycleCount = Timeline.INDEFINITE
        refreshTimeline.play()

        stage.setOnHidden {
            refreshTimeline.stop()
            eventStore.removeListener(this)
        }

        appendLog("Simulator startet på port $port")
        appendLog("Scenario: ${config.defaultScenario}")

        stage.show()
    }

    private fun buildUI(): BorderPane {
        val root = BorderPane()
        root.padding = Insets(12.0)
        root.style = "-fx-background-color: #0A0A0A;"

        // --- Top: Terminal Display (Ingenico-style) ---
        root.top = displayPanel
        BorderPane.setMargin(displayPanel, Insets(0.0, 0.0, 8.0, 0.0))

        // --- Center: Controls + Info ---
        val centerBox = VBox(6.0)

        // Status row
        val statusRow = HBox(10.0)
        statusRow.alignment = Pos.CENTER_LEFT
        statusRow.padding = Insets(4.0, 0.0, 4.0, 0.0)

        val statusCaption = Label("Status:")
        statusCaption.style = "-fx-text-fill: rgb(140,140,140);"
        stateLabel.style = "-fx-text-fill: rgb(200,200,200); -fx-font-weight: bold; -fx-font-size: 14;"
        portLabel.style = "-fx-text-fill: rgb(120,120,120); -fx-font-size: 12;"
        statusRow.children.addAll(statusCaption, stateLabel, portLabel)

        // Scenario + amount row
        val scenarioRow = HBox(10.0)
        scenarioRow.alignment = Pos.CENTER_LEFT
        scenarioRow.padding = Insets(4.0, 0.0, 4.0, 0.0)

        val scenarioCaption = Label("Scenario:")
        scenarioCaption.style = "-fx-text-fill: rgb(140,140,140);"
        scenarioCombo.value = try {
            Scenario.valueOf(config.defaultScenario.uppercase())
        } catch (_: Exception) {
            Scenario.APPROVED
        }
        scenarioCombo.style = "-fx-background-color: rgb(35,35,38); -fx-text-fill: white;"
        scenarioCombo.tooltip = Tooltip("Velg scenario for neste operasjon")
        amountField.style = "-fx-background-color: rgb(35,35,38); -fx-text-fill: white; -fx-pref-width: 80;"
        val amountCaption = Label("Reservasjon (kr):")
        amountCaption.style = "-fx-text-fill: rgb(140,140,140);"
        scenarioRow.children.addAll(scenarioCaption, scenarioCombo, amountCaption, amountField)

        // Action buttons
        val buttonRow = HBox(8.0)
        buttonRow.alignment = Pos.CENTER_LEFT
        buttonRow.padding = Insets(6.0, 0.0, 4.0, 0.0)

        btnOpen.tooltip = Tooltip("POST /v1/terminal/open")
        btnClose.tooltip = Tooltip("POST /v1/terminal/close")
        btnTrekkeKort.tooltip = Tooltip("POST /v1/payments/reservation - Simulerer korttrekking, frigir pumpe")
        btnPurchase.tooltip = Tooltip("POST /v1/payments/purchase (100 kr) - Direkte kjøp")
        btnClear.tooltip = Tooltip("Tøm hendelsesloggen")

        btnOpen.style = "-fx-background-color: rgb(50,50,55); -fx-text-fill: white;"
        btnClose.style = "-fx-background-color: rgb(50,50,55); -fx-text-fill: white;"
        btnTrekkeKort.style = "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold;"
        btnPurchase.style = "-fx-background-color: rgb(50,50,55); -fx-text-fill: white;"
        btnClear.style = "-fx-background-color: rgb(50,50,55); -fx-text-fill: white;"

        btnOpen.setOnAction { performAction("terminal/open", "POST") }
        btnClose.setOnAction { performAction("terminal/close", "POST") }
        btnTrekkeKort.setOnAction { performReservation() }
        btnPurchase.setOnAction { performPurchase() }
        btnClear.setOnAction { eventLog.clear(); appendLog("Logg tømt") }

        buttonRow.children.addAll(btnOpen, btnClose, btnTrekkeKort, btnPurchase, btnClear)

        centerBox.children.addAll(statusRow, scenarioRow, buttonRow)
        root.center = centerBox

        // --- Bottom: Event Log ---
        eventLog.isEditable = false
        eventLog.isWrapText = true
        eventLog.prefRowCount = 12
        eventLog.style = "-fx-control-inner-background: rgb(30,30,32); " +
                "-fx-text-fill: rgb(180,200,180); " +
                "-fx-font-family: 'Monospaced'; -fx-font-size: 12;"

        val logBox = VBox(4.0)
        val logTitle = Label("Hendelser")
        logTitle.style = "-fx-text-fill: rgb(150,150,150); -fx-font-size: 12;"
        logBox.children.addAll(logTitle, eventLog)
        logBox.padding = Insets(8.0, 0.0, 0.0, 0.0)
        VBox.setVgrow(eventLog, javafx.scene.layout.Priority.ALWAYS)

        root.bottom = logBox

        return root
    }

    /**
     * Called by TerminalEventStore when a new event is published (may be on any thread).
     */
    override fun onEvent(event: EventEnvelope) {
        Platform.runLater {
            val payload = event.Payload
            when (event.EventType) {
                "DisplayText" -> {
                    val text = payload["text"]?.toString() ?: ""
                    if (text.isNotBlank()) {
                        displayPanel.updateDisplay(text)
                        appendLog("📺 $text")
                    }
                }
                "OperationStarted" -> {
                    val type = payload["type"]?.toString() ?: "operation"
                    val amount = payload["amountMinor"]?.toString()?.toIntOrNull()
                    val amountStr = if (amount != null) "NOK %.2f".format(amount / 100.0) else ""
                    displayPanel.updateDisplay("BEHANDLER...", amountStr, null)
                    appendLog("▶ $type startet" + if (amountStr.isNotBlank()) " ($amountStr)" else "")
                }
                "OperationCompleted" -> {
                    val success = (payload["Success"] as? Boolean) ?: (payload["success"] as? Boolean) ?: false
                    val text = if (success) "GODKJENT" else "AVVIST"
                    displayPanel.updateDisplay(text, success = success)
                    appendLog(if (success) "✅ Operasjon godkjent" else "❌ Operasjon avvist")
                }
                "PrintText" -> {
                    appendLog("🧾 Kvittering generert")
                }
                else -> {
                    appendLog("📌 ${event.EventType}")
                }
            }
        }
    }

    private fun refreshState() {
        val state = stateManager.getState().name
        Platform.runLater {
            stateLabel.text = state
            displayPanel.updateStatus(state)

            // Update button states
            val isOpen = state == "READY" || state == "OPEN"
            btnOpen.isDisable = isOpen
            btnClose.isDisable = !isOpen || state == "BUSY"
            btnTrekkeKort.isDisable = state != "READY"
            btnPurchase.isDisable = state != "READY"
        }
    }

    private fun performAction(endpoint: String, method: String) {
        Thread {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:$port/v1/$endpoint"))
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .header("Content-Type", "application/json")
                    .build()

                appendLog("→ $method /v1/$endpoint")
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                appendLog("← ${response.statusCode()}")
            } catch (ex: Exception) {
                appendLog("⚠ Feil: ${ex.message}")
            }
        }.start()
    }

    private fun performReservation() {
        val scenario = scenarioCombo.value
        val amountKr = amountField.text.toIntOrNull() ?: 1500
        val amountMinor = amountKr * 100
        Thread {
            try {
                val body = """{"AmountMinor": $amountMinor, "Currency": "NOK", "OperatorId": "0000"}"""
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:$port/v1/payments/reservation"))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/json")

                if (scenario != Scenario.APPROVED) {
                    builder.header("X-Terminal-Scenario", scenario.name)
                }

                appendLog("→ POST /v1/payments/reservation ($amountKr kr, scenario=${scenario.name})")
                val response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
                appendLog("← ${response.statusCode()}")
            } catch (ex: Exception) {
                appendLog("⚠ Feil: ${ex.message}")
            }
        }.start()
    }

    private fun performPurchase() {
        val scenario = scenarioCombo.value
        Thread {
            try {
                val body = """{"AmountMinor": 10000, "Currency": "NOK", "OperatorId": "0000"}"""
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:$port/v1/payments/purchase"))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/json")

                if (scenario != Scenario.APPROVED) {
                    builder.header("X-Terminal-Scenario", scenario.name)
                }

                appendLog("→ POST /v1/payments/purchase (scenario=${scenario.name})")
                val response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
                appendLog("← ${response.statusCode()}")
            } catch (ex: Exception) {
                appendLog("⚠ Feil: ${ex.message}")
            }
        }.start()
    }

    private fun appendLog(message: String) {
        Platform.runLater {
            val time = LocalTime.now().format(timeFormatter)
            eventLog.appendText("$time  $message\n")
        }
    }
}
