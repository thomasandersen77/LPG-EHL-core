package no.cloudberries.lpg.protocol

/**
 * EHL-x4 Fault Model
 * 
 * Based on MM Petro EHL-x4 Installation and User Manual (Section 7.4 - Description of faults displayed)
 * 
 * This enum represents the complete fault/error model for the EHL-x4 electronic counter.
 * Each fault code (E-01 through E-09) has specific meaning, severity, and recommended action.
 * 
 * ## Protocol Context:
 * Faults are displayed on the dispenser in the "unit price" field as: `E-{fault_number}`
 * 
 * ## Architecture Placement:
 * - **lpg-ehl-core**: Protocol-level fault definitions and parsing (this file)
 * - **lpg-ehl-api**: DispenserService reacts to faults, blocks operations, logs structured events
 */
enum class EhlFault(
    val code: String,
    val description: String,
    val level: EhlErrorLevel,
    val recommendedAction: String,
    val autoRetryable: Boolean = false
) {
    /**
     * E-01: Improper operation of EHI converter
     * 
     * Source: MM Petro manual page 16
     * - Dispenser continues to operate correctly except faulty hose
     * - Repeat the operation that caused occurrence of fault
     */
    E_01(
        code = "E-01",
        description = "Feil bruk av EHI-konverter / Improper operation of EHI converter",
        level = EhlErrorLevel.WARNING,
        recommendedAction = "Gjenta operasjonen. Hvis feilen gjentar seg, kall service. / Repeat operation, if fault persists call service.",
        autoRetryable = true
    ),
    
    /**
     * E-02: Improper operation of EHI converter - difference in channels
     * 
     * Source: MM Petro manual page 16
     * - Channel mismatch detected in pulse converter
     * - Dispenser continues except faulty hose
     */
    E_02(
        code = "E-02",
        description = "Feil bruk av EHI-konverter - forskjell i kanaler / EHI converter channel difference",
        level = EhlErrorLevel.WARNING,
        recommendedAction = "Sjekk konverteren. Gjenta operasjonen. / Check converter, repeat operation.",
        autoRetryable = true
    ),
    
    /**
     * E-05: Damage to memory of counter configuration
     * 
     * Source: MM Petro manual page 16
     * - **CRITICAL**: Blocks entire dispenser
     * - Configuration parameters corrupted (price, etc.) except totalizer values
     * - Requires re-programming all parameters via Fn-00 through Fn-XX
     */
    E_05(
        code = "E-05",
        description = "Skade på konfigurasjonminne / Configuration memory damage",
        level = EhlErrorLevel.CRITICAL,
        recommendedAction = "BLOKKERING: Telleapparat i leveringsmodus blokkert. Reset alle funksjoner. / Reset all counter functions to pre-fault values.",
        autoRetryable = false
    ),
    
    /**
     * E-06: Damage to data base – memory fault
     * 
     * Source: MM Petro manual page 16-17
     * - **CRITICAL**: Blocks entire dispenser
     * - Database/EEPROM corruption detected
     * - May require IC3 chip removal/reinsertion or CPU package replacement
     */
    E_06(
        code = "E-06",
        description = "Skade på database - minnefeil / Database memory fault",
        level = EhlErrorLevel.CRITICAL,
        recommendedAction = "BLOKKERING: Fjern og sett inn IC3 chip. Reset alle parametere (unntatt Fn-29). / Remove/reinsert IC3, reset parameters except Fn-29.",
        autoRetryable = false
    ),
    
    /**
     * E-07: Damage to EHI converter supply system
     * 
     * Source: MM Petro manual page 17
     * - Current consumption in converter circuit beyond 40mA±10mA range
     * - Check cabling to flow-meter and pulse generator
     */
    E_07(
        code = "E-07",
        description = "Skade på EHI-konverterens strømforsyning / EHI converter supply system damage",
        level = EhlErrorLevel.CRITICAL,
        recommendedAction = "Sjekk kabling til pulsgiver. Bytt pulser. Sjekk strømforbruk (skal være 40mA±10mA). / Check cabling, replace pulse generator.",
        autoRetryable = false
    ),
    
    /**
     * E-08: Damage in connection system of electromechanical adder of liters delivered
     * 
     * Source: MM Petro manual page 17
     * - **CRITICAL**: Blocks dispenser
     * - Totalizer connection failure
     */
    E_08(
        code = "E-08",
        description = "Skade i koblingssystem på elektromekanisk adder / Electromechanical adder connection damage",
        level = EhlErrorLevel.CRITICAL,
        recommendedAction = "Dispenser blokkert. Sjekk koblingssystem til elektromekaniske addere. / Check totalizer connection system.",
        autoRetryable = false
    ),
    
    /**
     * E-09: Voltage control system detects supply decays
     * 
     * Source: MM Petro manual page 17
     * - Power supply instability or voltage drops detected
     * - Check 230V installation, use UPS if needed
     * - NOTE: Displayed after power-on, cancelled by first nozzle lift
     */
    E_09(
        code = "E-09",
        description = "Spenningskontrollsystem oppdager forsyningsfall / Voltage control detects supply decay",
        level = EhlErrorLevel.WARNING,
        recommendedAction = "Sjekk strømforsyning (230V). Bruk UPS. Feil vises ved oppstart, kanselleres ved første pistolløft. / Check power supply, use UPS.",
        autoRetryable = true
    ),
    
    /**
     * E-XX: Unknown fault code
     * 
     * Catchall for unrecognized error codes from hardware
     */
    UNKNOWN(
        code = "E-XX",
        description = "Ukjent feilkode / Unknown fault code",
        level = EhlErrorLevel.WARNING,
        recommendedAction = "Ukjent feil. Sjekk manual eller kontakt service. / Unknown fault, check manual or contact service.",
        autoRetryable = false
    );
    
    companion object {
        /**
         * Parse fault code from display string.
         * 
         * EHL-x4 sends fault codes as strings in format: "E-01", "E-02", etc.
         * These appear in the "unit price" field of the display.
         * 
         * @param code Raw string from display (e.g. "E-01", "E 01", "e-01")
         * @return Corresponding EhlFault, or UNKNOWN if not recognized
         */
        fun fromDisplayCode(code: String): EhlFault {
            // Normalize: remove whitespace, convert to uppercase
            val cleanCode = code.trim().replace(Regex("\\s+"), "-").uppercase()
            
            return entries.find { it.code == cleanCode } ?: UNKNOWN
        }
        
        /**
         * Check if a string represents a fault code.
         * 
         * @param str String to check (e.g. "E-01", "Fn-05", "12.50")
         * @return true if string matches E-XX pattern
         */
        fun isFaultCode(str: String): Boolean {
            return str.trim().matches(Regex("^E-?\\d{2}$", RegexOption.IGNORE_CASE))
        }
    }
}

/**
 * Severity level for EHL faults.
 * 
 * ## WARNING:
 * - Hose-specific issue, dispenser can continue with other hoses
 * - May auto-clear after retry or nozzle hang-up/lift cycle
 * 
 * ## CRITICAL:
 * - Entire dispenser blocked
 * - Requires manual intervention (service, power cycle, parameter reset)
 * - Cannot deliver fuel until resolved
 */
enum class EhlErrorLevel {
    /**
     * Warning: Hose-specific, dispenser continues, may auto-clear
     */
    WARNING,
    
    /**
     * Critical: Dispenser blocked, requires service/intervention
     */
    CRITICAL
}
