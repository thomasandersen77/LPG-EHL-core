#!/usr/bin/env kotlin
@file:DependsOn("no.cloudberries.lpg:lpg-transport:0.0.1-SNAPSHOT")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.13")

import no.cloudberries.lpg.communication.SerialPortManager

val ports = SerialPortManager.listAvailablePorts()
println("Detected serial ports (${ports.size}):")
ports.forEach { println(" - $it") }

println()
println("Tip:")
println("  EHL_SERIAL_PORT=/dev/ttyUSB0 kotlin kotlin-scripts/01_scan_addresses.main.kts")

