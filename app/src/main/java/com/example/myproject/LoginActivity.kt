package com.example.myproject

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myproject.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textViewSignUp.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        binding.buttonLogin.setOnClickListener {
            val phone = binding.editTextPhone.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (phone.isEmpty() || password.isEmpty()) {
                android.widget.Toast.makeText(this, "Please enter phone and password", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = com.example.myproject.model.LoginRequest(phone, password)
            com.example.myproject.network.ApiClient.instance.login(request).enqueue(object : retrofit2.Callback<com.example.myproject.model.LoginResponse> {
                override fun onResponse(call: retrofit2.Call<com.example.myproject.model.LoginResponse>, response: retrofit2.Response<com.example.myproject.model.LoginResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val user = response.body()?.user
                        if (user != null) {
                            val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            editor.putString("user_id", user.id)
                            editor.putString("user_type", user.user_type)
                            editor.apply()
                        }

                        android.widget.Toast.makeText(this@LoginActivity, "Login Successful", android.widget.Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        android.widget.Toast.makeText(this@LoginActivity, response.body()?.message ?: "Login Failed", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<com.example.myproject.model.LoginResponse>, t: Throwable) {
                    android.widget.Toast.makeText(this@LoginActivity, "Error: ${t.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}