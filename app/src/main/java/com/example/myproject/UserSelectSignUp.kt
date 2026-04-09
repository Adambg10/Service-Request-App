package com.example.myproject

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myproject.databinding.ActivityUserSelectSignUpBinding

class UserSelectSignUp : AppCompatActivity() {

    private lateinit var binding: ActivityUserSelectSignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSelectSignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnNormalUser.setOnClickListener {
            startActivity(Intent(this, NormalUserSignUp::class.java))
        }

        binding.btnProvider.setOnClickListener {
            startActivity(Intent(this, AffiliatedSignupActivity::class.java))
        }

        binding.linkSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}