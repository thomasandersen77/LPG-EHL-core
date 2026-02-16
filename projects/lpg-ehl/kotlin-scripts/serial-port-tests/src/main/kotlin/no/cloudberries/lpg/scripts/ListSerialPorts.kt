package no.cloudberries.lpg.scripts

import no.cloudberries.lpg.communication.SerialPortManager

/**
 * Maven run:
 *   ./mvnw -pl kotlin-scripts -Dexec.mainClass=no.cloudberries.lpg.scripts.ListSerialPortsKt exec:java
 */
fun cmdListPorts() {
    val ports = SerialPortManager.listAvailablePorts()
    println("Detected serial ports (${ports.size}):")
    ports.forEach { println(" - $it") }
}

fun main() = cmdListPorts()

