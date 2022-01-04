package orllewin.noisetimer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import orllewin.noisetimer.databinding.ActivityNoiseBinding

class NoiseActivity : AppCompatActivity() {

    lateinit var binding: ActivityNoiseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNoiseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startService(Intent(this, NoiseService::class.java).also {
            it.putExtra("action", START_NOISE_SERVICE)
            it.putExtra("rate", 1)
        })

        binding.timer30Seconds.setOnClickListener {
            setSleepTimer(30)
        }

        binding.rateSlider.addOnChangeListener { slider, value, fromUser ->
            startService(Intent(this, NoiseService::class.java).also {
                it.putExtra("action", CHANGE_RATE)
                it.putExtra("ratePercentage", value.toInt())
            })
        }
    }

    private fun setSleepTimer(seconds: Int){
        startService(Intent(this, NoiseService::class.java).also {
            it.putExtra("action", SET_NOISE_SLEEP_TIMER)
            it.putExtra("sleepTimerSeconds", seconds)
        })
    }
}