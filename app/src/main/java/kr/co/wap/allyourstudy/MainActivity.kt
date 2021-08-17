package kr.co.wap.allyourstudy

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kr.co.wap.allyourstudy.Service.TimerService
import kr.co.wap.allyourstudy.databinding.ActivityMainBinding
import kr.co.wap.allyourstudy.fragments.HomeFragment
import kr.co.wap.allyourstudy.fragments.YoutubeFragment
import kr.co.wap.allyourstudy.model.TimerEvent
import kr.co.wap.allyourstudy.utils.TimerUtil
import java.util.*


class MainActivity : AppCompatActivity() {

    val binding by lazy{ActivityMainBinding.inflate(layoutInflater)}

    private val youtubeFragment = YoutubeFragment()
    private val homeFragment = HomeFragment()

    private lateinit var currentTime: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setObservers()
    }
    override fun onStart(){
        super.onStart()
        binding.bottomBar.visibility = View.VISIBLE
        initNavBar(binding.bottomBar)
    }
    private fun initNavBar(navbar: BottomNavigationView){
        navbar.run{
            setOnItemSelectedListener {
                when(it.itemId){
                    R.id.pomodoroTimer -> { goTimer() }
                    R.id.Youtube -> { goYouTube() }
                    R.id.Home -> { goHome() }
                }
                true
            }
            selectedItemId = R.id.Home //기본 홈 설정
        }
    }
    private fun setObservers(){
        TimerService.timerEvent.observe(this){
            if(it == TimerEvent.END){
                binding.cardView.visibility = View.GONE
            }
            else{
                binding.cardView.visibility = View.VISIBLE
            }
        }
        TimerService.timerInMillis.observe(this) {
            currentTime = TimerUtil.getFormattedSecondTime(it, false)
            binding.shareTimer.text = currentTime //timer
        }
        TimerService.timerInMin.observe(this){
            currentTime = TimerUtil.getFormattedSecondTime(it, true)
            binding.shareTimer.text = currentTime
        }
        TimerService.timerPomodoro.observe(this){
            currentTime = TimerUtil.getFormattedSecondTime(it, true)
            binding.shareTimer.text = currentTime
        }
    }
    private fun replaceFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer , fragment)
        transaction.commit()
    }
    fun goTimer(){
        val intent = Intent(this, TimerActivity::class.java)
        startActivity(intent)
        binding.cardView.visibility = View.GONE
        binding.bottomBar.visibility = View.GONE
    }
    private fun goYouTube(){
        replaceFragment(youtubeFragment)
    }
    private fun goHome(){
        replaceFragment(homeFragment)
    }
}