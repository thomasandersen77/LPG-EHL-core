package no.cloudberries.lpg.pls.sim

import kotlin.test.Test
import kotlin.test.assertTrue

class PlsStateUnblockTest {
    @Test
    fun `unblock sets open_for_delivery bit in next state response`() {
        val plsState = PlsState()
        val addr = 0x01.toByte()

        val unblockFrame = EhlFrame(EhlFrameCodec.STX_CONTROLLER, addr, EhlFrameCodec.CMD_UNBLOCK, ByteArray(0))
        val stateFrame = EhlFrame(EhlFrameCodec.STX_CONTROLLER, addr, EhlFrameCodec.CMD_STATE, ByteArray(0))

        plsState.processEhlCommand(unblockFrame)
        val response = plsState.processEhlCommand(stateFrame)

        val statusByte = when (response) {
            is EhlCommandResult.StateResponse -> response.data[0].toInt() and 0xFF
            else -> error("Expected StateResponse, got $response")
        }

        assertTrue(
            statusByte and 0x02 != 0,
            "Expected open_for_delivery bit set after UNBLOCK, got 0x${statusByte.toString(16).uppercase()}"
        )
    }
}