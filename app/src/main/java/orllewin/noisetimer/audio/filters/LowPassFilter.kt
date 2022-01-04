package orllewin.noisetimer.audio.filters

import kotlin.math.tan


/**
 *
 * @author Orllewin
 *
 * Converted from C# implementation: https://www.musicdsp.org/en/latest/Filters/38-lp-and-hp-filter.html
 *
 */

// resonance amount, from 1.414 (sqrt(2)) to ~ 0.1
class LowPassFilter(
    private var frequency: Float,
    private val sampleRate: Int,
    private val resonance: Float) {

    private var c = 0f

    //BiQuad fields
    private var a1 = 0f
    private var a2 = 0f
    private var a3 = 0f
    private var b1 = 0f
    private var b2 = 0f

    //Previous values
    var inputH0: Float = 0f
    var inputH1: Float = 0f
    var outputH0: Float = 0f
    var outputH1: Float = 0f
    var outputH2: Float = 0f

    init {
        setFrequency(frequency)
    }

    fun getFrequency(): Float{
        return frequency
    }

    fun setFrequency(frequency: Float){
        if(frequency <= 0) return

        this.frequency = frequency
        c = 1.0f / tan(Math.PI * frequency / sampleRate).toFloat()
        a1 = 1.0f / (1.0f + resonance * c + c * c)
        a2 = 2f * a1
        a3 = a1
        b1 = 2.0f * (1.0f - c * c) * a1
        b2 = (1.0f - resonance * c + c * c) * a1
    }

    fun filter(input: Float): Float{
        val filtered = a1 * input + a2 * inputH0 + a3 * inputH1 - b1 * outputH0 - b2 * outputH1

        inputH1 = inputH0
        inputH0 = input

        outputH2 = outputH1
        outputH1 = outputH0
        outputH0 = filtered

        return filtered
    }
}