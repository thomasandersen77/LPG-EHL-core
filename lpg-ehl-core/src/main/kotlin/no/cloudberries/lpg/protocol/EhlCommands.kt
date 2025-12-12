package no.cloudberries.lpg.protocol

/**
 * EHL Protocol Commands
 * 
 * Defines all command codes used in the EHL (European Hexadecimal Language) protocol
 * for communication with LPG dispensers over RS-485.
 */
enum class EhlCommand(val code: Int, val description: String) {
    /** Command acknowledgement - dispenser confirms command received */
    OK(30, "Command acknowledgement"),
    
    /** Error code data - dispenser reports error */
    ERROR(37, "Error code data"),
    
    /** Stop the dispenser */
    STOP(47, "Stop the dispenser"),
    
    /** Give/take the calculator state */
    STATE(75, "Give/take the calculator state"),
    
    /** Give/take the fuel amount (volume) */
    VOLUME(69, "Give/take the fuel amount"),
    
    /** Give/take the fuel price */
    PRICE(92, "Give/take the fuel price"),
    
    /** Block - stop the dispenser */
    BLOCK(105, "Block/stop the dispenser"),
    
    /** Transmission channel test */
    LINETEST(106, "Transmission channel test"),
    
    /** Programming fuel amount to delivery (volume preset) */
    PROG_I(112, "Programming fuel amount to delivery"),
    
    /** Programming fuel value to delivery (money preset) */
    PROG_W(117, "Programming fuel value to delivery"),
    
    /** Start delivery mode - unblock dispenser */
    UNBLOCK(119, "Start delivery mode"),
    
    /** Reset the calculator */
    ZER(129, "Reset the calculator"),
    
    /** Programming of fuel price */
    PROG_PRC(169, "Programming of fuel price"),
    
    /** Tank status/control */
    TANK(197, "Tank status/control"),
    
    /** Unknown/unsupported command */
    UNKNOWN(-1, "Unknown command");
    
    companion object {
        /**
         * Find EHL command by its code value
         */
        fun fromCode(code: Int): EhlCommand {
            return entries.find { it.code == code } ?: UNKNOWN
        }
    }
}

/**
 * EHL Protocol Constants
 */
object EhlProtocol {
    /** Start of transmission byte */
    const val STX: Byte = 0x20
    
    /** End of transmission byte */
    const val ETX: Byte = 0x36
    
    /** Minimum packet length (STX + LEN + ADDR + CMD + CHKSUM + ETX) */
    const val MIN_PACKET_LENGTH = 6
    
    /** Maximum packet length */
    const val MAX_PACKET_LENGTH = 256
    
    /** Default timeout for dispenser response (seconds) */
    const val DEFAULT_TIMEOUT_SECONDS = 120
}
