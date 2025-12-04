/*
 * Copyright 2022-2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.icl.demo

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.icl.demo.auth.LoginActivity
import com.icl.demo.databinding.ActivityMainBinding
import com.icl.demo.utils.FormatterClass

const val MAX_RESOURCE_COUNT = 20

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val activityViewModel: ActivityViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        val isLoggedIn = FormatterClass().getSharedPref("isLoggedIn", this)
        if (isLoggedIn == null || isLoggedIn != "true") {
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initActionBar()

        val navView: BottomNavigationView = binding.navView
        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration =
            AppBarConfiguration(
                setOf(
                    R.id.navigation_home,
                    R.id.nav_resources,
                    R.id.navigation_profile
                )
            )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Do something when Home is clicked
                    navController.navigate(R.id.navigation_home)
                    true
                }

                R.id.nav_resources -> {
                    // Do something when Dashboard is clicked
                    navController.navigate(R.id.nav_resources)
                    true
                }

                R.id.navigation_profile -> {
                    // Do something when Notifications is clicked
                    navController.navigate(R.id.navigation_profile)
                    true
                }

                else -> false
            }
        }
    }

    private fun initActionBar() {
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar.apply {
            title = ""
        }
    }
}
