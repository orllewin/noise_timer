package orllewin.noisetimer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.button.MaterialButton
import orllewin.noisetimer.databinding.ActivityNoiseBinding

class NoiseActivity : AppCompatActivity() {

    lateinit var binding: ActivityNoiseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNoiseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButton(binding.timer30Seconds, 30)
        setupButton(binding.timer15Minutes, 15 * 60)
        setupButton(binding.timer30Minutes, 30 * 60)
        setupButton(binding.timer45Minutes, 45 * 60)
        setupButton(binding.timer1Hour, 60 * 60)
        setupButton(binding.timer1AndAHalfHours, 90 * 60)
        setupButton(binding.timer2Hours, 120 * 60)
        setupButton(binding.timer3Hours, 180 * 60)
        setupButton(binding.timer4Hours, 240 * 60)

        binding.rateSlider.addOnChangeListener { slider, value, fromUser ->
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_about -> {
                //
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()

        startService(Intent(this, NoiseService::class.java).also {
            it.putExtra("action", START_NOISE_SERVICE)
            it.putExtra("rate", 11025)
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()

        startService(Intent(this, NoiseService::class.java).also {
            it.putExtra("action", STOP_NOISE_SERVICE)
        })
    }
}