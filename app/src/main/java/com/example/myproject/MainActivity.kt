package com.example.myproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.myproject.databinding.ActivityMainBinding
import com.example.myproject.service.ServiceListFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved theme preference
        val sharedPreferences = getSharedPreferences("AppParams", android.content.Context.MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        if (isDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check user type
        val sessionPrefs = getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        val userType = sessionPrefs.getString("user_type", "client")

        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            var selectedFragment: Fragment? = null

            when (item.itemId) {
                R.id.navigation_home -> {
                    selectedFragment = if (userType == "provider") {
                        HomeProviderFragment()
                    } else {
                        HomeFragment()
                    }
                }
                R.id.navigation_services -> selectedFragment = ServiceListFragment()
                R.id.navigation_profile -> selectedFragment = ProfileFragment()
                R.id.navigation_settings -> selectedFragment = SettingsFragment()
            }

            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, selectedFragment).commit()
            }
            true
        }

        // Set the initial fragment
        if (savedInstanceState == null) {
            val initialFragment = if (userType == "provider") {
                HomeProviderFragment()
            } else {
                HomeFragment()
            }
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, initialFragment).commit()
        }


    }
}