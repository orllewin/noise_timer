package orllewin.noisetimer.audio.filters

/**
 *
 * Very quick and basic frequency limiter,
 * returns the least significant 16 bits of the frame
 *
 * @author Orllewin
 */
class BasicLimiter {

    fun filter(input: Float): Float{
        return input.toInt().toShort().toFloat()
    }
}



