package com.example.myproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_profile, container, false)

        val btnLogout = root.findViewById<MaterialButton>(R.id.btnLogout)
        val tvUserName = root.findViewById<android.widget.TextView>(R.id.tvUserName)
        val tvUserPhone = root.findViewById<android.widget.TextView>(R.id.tvUserPhone)
        val rvReservationHistory = root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvReservationHistory)
        val tvEmptyHistory = root.findViewById<android.widget.TextView>(R.id.tvEmptyHistory)

        // Setup RecyclerView
        rvReservationHistory.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        val adapter = InterventionAdapter(emptyList())
        rvReservationHistory.adapter = adapter

        // Fetch user details
        val sharedPreferences = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("user_id", null)

        if (userId != null) {
            // Fetch User Info
            com.example.myproject.network.ApiClient.instance.getUserDetails(userId).enqueue(object : retrofit2.Callback<com.example.myproject.model.LoginResponse> {
                override fun onResponse(call: retrofit2.Call<com.example.myproject.model.LoginResponse>, response: retrofit2.Response<com.example.myproject.model.LoginResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val user = response.body()?.user
                        if (user != null) {
                            tvUserName.text = user.username
                            tvUserPhone.text = user.phone
                        }
                    }
                }

                override fun onFailure(call: retrofit2.Call<com.example.myproject.model.LoginResponse>, t: Throwable) {
                    // Handle failure silently
                }
            })

            // Fetch Interventions
            com.example.myproject.network.ApiClient.instance.getUserInterventions(userId).enqueue(object : retrofit2.Callback<com.example.myproject.model.UserInterventionsResponse> {
                override fun onResponse(call: retrofit2.Call<com.example.myproject.model.UserInterventionsResponse>, response: retrofit2.Response<com.example.myproject.model.UserInterventionsResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val interventions = response.body()?.interventions ?: emptyList()
                        if (interventions.isNotEmpty()) {
                            adapter.updateList(interventions)
                            tvEmptyHistory.visibility = View.GONE
                            rvReservationHistory.visibility = View.VISIBLE
                        } else {
                            tvEmptyHistory.visibility = View.VISIBLE
                            rvReservationHistory.visibility = View.GONE
                        }
                    }
                }

                override fun onFailure(call: retrofit2.Call<com.example.myproject.model.UserInterventionsResponse>, t: Throwable) {
                    // Handle failure silently
                }
            })
        }

        btnLogout.setOnClickListener {
            // Stop location tracking service if running
            try {
                com.example.myproject.service.LocationTrackingService.stopService(requireContext())
            } catch (e: Exception) {
                // Service might not be running
            }
            
            // Clear session
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()
            
            val intent = Intent(requireContext(), LoginActivity::class.java)
            // Clear the back stack so the user can't go back to the profile
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        return root
    }
}