package orllewin.noisetimer

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.button.MaterialButton
import orllewin.noisetimer.databinding.ActivityNoiseBinding
import android.content.ComponentName
import com.google.android.material.slider.LabelFormatter

class NoiseActivity : AppCompatActivity() {

    lateinit var binding: ActivityNoiseBinding

    var serviceComponentName: ComponentName? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNoiseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setupButton(binding.timer5Minutes, 15)
        setupButton(binding.timer5Minutes, 5 * 60)
        setupButton(binding.timer10Minutes, 10 * 60)
        setupButton(binding.timer20Minutes, 20 * 60)
        setupButton(binding.timer30Minutes, 30 * 60)
        setupButton(binding.timer45Minutes, 45 * 60)
        setupButton(binding.timer1Hour, 60 * 60)
        setupButton(binding.timer1AndAHalfHours, 90 * 60)
        setupButton(binding.timer2Hours, 120 * 60)
        setupButton(binding.timer3Hours, 180 * 60)

        binding.rateSlider.setLabelFormatter(null)
        binding.rateSlider.labelBehavior = LabelFormatter.LABEL_GONE
        binding.rateSlider.addOnChangeListener { _, value, _ ->
            startService(Intent(this, NoiseService::class.java).also {
                it.putExtra("action", CHANGE_RATE)
                it.putExtra("ratePercentage", value.toInt())
            })
        }
    }

    private fun setupButton(button: MaterialButton, seconds: Int){
        button.setOnClickListener {
            setSleepTimer(seconds)
            finish()
        }
    }

    private fun setSleepTimer(seconds: Int){
        startService(Intent(this, NoiseService::class.java).also {
            it.putExtra("action", SET_NOISE_SLEEP_TIMER)
            it.putExtra("sleepTimerSeconds", seconds)
        })
    }

    override fun onResume() {
        super.onResume()

        if(serviceComponentName == null) {
            serviceComponentName = startService(Intent(this, NoiseService::class.java).also {
                it.putExtra("action", START_NOISE_SERVICE)
                it.putExtra("rate", 11025)
            })
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        startService(Intent(this, NoiseService::class.java).also {
            it.putExtra("action", STOP_NOISE_SERVICE)
        })
    }
}