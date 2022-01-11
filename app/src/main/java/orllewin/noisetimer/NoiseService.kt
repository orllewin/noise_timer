package orllewin.noisetimer

import android.animation.ValueAnimator
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.animation.doOnEnd
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import orllewin.noisetimer.audio.Noise
import java.text.SimpleDateFormat
import java.util.*

const val START_NOISE_SERVICE = 111
const val PAUSE_NOISE_SERVICE = 112
const val RESUME_NOISE_SERVICE = 113
const val STOP_NOISE_SERVICE = 114
const val SET_NOISE_SLEEP_TIMER = 115
const val CHANGE_RATE = 116

class NoiseService: Service() {

    private var generateJob: Job? = null
    private val noise = Noise()
    private var timerHandler: Handler? = null
    private lateinit var timerRunable: Runnable
    private var timerEndTimestamp = -1L

    private lateinit var notification: Notification
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var pauseAction: NotificationCompat.Action
    private lateinit var resumeAction: NotificationCompat.Action
    private lateinit var stopAction: NotificationCompat.Action

    private lateinit var  pauseIntent: PendingIntent
    private lateinit var  resumeIntent: PendingIntent
    private lateinit var  stopIntent: PendingIntent



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.run{
            val rate = getIntExtra("rate", 11025)
            val sleepTimerSeconds = getIntExtra("sleepTimerSeconds", -1)
            val ratePercentage = getIntExtra("ratePercentage", 50)
            when(getIntExtra("action", -1)){
                START_NOISE_SERVICE -> initialise(rate)
                PAUSE_NOISE_SERVICE -> pause()
                RESUME_NOISE_SERVICE -> resume()
                STOP_NOISE_SERVICE -> stop()
                SET_NOISE_SLEEP_TIMER -> setSleepTimer(sleepTimerSeconds)
                CHANGE_RATE -> setPlaybackRate(ratePercentage)
            }
        }
        //return super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun setPlaybackRate(ratePercentage: Int){
        val rate = kotlin.math.max(2000f, 22050f / 100 * (ratePercentage))
        println("setPlaybackrate: ratePercentage: $ratePercentage rate: $rate")
        noise.playbackRate(rate)
    }

    private fun setSleepTimer(sleepTimerSeconds: Int){
        println("setSleepTimer: sleepTimerSeconds: $sleepTimerSeconds")
        timerEndTimestamp = System.currentTimeMillis() + (sleepTimerSeconds * 1000)

        timerHandler = Handler(Looper.getMainLooper())

        timerRunable = Runnable{
            val fadeOutAnimator = ValueAnimator.ofFloat(noise.volume, 0f)
            fadeOutAnimator.duration = 8000
            fadeOutAnimator.addUpdateListener {
                val value = fadeOutAnimator.animatedValue as Float
                noise.setNoiseVolume(value)
            }
            fadeOutAnimator.doOnEnd {
                stop()
            }
            fadeOutAnimator.start()

        }

        timerHandler?.postDelayed(timerRunable, (sleepTimerSeconds * 1000).toLong())

        initialise(noise.playbackRate())
    }

    private fun pause(){
        println("NOISE: mute")
        noise.setNoiseVolume(0.0f)
        notification = notificationBuilder
            .clearActions()
            .addAction(resumeAction)
            .addAction(stopAction)
            .build()
        startForeground(1, notification)
    }

    private fun resume(){
        println("NOISE: unmute")
        noise.setNoiseVolume(0.5f)
        notification = notificationBuilder
            .clearActions()
            .addAction(pauseAction)
            .addAction(stopAction)
            .build()
        startForeground(1, notification)
    }

    private fun stop(){
        println("NOISE: stop")
        timerHandler?.removeCallbacks(timerRunable)
        noise.stop()
        generateJob?.cancel()
        notification
        stopSelf()
    }

    private fun initialise(rate: Int) {


        println("NOISE: initialise with rate: $rate")

        pauseIntent =
            PendingIntent.getService(
                this,
                PAUSE_NOISE_SERVICE,
                Intent(this, NoiseService::class.java).also { intent ->
                    intent.putExtra("action", PAUSE_NOISE_SERVICE)
                },
                PendingIntent.FLAG_IMMUTABLE
            )

        resumeIntent =
            PendingIntent.getService(
                this,
                RESUME_NOISE_SERVICE,
                Intent(this, NoiseService::class.java).also { intent ->
                    intent.putExtra("action", RESUME_NOISE_SERVICE)
                },
                PendingIntent.FLAG_IMMUTABLE
            )

        stopIntent =
            PendingIntent.getService(
                this,
                STOP_NOISE_SERVICE,
                Intent(this, NoiseService::class.java).also { intent ->
                    intent.putExtra("action", STOP_NOISE_SERVICE)
                },
                PendingIntent.FLAG_IMMUTABLE
            )


        val pendingIntent: PendingIntent = Intent(this, NoiseActivity::class.java).run {
            putExtra("rate", rate)//todo - this doesn't work
            PendingIntent.getActivity(applicationContext, 0, this, PendingIntent.FLAG_IMMUTABLE)
        }

        notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = createNotificationChannel("noise_service", "Noise Timer Channel")
            NotificationCompat.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            NotificationCompat.Builder(this)
        }

        pauseAction = NotificationCompat.Action.Builder(null, "Mute", pauseIntent).build()
        resumeAction = NotificationCompat.Action.Builder(null, "Unmute", resumeIntent).build()
        stopAction = NotificationCompat.Action.Builder(null, "Cancel", stopIntent).build()

        val message = if (timerEndTimestamp < 0){
            "Timer not set"
        }else{
            generateMessage(timerEndTimestamp)
        }

        notification = notificationBuilder
            .setContentTitle("Noise Timer")
            .setContentText(message)
            .setSmallIcon(R.drawable.vector_notification_icon)
            .setContentIntent(pendingIntent)
            .setTicker("Orllewin Noise Notification")
            .addAction(pauseAction)
            .addAction(stopAction)
            .build()

        startForeground(1076, notification)

        if(noise.alive) return

        generateJob = GlobalScope.launch {
            noise.start(true, rate)
        }
    }

    private fun generateMessage(timerEndTimestamp: Long): String {
        val date = Date(timerEndTimestamp)
        val formatter = SimpleDateFormat("HH:mm:ss")
        return "Playing noise until ${formatter.format(date)}"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        noise.stop()
        generateJob?.cancel()

        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.cancel(1076)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        chan.lightColor = Color.parseColor("#7251A8")
        chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
}