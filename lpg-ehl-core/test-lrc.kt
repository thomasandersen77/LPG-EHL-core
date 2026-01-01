#!/usr/bin/env kotlin

// Quick LRC test
val payload = "P,1,100"
val payloadBytes = payload.toByteArray(Charsets.ISO_8859_1)
val etx: Byte = 0x03

var lrc: Byte = 0
for (b in payloadBytes) {
    lrc = (lrc.toInt() xor b.toInt()).toByte()
}
lrc = (lrc.toInt() xor etx.toInt()).toByte()

println("Payload: $payload")
println("Bytes: ${payloadBytes.joinToString(" ") { "%02X".format(it) }}")
println("ETX: %02X".format(etx))
println("LRC: %02X".format(lrc))
println()
println("Complete frame should be:")
print("02 ")  // STX
print(payloadBytes.joinToString(" ") { "%02X".format(it) })
print(" 03 ")  // ETX
println("%02X".format(lrc))  // LRC
