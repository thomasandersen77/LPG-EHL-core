package no.cloudberries.lpg.emulator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LpgEhlEmulatorApplication

fun main(args: Array<String>) {
    // Force IPv4 for Windows Dispenserkontroll compatibility (Parallels/VMware networking)
    // This ensures the TCP server binds to 0.0.0.0 (IPv4) instead of :: (IPv6)
    // Emulator is only for local testing with legacy Windows software
    System.setProperty("java.net.preferIPv4Stack", "true")
    
    runApplication<LpgEhlEmulatorApplication>(*args)
}
