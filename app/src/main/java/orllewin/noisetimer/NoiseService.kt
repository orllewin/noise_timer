package orllewin.noisetimer

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import orllewin.noisetimer.audio.Noise

const val START_NOISE_SERVICE = 111
const val PAUSE_NOISE_SERVICE = 112
const val RESUME_NOISE_SERVICE = 113
const val STOP_NOISE_SERVICE = 114
const val SET_NOISE_SLEEP_TIMER = 115
const val CHANGE_RATE = 116

class NoiseService: Service() {

    private var generateJob: Job? = null
    private val noise = Noise()

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
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            stop()
        }, (sleepTimerSeconds * 1000).toLong())
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
        noise.stop()
        generateJob?.cancel()
        stopSelf()
    }

    private fun initialise(rate: Int) {

        if(noise.alive) return

        println("NOISE: initialise with rate: $rate")

        pauseIntent =
            PendingIntent.getService(this, PAUSE_NOISE_SERVICE, Intent(this, NoiseService::class.java).also { intent ->
                intent.putExtra("action", PAUSE_NOISE_SERVICE)
            }, PendingIntent.FLAG_IMMUTABLE)

        resumeIntent =
            PendingIntent.getService(this, RESUME_NOISE_SERVICE, Intent(this, NoiseService::class.java).also { intent ->
                intent.putExtra("action", RESUME_NOISE_SERVICE)
            }, PendingIntent.FLAG_IMMUTABLE)

        stopIntent =
            PendingIntent.getService(this, STOP_NOISE_SERVICE, Intent(this, NoiseService::class.java).also { intent ->
                intent.putExtra("action", STOP_NOISE_SERVICE)
            }, PendingIntent.FLAG_IMMUTABLE)


        val pendingIntent: PendingIntent = Intent(this, NoiseActivity::class.java).run {
            putExtra("rate", rate)//todo - this doesn't work
            PendingIntent.getActivity(applicationContext, 0, this, PendingIntent.FLAG_IMMUTABLE)
        }

        notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = createNotificationChannel("noise_service", "Noise ZZZZzzzz....")
            NotificationCompat.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            NotificationCompat.Builder(this)
        }

        pauseAction = NotificationCompat.Action.Builder(null, "Mute", pauseIntent).build()
        resumeAction = NotificationCompat.Action.Builder(null, "Unmute", resumeIntent).build()
        stopAction = NotificationCompat.Action.Builder(null, "Cancel", stopIntent).build()

        notification = notificationBuilder
            .setContentTitle("Noise Timer")
            .setSmallIcon(R.drawable.vector_notification_icon)
            .setContentIntent(pendingIntent)
            .setTicker("Orllewin Noise Notification")
            .addAction(pauseAction)
            .addAction(stopAction)
            .build()

        startForeground(1, notification)

        generateJob = GlobalScope.launch {
            noise.start(true, rate)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        noise.stop()
        generateJob?.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
}