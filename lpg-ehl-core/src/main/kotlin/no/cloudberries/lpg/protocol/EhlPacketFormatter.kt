package no.cloudberries.lpg.protocol

/**
 * Human-readable formatter for EHL packets and protocol data.
 * Converts raw protocol data into readable strings for logging and debugging.
 */
object EhlPacketFormatter {
    
    /**
     * Format an EHL packet as a human-readable string with both hex and decoded information.
     * 
     * Example output:
     * "📤 SENDING → Dispenser #1 | STATE Query | Bytes: [20 06 01 4B 6C 36] | Checksum: 0x6C ✓"
     */
    fun formatPacketForLogging(packet: EhlPacket, direction: Direction): String {
        val arrow = when (direction) {
            Direction.SENDING -> "📤 SENDING →"
            Direction.RECEIVING -> "📥 RECEIVING ←"
        }
        
        val commandDesc = formatCommandDescription(packet)
        val rawBytes = EhlCodec.encode(packet).joinToString(" ") { "%02X".format(it) }
        val checksum = packet.calculateChecksum()
        
        return "$arrow Dispenser #${packet.address} | $commandDesc | Bytes: [$rawBytes] | Checksum: 0x%02X ✓".format(checksum)
    }
    
    /**
     * Format command with its description and decoded data payload.
     */
    private fun formatCommandDescription(packet: EhlPacket): String {
        val baseName = "${packet.command.name} (${packet.command.description})"
        
        if (packet.data.isEmpty()) {
            return baseName
        }
        
        val dataDescription = when (packet.command) {
            EhlCommand.STATE -> formatStateData(packet.data)
            EhlCommand.VOLUME -> formatVolumeData(packet.data)
            EhlCommand.PRICE -> formatPriceData(packet.data)
            EhlCommand.PROG_PRC -> formatPriceData(packet.data)
            EhlCommand.ERROR -> formatErrorData(packet.data)
            EhlCommand.PROG_AMOUNT -> formatValuePresetData(packet.data)
            EhlCommand.PROG_VOLUME -> formatVolumePresetData(packet.data)
            else -> "Data: [${packet.data.joinToString(" ") { "%02X".format(it) }}]"
        }
        
        return "$baseName | $dataDescription"
    }
    
    private fun formatStateData(data: ByteArray): String {
        if (data.isEmpty()) return "Query"
        
        val statusByte = data[0]
        val stateCode = statusByte.toInt() and 0xFF

        // VB6/SSOT bit mapping (see StatusBitMasks):
        // - 0x02 = OPEN_FOR_DELIVERY
        // - 0x04 = START_BUTTON_PRESSED
        // - 0x08 = AUTOMODE (used by legacy VB6 for transaction/payment state)
        // - 0x80 = ERROR_FLAG
        val hasError = StatusBitMasks.isBitSet(statusByte, StatusBitMasks.ERROR_FLAG)
        val automode = StatusBitMasks.isBitSet(statusByte, StatusBitMasks.AUTOMODE)
        val startButton = StatusBitMasks.isBitSet(statusByte, StatusBitMasks.START_BUTTON_PRESSED)
        val openForDelivery = StatusBitMasks.isBitSet(statusByte, StatusBitMasks.OPEN_FOR_DELIVERY)

        val bits = stateCode
            .toString(radix = 2)
            .padStart(8, '0')
        
        val stateName = when {
            hasError -> "ERROR (Dispenser error)"
            automode && !startButton && !openForDelivery -> "PAYMENT_PENDING (Awaiting settlement)"
            startButton && openForDelivery -> "PUMPING (Fuel flowing)"  // 0x06
            startButton && !openForDelivery -> "AUTHORIZED (Ready for nozzle)"  // 0x04
            !startButton && !openForDelivery && !automode -> "IDLE (Ready for new transaction)"  // 0x00
            openForDelivery && !startButton -> "OPEN_FOR_DELIVERY (Awaiting start)"  // 0x02 (may be observed directly after UNBLOCK)
            else -> "UNKNOWN (bits: 0x%02X)".format(stateCode)
        }

        return "State=0x%02X bits=%s open_for_delivery=%s startbutton=%s automode=%s error=%s (%s)".format(
            stateCode,
            bits,
            openForDelivery,
            startButton,
            automode,
            hasError,
            stateName
        )
    }
    
