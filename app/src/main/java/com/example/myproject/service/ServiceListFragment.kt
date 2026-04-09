package com.example.myproject.service

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.R
import com.example.myproject.databinding.FragmentServiceListBinding
import android.text.Editable
import android.text.TextWatcher

class ServiceListFragment : Fragment() {

    private var _binding: FragmentServiceListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ServiceAdapter
    private var allProviders = emptyList<com.example.myproject.model.ServiceProvider>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentServiceListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterList(adapter)
            }
        })

        // Filter Listener
        binding.chipGroupFilters.setOnCheckedChangeListener { group, checkedId ->
            filterList(adapter)
        }

        com.example.myproject.network.ApiClient.instance.getServiceProviders().enqueue(object : retrofit2.Callback<com.example.myproject.model.ServiceProvidersResponse> {
            override fun onResponse(call: retrofit2.Call<com.example.myproject.model.ServiceProvidersResponse>, response: retrofit2.Response<com.example.myproject.model.ServiceProvidersResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    allProviders = response.body()?.providers ?: emptyList()
                    filterList(adapter)
                } else {
                    Toast.makeText(requireContext(), "Failed to load services", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<com.example.myproject.model.ServiceProvidersResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupRecycler() {
        adapter = ServiceAdapter(emptyList()) { provider ->
            showBookingDialog(provider)
        }
        binding.recyclerServices.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerServices.adapter = adapter
    }

    private fun filterList(adapter: ServiceAdapter) {
        val searchText = binding.editSearch.text.toString().trim().lowercase()
        val checkedChipId = binding.chipGroupFilters.checkedChipId
        
        val filteredList = allProviders.filter { provider ->
            val matchesSearch = provider.username.lowercase().contains(searchText) ||
                                provider.profession.lowercase().contains(searchText) ||
                                provider.description.lowercase().contains(searchText)

            val matchesCategory = if (checkedChipId != View.NO_ID) {
                val chip = binding.root.findViewById<com.google.android.material.chip.Chip>(checkedChipId)
                val category = chip.text.toString()
                provider.profession.equals(category, ignoreCase = true)
            } else {
                true
            }

            matchesSearch && matchesCategory
        }

        adapter.updateList(filteredList)
    }

    private fun showBookingDialog(provider: com.example.myproject.model.ServiceProvider) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_book_intervention, null)
        val editDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editDescription)
        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_confirm)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)
        val textTitle = dialogView.findViewById<android.widget.TextView>(R.id.text_title)
        
        textTitle.text = "Intervention avec ${provider.username}"
        
        val dialog = android.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        
        // Apply transparent background to show card styling
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnConfirm.setOnClickListener {
            val description = editDescription.text.toString()
            if (description.isNotEmpty()) {
                // Check location permission
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireActivity())
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            val scheduledTime = sdf.format(java.util.Date())
                            // Update client location in users table first
                            updateClientLocation(location.latitude, location.longitude)
                            bookIntervention(provider.id, scheduledTime, description, location.latitude, location.longitude)
                            dialog.dismiss()
                        } else {
                            Toast.makeText(context, "Impossible de récupérer votre localisation", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Permission de localisation requise", Toast.LENGTH_SHORT).show()
                    // Optionally request permission here
                }
            } else {
                Toast.makeText(context, "Veuillez décrire le problème", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun bookIntervention(providerId: String, scheduledTime: String, description: String, latitude: Double, longitude: Double) {
        val sharedPreferences = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        val clientId = sharedPreferences.getString("user_id", null)

        if (clientId != null) {
            val request = com.example.myproject.model.InterventionRequest(clientId, providerId, scheduledTime, description, latitude, longitude)
            com.example.myproject.network.ApiClient.instance.createIntervention(request).enqueue(object : retrofit2.Callback<com.example.myproject.model.InterventionResponse> {
                override fun onResponse(call: retrofit2.Call<com.example.myproject.model.InterventionResponse>, response: retrofit2.Response<com.example.myproject.model.InterventionResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(requireContext(), "Demande envoyée avec succès!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "Erreur: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<com.example.myproject.model.InterventionResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Erreur réseau: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(requireContext(), "Erreur: Utilisateur non connecté", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateClientLocation(latitude: Double, longitude: Double) {
        val sharedPreferences = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("user_id", null)

        if (userId != null) {
            val request = com.example.myproject.model.LocationUpdateRequest(userId, latitude, longitude)
            com.example.myproject.network.ApiClient.instance.updateClientLocation(request).enqueue(object : retrofit2.Callback<com.example.myproject.model.LocationUpdateResponse> {
                override fun onResponse(call: retrofit2.Call<com.example.myproject.model.LocationUpdateResponse>, response: retrofit2.Response<com.example.myproject.model.LocationUpdateResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        // Location updated silently
                    } else {
                        Toast.makeText(requireContext(), "Erreur mise à jour localisation", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<com.example.myproject.model.LocationUpdateResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Erreur réseau: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
