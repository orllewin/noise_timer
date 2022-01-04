package orllewin.noisetimer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import orllewin.noisetimer.audio.filters.LowPassFilter
import orllewin.noisetimer.audio.filters.BasicLimiter
import orllewin.noisetimer.audio.filters.SmoothFilter
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class Noise {

    var volume = 1.0f
    private val samplingFrequency = 22050
    private val bufferSeconds = 5
    private val bufferFrames = samplingFrequency * bufferSeconds
    private var audioTrack: AudioTrack? = null

    private val noiseSource = NoiseSource.BrownNoise()
    private val lowPrecisionFilter = BasicLimiter()
    private val lowPassFilter = LowPassFilter(3500f, samplingFrequency, 1f)
    private val iirFilter = SmoothFilter(4f)

    private lateinit var walkHandler: Handler
    private lateinit var walkRunnable: Runnable

    var alive = false

    fun start(audioOn: Boolean, rate: Int = 11025){
        alive = true
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(samplingFrequency)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferFrames)
            .build()

        when {
            audioOn -> audioTrack?.setVolume(0.5f)
            else -> audioTrack?.setVolume(0.0f)
        }

        audioTrack?.playbackRate = rate//hz

        audioTrack?.play()

        initialisePerlinWalk()

        var audioBuffer: ShortArray
        var frame: Float

        while(alive){

            audioBuffer = ShortArray(bufferFrames)

            for (i in 0 until bufferFrames) {
                frame = noiseSource.nextValue().toFloat() * 100000
                frame = lowPrecisionFilter.filter(frame)
                frame = iirFilter.filter(frame)
                frame = lowPassFilter.filter(frame)
                audioBuffer[i] = frame.toInt().toShort()
            }

            //Audiotrack write is blocking so this loop only executes every bufferSeconds seconds
            audioTrack?.write(audioBuffer, 0, bufferFrames)
        }
    }

    fun setNoiseVolume(volume: Float){
        this.volume = volume
        audioTrack?.setVolume(volume)
    }

    fun volumeInc() {
        if(volume < 1.0f) volume += 0.1f
        audioTrack?.setVolume(volume)
    }

    fun volumeDec() {
        if(volume > 0.0f) volume -= 0.1f
        audioTrack?.setVolume(volume)
    }

    fun stop() {
        alive = false
        walkHandler.removeCallbacks(walkRunnable)
        audioTrack?.setVolume(0.0f)
        audioTrack?.pause()
        audioTrack?.flush()
        audioTrack?.stop()
    }

    fun playbackRate(playbackRate: Float) {
        audioTrack?.playbackRate = playbackRate.toInt()
    }

    fun playbackRate(): Int = audioTrack?.playbackRate ?: 1

    private fun initialisePerlinWalk(){
        var xWalk = Random.nextFloat()
        val yWalk = Random.nextFloat()
        walkHandler = Handler(Looper.getMainLooper())

        walkRunnable = Runnable{
            audioTrack?.playbackRate?.let {rate ->
                xWalk +=0.1f
                //Limit range to min/max offered by slider ui
                var nextRate = rate + (Perlin.noise(xWalk, yWalk) * 250).toInt()
                nextRate = max(2000, nextRate)
                nextRate = min(22050, nextRate)

                audioTrack?.playbackRate = nextRate
                walkHandler.postDelayed(walkRunnable, 250)
            }
        }

        walkHandler.post(walkRunnable)
    }
}