    private fun formatVolumeData(data: ByteArray): String {
        if (data.size != 5) return "Invalid data size: ${data.size} (expected 5 ASCII digits LSB-first)"
        
        return try {
            val litres = EhlDataParser.parseVolumeDataVb6(data)
            "Volume=%.2f L".format(litres)
        } catch (e: Exception) {
            "Parse error: ${e.message}"
        }
    }
    
    private fun formatPriceData(data: ByteArray): String {
        if (data.size != 4) return "Invalid data size: ${data.size}"
        
        return try {
            val priceString = EhlDataParser.parsePriceData(data)
            "Price=$priceString kr/L"
        } catch (e: Exception) {
            "Parse error: ${e.message}"
        }
    }
    
    private fun formatErrorData(data: ByteArray): String {
        if (data.isEmpty()) return "No error code"
        
        val errorCode = data[0].toInt() and 0xFF
        val errorDesc = when (errorCode) {
            0x01 -> "Checksum error"
            0x02 -> "Invalid format"
            0x03 -> "Invalid data size"
            0x04 -> "Invalid price format"
            0x10 -> "Unsupported command"
            else -> "Unknown error"
        }
        return "Error=0x%02X ($errorDesc)".format(errorCode)
    }
    
    private fun formatValuePresetData(data: ByteArray): String {
        if (data.size != 4) return "Invalid data size: ${data.size}"
        
        // BCD format decoding (simplified - shows hex)
        val hex = data.joinToString("") { "%02X".format(it) }
        return "Preset amount=$hex (BCD)"
    }
    
    private fun formatVolumePresetData(data: ByteArray): String {
        if (data.isEmpty()) return "No preset"
        
        val hex = data.joinToString(" ") { "%02X".format(it) }
        return "Preset volume=$hex (BCD)"
    }
    
    /**
     * Format a summary of a communication operation.
     * Used for high-level operation logging.
     */
    fun formatOperationSummary(
        operation: String,
        address: Int,
        success: Boolean,
        details: String = ""
    ): String {
        val icon = if (success) "✅" else "❌"
        val status = if (success) "SUCCESS" else "FAILED"
        val detailsStr = if (details.isNotEmpty()) " | $details" else ""
        
        return "$icon $operation | Dispenser #$address | $status$detailsStr"
    }
    
    /**
     * Format buffer status for debugging.
     */
    fun formatBufferStatus(bufferSize: Int, operation: String): String {
        val icon = when {
            bufferSize == 0 -> "🔵"
            bufferSize < 100 -> "🟢"
            bufferSize < 500 -> "🟡"
            else -> "🔴"
        }
        return "$icon Buffer $operation | Size: $bufferSize bytes"
    }
    
    /**
     * Format error with context.
     */
    fun formatError(errorType: String, details: String, context: String = ""): String {
        val contextStr = if (context.isNotEmpty()) " | Context: $context" else ""
        return "❌ ERROR: $errorType | $details$contextStr"
    }
    
    /**
     * Format timeout information.
     */
    fun formatTimeout(operation: String, timeoutMs: Long): String {
        return "⏱️ TIMEOUT: $operation | Waited ${timeoutMs}ms with no response"
    }
    
    /**
     * Format state transition for emulator.
     */
    fun formatStateTransition(from: String, to: String, reason: String = ""): String {
        val reasonStr = if (reason.isNotEmpty()) " | Reason: $reason" else ""
        return "🔄 STATE CHANGE: $from → $to$reasonStr"
    }
    
    /**
     * Format delivery progress.
     */
    fun formatDeliveryProgress(litres: Double, amount: Int, pricePerLitre: Int): String {
        val kr = amount / 100.0
        val price = pricePerLitre / 100.0
        return "⛽ DELIVERY: %.2f L × %.2f kr/L = %.2f kr".format(litres, price, kr)
    }
    
    enum class Direction {
        SENDING,
        RECEIVING
    }
}
