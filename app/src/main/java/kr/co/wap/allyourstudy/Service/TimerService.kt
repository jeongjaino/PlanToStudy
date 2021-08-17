package kr.co.wap.allyourstudy.Service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.co.wap.allyourstudy.R
import kr.co.wap.allyourstudy.fragments.TimerFragment
import kr.co.wap.allyourstudy.model.TimerEvent
import kr.co.wap.allyourstudy.utils.*

class TimerService: LifecycleService() {

    companion object {
        val timerEvent = MutableLiveData<TimerEvent>()
        val timerInMillis = MutableLiveData<Long>()
        val timerInMin = MutableLiveData<Long>()
        val timerPomodoro = MutableLiveData<Long>()
        val cumulativeTimer = MutableLiveData<Long>()
    }

    private lateinit var notificationManager: NotificationManagerCompat
    private var isServiceStopped = false

    private var lapTime = 0L

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        //initValues
    }
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val any = when (it.action) {
                ACTION_TIMER_START -> {
                    Log.d("tag", "startService")
                    startForegroundService(it.action!!,it.getLongExtra("data",-1))
                }
                ACTION_TIMER_STOP -> {
                    Log.d("tag", "stopService")
                    stopService()
                }
                ACTION_DOWNTIMER_START ->{
                    startForegroundService(it.action!!,it.getLongExtra("data", -1))
                }
                ACTION_DOWNTIMER_STOP ->{
                    Log.d("tag", "stopService")
                    stopService()
                }
                ACTION_POMODORO_TIMER_START ->{
                    startForegroundService(it.action!!,it.getLongExtra("data", -1))
                }
                ACTION_POMODORO_TIMER_STOP ->{
                    stopService()
                }
                ACTION_TIMER_PAUSE ->{
                    pauseService()
                }
                ACTION_CUMULATIVE_TIMER_START ->{
                    startCumulativeTimer(it.getLongExtra("data",-1))
                }
                ACTION_POMODORO_REST_TIMER_START ->{
                    pomodoroRestTimer()
                }
                else -> {}
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initValues(){
        timerEvent.postValue(TimerEvent.END)
        timerInMillis.postValue(0L)
        timerInMin.postValue(0L)
        timerPomodoro.postValue(25*1000*60L)
    }
    private fun startForegroundService(action: String, data: Long) {
        timerEvent.postValue(TimerEvent.START)
        when (action) {
            ACTION_TIMER_START -> {
                startTimer(data)
            }
            ACTION_DOWNTIMER_START -> {
                startDownTimer(data)
            }
            ACTION_POMODORO_TIMER_START -> {
                pomodoroTimer(data)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        startForeground(NOTIFICATION_ID, getNotificationBuilder().build())

        timerInMillis.observe(this) {
            if (!isServiceStopped) {
                val builder = getNotificationBuilder().setContentText(
                    TimerUtil.getFormattedSecondTime(it, false)
                )
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        }
        timerInMin.observe(this) {
            if (!isServiceStopped) {
                val builder = getNotificationBuilder().setContentText(
                    TimerUtil.getFormattedSecondTime(it, true)
                )
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        }
        timerPomodoro.observe(this) {
            if (!isServiceStopped){
                val builder = getNotificationBuilder().setContentText(
                    TimerUtil.getFormattedSecondTime(it, true)
                )
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(){
        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )

        notificationManager.createNotificationChannel(channel)
    }

    private fun getNotificationBuilder() :NotificationCompat.Builder =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_baseline_access_alarm_24)
            .setContentTitle("AllYourStudy")
            .setContentText("00:00:00")
            .setContentIntent(getTimerFragmentPendingIntent())


    private fun getTimerFragmentPendingIntent() =
        PendingIntent.getActivity(
            this,
            420,
            Intent(this, TimerFragment::class.java).apply{
                this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    private fun stopService(){
        isServiceStopped = true
        initValues()
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(true)
        stopSelf()
    }
    @RequiresApi(Build.VERSION_CODES.N) //android API 24 higher foreground detach
    private fun pauseService(){
        isServiceStopped = true
        notificationManager.cancel(NOTIFICATION_ID)
        timerEvent.postValue(TimerEvent.END)
        stopForeground(Service.STOP_FOREGROUND_DETACH)
        stopSelf()
    }
    private fun startTimer(data: Long){
        val timeStarted = System.currentTimeMillis() - data * 1000  //(data,second) (millis = second *1000)
        CoroutineScope(Dispatchers.Main).launch{
            while(!isServiceStopped && timerEvent.value!! == TimerEvent.START){
                lapTime = System.currentTimeMillis() - timeStarted
                timerInMillis.postValue(lapTime)
                delay(1000L)
            }
        }
    }
    private fun startDownTimer(data: Long){
        var starting = data * 1000 + 50 // 50이 수가 전달되면서 소실되는 값
        CoroutineScope(Dispatchers.Main).launch {
            object : CountDownTimer(starting, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    if(!isServiceStopped && timerEvent.value!! == TimerEvent.START) {
                        starting = millisUntilFinished
                        Log.d("downTimer",starting.toString())
                        timerInMin.postValue(starting)
                    }
                }
                override fun onFinish() {
                    stopService()
                }
            }.start()
        }
    }
    private fun pomodoroTimer(data: Long){
        var starting: Long = data * 1000 + 50
        CoroutineScope(Dispatchers.Main).launch {
            object  : CountDownTimer(starting, 1000){
                override fun onTick(millisUntilFinished: Long) {
                    if(!isServiceStopped && timerEvent.value!! == TimerEvent.START) {
                        starting = millisUntilFinished
                        Log.d("pomodoro",starting.toString())
                        timerPomodoro.postValue(starting)
                    }
                }
                override fun onFinish() {
                    timerEvent.postValue(TimerEvent.POMODORO_END)
                    isServiceStopped = true
                    timerPomodoro.postValue(5*1000*60L)
                    notificationManager.cancel(NOTIFICATION_ID)
                    stopForeground(true)
                    stopSelf()
                }
            }.start()
        }
    }
    private fun pomodoroRestTimer(){
        var starting: Long = 1000 * 60 * 5 + 50
        CoroutineScope(Dispatchers.Main).launch {
            object : CountDownTimer(starting, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    if(!isServiceStopped && timerEvent.value!! == TimerEvent.START) {
                        starting = millisUntilFinished
                        timerPomodoro.postValue(starting)
                    }
                }
                override fun onFinish() {
                    stopService()
                }
            }.start()
        }
    }
    private fun startCumulativeTimer(data: Long){
        val timeStarted = System.currentTimeMillis() - data * 1000  //(data,second) (millis = second *1000)
        CoroutineScope(Dispatchers.Main).launch{
            while(!isServiceStopped && timerEvent.value!! == TimerEvent.START){
                lapTime = System.currentTimeMillis() - timeStarted
                cumulativeTimer.postValue(lapTime)
                delay(1000L)
            }
        }
    }
}