package com.example.myproject

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.example.myproject.network.ApiClient
import com.example.myproject.model.ServiceProvidersResponse
import com.example.myproject.model.ServiceProvider
import com.example.myproject.model.ActiveIntervention
import com.example.myproject.model.ActiveInterventionResponse
import com.example.myproject.model.MarkArrivedRequest
import com.example.myproject.model.GenericResponse
import com.example.myproject.model.SubmitReviewRequest
import com.example.myproject.model.InterventionRequest
import com.example.myproject.model.InterventionResponse
import com.example.myproject.model.LocationUpdateRequest
import com.example.myproject.model.LocationUpdateResponse
import com.example.myproject.adapter.ProviderHomeAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.location.Location
import android.app.AlertDialog
import android.widget.EditText
import android.widget.RatingBar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null
    private var allProviders: List<ServiceProvider> = emptyList()
    private lateinit var providerAdapter: ProviderHomeAdapter
    
    // UI Elements
    private lateinit var menuContainer: ConstraintLayout
    private lateinit var layoutCategories: ConstraintLayout
    private lateinit var layoutProviderList: ConstraintLayout
    private lateinit var btnToggle: MaterialButton
    private lateinit var btnBackMenu: ImageButton
    private lateinit var recyclerView: RecyclerView

    // Tracking UI Elements
    private lateinit var trackingBanner: MaterialCardView
    private lateinit var textProviderStatus: TextView
    private lateinit var textProviderDistance: TextView
    private lateinit var textProviderProfession: TextView

    // Tracking state
    private var isTrackingMode = false
    private var isArrived = false
    private var activeIntervention: ActiveIntervention? = null
    private var providerMarker: Marker? = null
    private val trackingHandler = Handler(Looper.getMainLooper())
    private val trackingRunnable = object : Runnable {
        override fun run() {
            checkActiveIntervention()
            trackingHandler.postDelayed(this, 5000) // Poll every 5 seconds
        }
    }

    // Providers polling (when not in tracking mode)
    private val providersPollingHandler = Handler(Looper.getMainLooper())
    private val providersPollingRunnable = object : Runnable {
        override fun run() {
            if (!isTrackingMode) {
                fetchProviders()
            }
            providersPollingHandler.postDelayed(this, 5000) // Poll every 5 seconds
        }
    }

    private var isMenuOpen = true
    private var isListMode = false 
    private var selectedCategory: String? = null 

    // Define required permissions
    private val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        trackingHandler.removeCallbacks(trackingRunnable)
        providersPollingHandler.removeCallbacks(providersPollingRunnable)
        providerMarker = null 
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        // Initialize map dynamically
        var mapFragment = childFragmentManager.findFragmentById(R.id.map_view) as? SupportMapFragment
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.map_view, mapFragment)
                .commit()
        }
        mapFragment?.getMapAsync { googleMap ->
            onMapReady(googleMap)
        }

        menuContainer = view.findViewById(R.id.menu_container)
        layoutCategories = view.findViewById(R.id.layout_categories)
        layoutProviderList = view.findViewById(R.id.layout_provider_list)
        btnToggle = view.findViewById(R.id.btn_toggle_menu)
        btnBackMenu = view.findViewById(R.id.btn_back_menu)
        recyclerView = view.findViewById(R.id.recycler_view_providers)

        recyclerView.layoutManager = LinearLayoutManager(context)
        providerAdapter = ProviderHomeAdapter(
            emptyList(),
            onProviderClick = { provider ->
                // Handle provider click (e.g., zoom to marker or open details)
                val location = LatLng(provider.latitude, provider.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            },
            onDemanderClick = { provider ->
                // Handle demand button click - create intervention
                createIntervention(provider)
            }
        )
        recyclerView.adapter = providerAdapter

        trackingBanner = view.findViewById(R.id.tracking_banner)
        textProviderStatus = view.findViewById(R.id.text_provider_status)
        textProviderDistance = view.findViewById(R.id.text_provider_distance)
        textProviderProfession = view.findViewById(R.id.text_provider_profession)

        view.findViewById<MaterialButton>(R.id.btnElectricien).setOnClickListener { onCategorySelected("Electricien") }
        view.findViewById<MaterialButton>(R.id.btnPlombier).setOnClickListener { onCategorySelected("Plombier") }
        view.findViewById<MaterialButton>(R.id.btnClim).setOnClickListener { onCategorySelected("Climatisation") }
        view.findViewById<MaterialButton>(R.id.btnMecanicien).setOnClickListener { onCategorySelected("Mecanicien") }

        btnBackMenu.setOnClickListener {
            onBackToCategories()
        }

        btnToggle.setOnClickListener {
            toggleMenu()
        }
        fetchProviders()
        
        // Start checking for active intervention
        checkActiveIntervention()
    }

    override fun onResume() {
        super.onResume()
        trackingHandler.post(trackingRunnable)
        providersPollingHandler.post(providersPollingRunnable)
    }

    override fun onPause() {
        super.onPause()
        trackingHandler.removeCallbacks(trackingRunnable)
        providersPollingHandler.removeCallbacks(providersPollingRunnable)
    }

    private fun checkActiveIntervention() {
        val sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("user_id", null) ?: return
        val userType = sharedPreferences.getString("user_type", "client")

        if (userType != "client") return


        ApiClient.instance.getActiveIntervention(userId).enqueue(object : Callback<ActiveInterventionResponse> {
            override fun onResponse(
                call: Call<ActiveInterventionResponse>,
                response: Response<ActiveInterventionResponse>
            ) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val body = response.body()!!
                    if (body.has_active && body.intervention != null) {
                        activeIntervention = body.intervention
                        enterTrackingMode(body.intervention)
                    } else {
                        if (isTrackingMode && activeIntervention != null) {
                            val completedIntervention = activeIntervention!!
                            exitTrackingMode()
                            showRatingDialog(completedIntervention)
                        } else if (isTrackingMode) {
                            exitTrackingMode()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ActiveInterventionResponse>, t: Throwable) {
                
            }
        })
    }

    private fun showRatingDialog(intervention: ActiveIntervention) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rating, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val editReview = dialogView.findViewById<EditText>(R.id.editReview)
        val textProviderName = dialogView.findViewById<TextView>(R.id.textProviderName)
        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_confirm)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)
        
        textProviderName.text = "Comment était le service de ${intervention.provider_name}?"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnConfirm.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val review = editReview.text.toString().trim()
            submitReview(intervention.intervention_id.toInt(), rating, review)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun submitReview(interventionId: Int, rating: Int, review: String) {
        val sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val clientId = sharedPreferences.getString("user_id", null)?.toInt() ?: return

        val request = SubmitReviewRequest(interventionId, clientId, rating, review)
        ApiClient.instance.submitReview(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(context, "Merci pour votre évaluation!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        response.body()?.message ?: "Erreur serveur",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(context, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun enterTrackingMode(intervention: ActiveIntervention) {
        isTrackingMode = true
        trackingBanner.visibility = View.VISIBLE
        
        if (intervention.status.equals("arrive") ) {
            isArrived = true
            trackingBanner.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark))
                textProviderStatus.text = "${intervention.provider_name} est arrivé"
            textProviderDistance.visibility = View.GONE
            textProviderProfession.text = intervention.provider_profession
            
            
        } else {
            val arrived = checkIfArrived(intervention)

            if (arrived && !isArrived) {
                isArrived = true
                markProviderAsArrived(intervention.intervention_id)
                
                trackingBanner.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark))
                textProviderStatus.text = "${intervention.provider_name} est arrivé"
                textProviderDistance.visibility = View.GONE
                textProviderProfession.text = intervention.provider_profession
                
               
            } else if (!arrived) {
                isArrived = false
                trackingBanner.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                textProviderStatus.text = "${intervention.provider_name} est en route"
                textProviderDistance.visibility = View.VISIBLE
                textProviderProfession.text = intervention.provider_profession
                
                updateDistance(intervention)
            }
        }

        // Hide menu
        menuContainer.visibility = View.GONE
        btnToggle.visibility = View.GONE

        // Update provider marker on map
        updateProviderMarker(intervention)
    }

    private fun markProviderAsArrived(interventionId: String) {
        val request = MarkArrivedRequest(interventionId)
        ApiClient.instance.markProviderArrived(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(context, "Le prestataire est arrivé!", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                // Silent fail - UI already updated
            }
        })
    }

    private fun checkIfArrived(intervention: ActiveIntervention): Boolean {
        val providerLat = intervention.provider_latitude
        val providerLng = intervention.provider_longitude

        if (providerLat != null && providerLng != null && userLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation!!.latitude, userLocation!!.longitude,
                providerLat, providerLng,
                results
            )
            val distanceInMeters = results[0]
            return distanceInMeters <= 10f // Within 10 meters
        }
        return false
    }

    private fun exitTrackingMode() {
        isTrackingMode = false
        isArrived = false
        activeIntervention = null

        // Hide tracking banner and reset color
        trackingBanner.visibility = View.GONE
        trackingBanner.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
        textProviderDistance.visibility = View.VISIBLE

        // Show menu
        menuContainer.visibility = View.VISIBLE
        btnToggle.visibility = View.VISIBLE

        // Clear provider marker and show all providers
        providerMarker?.remove()
        providerMarker = null
        displayMarkers(allProviders)
    }

    private fun updateProviderMarker(intervention: ActiveIntervention) {
        if (!::map.isInitialized) return

        val providerLat = intervention.provider_latitude
        val providerLng = intervention.provider_longitude

        if (providerLat != null && providerLng != null) {
            val providerLocation = LatLng(providerLat, providerLng)

            // 1. If we haven't created the marker yet, create it
            if (providerMarker == null) {
                map.clear() // Clear user markers/other junk ONLY the first time
                val iconResId = getEnRouteMarkerIconResId(intervention.provider_profession)
                val icon = bitmapDescriptorFromVector(requireContext(), iconResId)

                providerMarker = map.addMarker(
                    MarkerOptions()
                        .position(providerLocation)
                        .title(intervention.provider_name)
                        .snippet(intervention.provider_profession)
                        .icon(icon)
                )
                // Initial Zoom logic
                if (userLocation != null) {
                    val userLatLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)
                    val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                        .include(userLatLng)
                        .include(providerLocation)
                        .build()
                    // Use a safer padding for bounds to avoid crashing on small screens
                    try {
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                    } catch (e: Exception) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(providerLocation, 14f))
                    }
                }
            } else {
                // 2. If marker exists, just UPDATE position (No flickering!)
                providerMarker?.position = providerLocation
                // Optional: Animate camera to follow him? 
                // map.animateCamera(CameraUpdateFactory.newLatLng(providerLocation)) 
            }
        }
    }

    private fun updateDistance(intervention: ActiveIntervention) {
        val providerLat = intervention.provider_latitude
        val providerLng = intervention.provider_longitude

        if (providerLat != null && providerLng != null && userLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation!!.latitude, userLocation!!.longitude,
                providerLat, providerLng,
                results
            )
            val distanceInMeters = results[0]

            val distanceText = if (distanceInMeters >= 1000) {
                String.format("%.1f km", distanceInMeters / 1000)
            } else {
                String.format("%.0f m", distanceInMeters)
            }
            textProviderDistance.text = "Distance: $distanceText"
        } else {
            textProviderDistance.text = "Localisation du prestataire en cours..."
        }
    }

    private fun onCategorySelected(profession: String) {
        selectedCategory = profession
        
        val filteredList = filterMarkers(profession)

        
        providerAdapter.updateList(filteredList, userLocation)

        // 3. Switch View
        layoutCategories.visibility = View.GONE
        layoutProviderList.visibility = View.VISIBLE
        isListMode = true

        // 4. Expand Menu
        expandMenu()
    }

    private fun onBackToCategories() {
        selectedCategory = null
        
        displayMarkers(allProviders)

        
        layoutProviderList.visibility = View.GONE
        layoutCategories.visibility = View.VISIBLE
        isListMode = false

        
        collapseMenuToNormal()
    }

    private fun expandMenu() {
        val screenHeight = resources.displayMetrics.heightPixels
        val targetHeight = (screenHeight * 0.35).toInt() // 35% of screen height

        // Animate height change
        val params = menuContainer.layoutParams
        
        params.height = targetHeight
        menuContainer.layoutParams = params
        menuContainer.requestLayout()
        
        // Ensure toggle button follows
        updateToggleButtonPosition()
    }

    private fun collapseMenuToNormal() {
        val params = menuContainer.layoutParams
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        menuContainer.layoutParams = params
        menuContainer.requestLayout()
        menuContainer.post { updateToggleButtonPosition() }
    }

    private fun toggleMenu() {
        val contentHeight = menuContainer.height.toFloat()
        val buttonHeight = btnToggle.height.toFloat()
        val paddingBottom = 16 * resources.displayMetrics.density
        
        val targetTranslationY = if (isMenuOpen) contentHeight else 0f
        val targetButtonTranslationY = if (isMenuOpen) {
            // Closing: Button goes to bottom of screen
            contentHeight - paddingBottom - (buttonHeight / 2)
        } else {
            // Opening: Button goes to top of menu (0 relative to start)
            0f
        }

        menuContainer.animate()
            .translationY(targetTranslationY)
            .setDuration(300)
            .start()

        btnToggle.animate()
            .translationY(targetButtonTranslationY)
            .setDuration(300)
            .withEndAction {
                btnToggle.setIconResource(if (isMenuOpen) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up)
            }
            .start()

        isMenuOpen = !isMenuOpen
    }

    private fun updateToggleButtonPosition() {
        // Reset translation when mode changes to ensure it sits correctly on top
        if (isMenuOpen) {
            btnToggle.animate().translationY(0f).setDuration(200).start()
        } else {
            // If menu is closed and we change mode (unlikely but possible), keep it closed
            val contentHeight = menuContainer.height.toFloat()
            val buttonHeight = btnToggle.height.toFloat()
            val paddingBottom = 16 * resources.displayMetrics.density
            val targetY = contentHeight - paddingBottom - (buttonHeight / 2)
            btnToggle.animate().translationY(targetY).setDuration(200).start()
        }
    }

    private fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        
        checkLocationPermissions()
        val tunisia = LatLng(36.8065, 10.1815)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(tunisia, 10f))

        if (isTrackingMode && activeIntervention != null) {
            updateProviderMarker(activeIntervention!!)
        } else {
            if (allProviders.isNotEmpty()) {
                displayMarkers(allProviders)
            }
        }
    }

    private fun getMarkerIconResId(profession: String): Int {
        return when (profession.lowercase()) {
            "électricien", "electricien" -> R.drawable.marker_electrician
            "plombier" -> R.drawable.marker_plumber
            "climatisation" -> R.drawable.marker_clim
            "mécanicien", "mecanicien" -> R.drawable.marker_mechanic
            else -> R.drawable.marker_electrician // Default
        }
    }

    private fun getEnRouteMarkerIconResId(profession: String): Int {
        return when (profession.lowercase()) {
            "électricien", "electricien" -> R.drawable.marker_electrician_enroute
            "plombier" -> R.drawable.marker_plumber_enroute
            "climatisation" -> R.drawable.marker_clim_enroute
            "mécanicien", "mecanicien" -> R.drawable.marker_mechanic_enroute
            else -> R.drawable.marker_electrician_enroute // Default
        }
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation()
        } else {
            requestPermissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            map.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = location

                    
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    if (!isTrackingMode) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    }
                    if (allProviders.isNotEmpty()) {
                        providerAdapter.updateList(allProviders, userLocation)
                    }
                }
            }
        }
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
 
        if (fineLocationGranted || coarseLocationGranted) {
            enableUserLocation()
        }
    }
    
    private fun filterMarkers(profession: String): List<ServiceProvider> {
        // Filter the list based on the profession (ignoring case)
        val filteredList = allProviders.filter {
            it.profession.equals(profession, ignoreCase = true)
        }

        // Update the map to show only these markers
        displayMarkers(filteredList)

        return filteredList
    }

    
    private fun displayMarkers(providersToShow: List<ServiceProvider>) {
        if (!::map.isInitialized) return

        map.clear() // Clear old markers

        providersToShow.forEach { provider ->
            val location = LatLng(provider.latitude, provider.longitude)

            // Get the correct icon
            val iconResId = getMarkerIconResId(provider.profession)
            val icon = bitmapDescriptorFromVector(requireContext(), iconResId)

            map.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(provider.profession)
                    .snippet("${provider.username}: ${provider.description}")
                    .icon(icon)
            )
        }
    }

    
    private fun fetchProviders() {
        
        ApiClient.instance.getServiceProviders().enqueue(object : Callback<ServiceProvidersResponse> {
            override fun onResponse(
                call: Call<ServiceProvidersResponse>,
                response: Response<ServiceProvidersResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    allProviders = response.body()!!.providers
                    
                    if (selectedCategory != null) {
                        // Apply filter if a category is selected
                        val filteredList = filterMarkers(selectedCategory!!)
                        providerAdapter.updateList(filteredList, userLocation)
                    } else {
                        // No filter, show all
                        displayMarkers(allProviders)
                        providerAdapter.updateList(allProviders, userLocation)
                    }
                } else {
                    Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ServiceProvidersResponse>, t: Throwable) {
                Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createIntervention(provider: ServiceProvider) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_book_intervention, null)
        val editDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editDescription)
        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_confirm)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)
        val textTitle = dialogView.findViewById<TextView>(R.id.text_title)
        
        textTitle.text = "Intervention avec ${provider.username}"
        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnConfirm.setOnClickListener {
            val description = editDescription.text.toString()
            if (description.isNotEmpty()) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
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
        val sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val clientId = sharedPreferences.getString("user_id", null)

        if (clientId != null) {
            val request = InterventionRequest(clientId, providerId, scheduledTime, description, latitude, longitude)
            ApiClient.instance.createIntervention(request).enqueue(object : Callback<InterventionResponse> {
                override fun onResponse(
                    call: Call<InterventionResponse>,
                    response: Response<InterventionResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(requireContext(), "Demande envoyée avec succès!", Toast.LENGTH_LONG).show()
                        // Go back to categories
                        onBackToCategories()
                    } else {
                        Toast.makeText(requireContext(), "Erreur: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<InterventionResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Erreur réseau: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(requireContext(), "Erreur: Utilisateur non connecté", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateClientLocation(latitude: Double, longitude: Double) {
        val sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val clientId = sharedPreferences.getString("user_id", null)

        if (clientId != null) {
            val request = LocationUpdateRequest(clientId, latitude, longitude)
            ApiClient.instance.updateClientLocation(request).enqueue(object : Callback<LocationUpdateResponse> {
                override fun onResponse(
                    call: Call<LocationUpdateResponse>,
                    response: Response<LocationUpdateResponse>
                ) {
                    if (!response.isSuccessful) {
                        Log.e("HomeFragment", "Erreur mise à jour localisation: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<LocationUpdateResponse>, t: Throwable) {
                    Log.e("HomeFragment", "Erreur réseau localisation: ${t.message}")
                }
            })
        }
    }
}