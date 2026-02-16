package no.cloudberries.lpg.pls.sim

import kotlin.random.Random

enum class SimProfile {
    LAB,
    FIELD
}

enum class ReadChunkingMode {
    OFF,
    RANDOM
}

data class IntRangeConfig(val min: Int, val max: Int) {
    fun nextMs(): Long {
        if (min >= max) {
            return min.toLong()
        }
        return (min + Random.nextInt(max - min + 1)).toLong()
    }
}

data class FieldConfig(
    val profile: SimProfile = SimProfile.LAB,
    val noAckOnUnblock: Boolean = true,
    val noAckOnBlock: Boolean = true,
    val mechanicalOpenDelayMs: IntRangeConfig = IntRangeConfig(800, 1500),
    val unsolicitedVolumeIntervalMs: IntRangeConfig = IntRangeConfig(400, 800),
    val concatFramesProbability: Double = 0.5,
    val dropResponseProbability: Double = 0.1,
    val interCharacterDelayMs: IntRangeConfig = IntRangeConfig(1, 2),
    val readChunkingMode: ReadChunkingMode = ReadChunkingMode.RANDOM
)
