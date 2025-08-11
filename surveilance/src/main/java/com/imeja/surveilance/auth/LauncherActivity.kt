package com.imeja.surveilance.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.imeja.surveilance.MainActivity
import com.imeja.surveilance.R
import com.imeja.surveilance.databinding.ActivityLauncherBinding
import com.imeja.surveilance.helpers.FormatterClass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        lifecycleScope.launch {
            delay(3000) // 3 seconds
            val loggedIn = FormatterClass().getSharedPref("isLoggedIn", this@LauncherActivity)
            if (loggedIn != null) {
                val intent = Intent(this@LauncherActivity, MainActivity::class.java)
                startActivity(intent)
                this@LauncherActivity.finish()
            } else {
                binding.getStartedButton.visibility = View.VISIBLE
            }
        }
        binding.apply {
            getStartedButton.apply {
                setOnClickListener {
                    val intent = Intent(this@LauncherActivity, LoginActivity::class.java)
                    startActivity(intent)
                    this@LauncherActivity.finish()
                }
            }
        }
    }
}