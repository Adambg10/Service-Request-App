package com.example.myproject

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myproject.databinding.ActivityAffiliatedSignupBinding

class AffiliatedSignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAffiliatedSignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAffiliatedSignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSignupAffiliated.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}