package orllewin.noisetimer.audio

import java.util.*

/**
 *
 * @author Orllewin
 *
 * Kotlin Derived from:
 *
 * PinkNoise.java  -  a pink noise generator
 *
 * Copyright (c) 2008, Sampo Niskanen <sampo.niskanen@iki.fi>
 * All rights reserved.
 * Source:  http://www.iki.fi/sampo.niskanen/PinkNoise/
 * @author Sampo Niskanen <sampo.niskanen@iki.fi>
 *
 */
open class NoiseSource(alpha: Float) {

    class WhiteNoise: NoiseSource(0f)
    class PinkNoise: NoiseSource(1f)
    class BrownNoise: NoiseSource(2f)

    private var poles = 10
    private val multipliers: DoubleArray

    private val values: DoubleArray
    private var random = Random()

    init {
        multipliers = DoubleArray(poles)
        values = DoubleArray(poles)

        var a = 1.0
        for (i in 0 until poles) {
            a = (i - alpha / 2) * a / (i + 1)
            multipliers[i] = a
        }

        //Prepopulate history
        for (i in 0 until 5 * poles) this.nextValue()
    }

    fun nextValue(): Double {
        var rnd = random.nextDouble() - 0.5
        for (i in 0 until poles) {
            rnd -= multipliers[i] * values[i]
        }
        System.arraycopy(values, 0, values, 1, values.size - 1)
        values[0] = rnd
        return rnd
    }
}