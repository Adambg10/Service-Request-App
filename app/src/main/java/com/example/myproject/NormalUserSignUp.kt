package com.example.myproject

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myproject.databinding.ActivityNormalUserSignUpBinding


class NormalUserSignUp : AppCompatActivity() {

    private lateinit var binding: ActivityNormalUserSignUpBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNormalUserSignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtAlreadyHaveAccount.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnSignup.setOnClickListener {
            val username = binding.editUsername.text.toString().trim()
            val phone = binding.editPhone.text.toString().trim()
            val password = binding.editPassword.text.toString().trim()

            if (username.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                android.widget.Toast.makeText(this, "Please fill all fields", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = com.example.myproject.model.SignupRequest(username, password, phone, "client")
            com.example.myproject.network.ApiClient.instance.signup(request).enqueue(object : retrofit2.Callback<com.example.myproject.model.SignupResponse> {
                override fun onResponse(call: retrofit2.Call<com.example.myproject.model.SignupResponse>, response: retrofit2.Response<com.example.myproject.model.SignupResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        android.widget.Toast.makeText(this@NormalUserSignUp, "Signup Successful", android.widget.Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@NormalUserSignUp, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        android.widget.Toast.makeText(this@NormalUserSignUp, response.body()?.message ?: "Signup Failed", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<com.example.myproject.model.SignupResponse>, t: Throwable) {
                    android.widget.Toast.makeText(this@NormalUserSignUp, "Error: ${t.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}