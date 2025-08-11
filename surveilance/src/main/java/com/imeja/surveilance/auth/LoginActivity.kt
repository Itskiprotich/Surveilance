package com.imeja.surveilance.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.imeja.surveilance.R
import com.imeja.surveilance.databinding.ActivityLoginBinding
import com.imeja.surveilance.models.DbSignIn
import com.imeja.surveilance.network.RetrofitCallsAuthentication

class  LoginActivity : AppCompatActivity() {

    private var retrofitCallsAuthentication = RetrofitCallsAuthentication()
    private lateinit var binding: ActivityLoginBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.apply {
            btnLogin.setOnClickListener {
                val email = etEmail.text.toString()
                val password = etPassword.text.toString()

                if (email.isEmpty()) {
                    binding.emailLayout.error = "Please enter username"
                    return@setOnClickListener
                }
                // check password
                if (password.isEmpty()) {
                    binding.passwordLayout.error = "Please enter password"
                    return@setOnClickListener
                }

                val dbSignIn = DbSignIn(idNumber = email, password = password, "Facility")
                retrofitCallsAuthentication.loginUser(this@LoginActivity, dbSignIn)

            }
        }
    }
}