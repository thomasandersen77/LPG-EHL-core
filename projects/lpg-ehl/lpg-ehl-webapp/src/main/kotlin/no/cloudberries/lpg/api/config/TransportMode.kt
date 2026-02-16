package no.cloudberries.lpg.api.config

/**
 * Transport modes for EHL serial communication.
 * 
 * EMULATOR (Default for webapp):
 *   - Uses in-memory transport with EhlDispenserEmulator
 *   - No physical hardware required
 *   - Perfect for development and unit testing
 * 
 * SOCAT (Development/Integration testing):
 *   - Uses real serial transport connected to socat PTY
 *   - PLS Simulator runs on the other end of the PTY pair
 *   - Realistic serial communication testing without hardware
 * 
 * HARDWARE (Production):
 *   - Uses real serial transport to physical RS-485 port
 *   - Communicates with actual LPG dispenser hardware
 *   - Only for production deployment on ARK-3600 or similar
 */
enum class TransportMode {
    /**
     * In-memory emulator - no hardware required.
     * Default for lpg-ehl-webapp development.
     */
    EMULATOR,
    
    /**
     * Virtual serial port via socat + PLS Simulator.
     * For integration testing with realistic serial communication.
     */
    SOCAT,
    
    /**
     * Real RS-485 serial port to physical hardware.
     * Production mode for actual LPG dispensers.
     */
    HARDWARE
}
