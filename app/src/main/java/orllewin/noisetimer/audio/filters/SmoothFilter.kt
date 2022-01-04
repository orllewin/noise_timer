package orllewin.noisetimer.audio.filters

/**
 *
 * Basic IIR Filter: https://en.wikipedia.org/wiki/Infinite_impulse_response
 * @author Orllewin
 *
 * Adapted from http://phrogz.net/js/framerate-independent-low-pass-filter.html
 *
 * @param amount: 1f returns original value, 2f returns half the distance between frames ...
 *
 */
class SmoothFilter(private val amount: Float) {

    var filtered: Float = 0f

    fun filter(input: Float): Float{
        filtered += ((input - filtered) / amount)
        return filtered
    }
}