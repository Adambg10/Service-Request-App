package com.example.myproject

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.adapter.ProviderDemandAdapter
import com.example.myproject.model.CompleteInterventionRequest
import com.example.myproject.model.GenericResponse
import com.example.myproject.model.LocationUpdateRequest
import com.example.myproject.model.LocationUpdateResponse
import com.example.myproject.model.ProviderIntervention
import com.example.myproject.model.ProviderInterventionsResponse
import com.example.myproject.model.UpdateStatusRequest
import com.example.myproject.model.UpdateStatusResponse
import com.example.myproject.network.ApiClient
import com.example.myproject.service.LocationTrackingService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeProviderFragment : Fragment() {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var recyclerDemands: RecyclerView
    private lateinit var demandAdapter: ProviderDemandAdapter
    private var demands: List<ProviderIntervention> = emptyList()
    private var providerLatitude: Double? = null
    private var providerLongitude: Double? = null
    private var isTrackingServiceRunning = false

    private val demandsPollingHandler = Handler(Looper.getMainLooper())
    private val demandsPollingRunnable = object : Runnable {
        override fun run() {
            fetchDemands()
            demandsPollingHandler.postDelayed(this, 3000) // Poll every 3 seconds
        }
    }

    private val LOCATION_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_provider, container, false)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        // Stop all polling immediately
        demandsPollingHandler.removeCallbacks(demandsPollingRunnable)
        // Service continues in background
        // Clean up map references
        if (::map.isInitialized) {
            map.clear()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        recyclerDemands = view.findViewById(R.id.recycler_demands)

        setupRecyclerView()
        setupMap()
        fetchDemands()
    }

    override fun onResume() {
        super.onResume()
        // Start polling for demands when fragment is visible
        demandsPollingHandler.post(demandsPollingRunnable)
        // Start location tracking service
        startLocationTrackingService()
    }

    override fun onPause() {
        super.onPause()
        demandsPollingHandler.removeCallbacks(demandsPollingRunnable)
    }

    private fun setupRecyclerView() {
        recyclerDemands.layoutManager = LinearLayoutManager(context)
        demandAdapter = ProviderDemandAdapter(
            emptyList(),
            onAcceptClick = { demand -> updateInterventionStatus(demand.id, "acceptee") },
            onDenyClick = { demand -> updateInterventionStatus(demand.id, "annulee") },
            onItineraryClick = { lat, lng -> startNavigation(lat, lng) },
            onCallClick = { phone -> startCall(phone) },
            onResolvedClick = { demand -> completeIntervention(demand.id, "resolved") },
            onFailedClick = { demand -> completeIntervention(demand.id, "failed") }
        )
        recyclerDemands.adapter = demandAdapter
    }

    private fun setupMap() {
        var mapFragment = childFragmentManager.findFragmentById(R.id.map_view) as? SupportMapFragment
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.map_view, mapFragment)
                .commit()
        }
        mapFragment?.getMapAsync { googleMap ->
            map = googleMap
            checkLocationPermissions()
            
            // Move camera to Tunisia by default
            val tunisia = LatLng(36.8065, 10.1815)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(tunisia, 10f))
        }
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
            
            // Zoom to user location and save coordinates
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    providerLatitude = location.latitude
                    providerLongitude = location.longitude
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    
                    // Recalculate distances if we already have demands
                    if (demands.isNotEmpty()) {
                        calculateDistancesAndUpdateList()
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
        val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] ?: false
        } else {
            true // Not needed for older versions
        }
 
        if (fineLocationGranted || coarseLocationGranted) {
            enableUserLocation()
            
            if (backgroundLocationGranted) {
                startLocationTrackingService()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Toast.makeText(
                    context,
                    "Background location permission is required for continuous tracking",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun fetchDemands() {
        val sharedPreferences = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        val providerId = sharedPreferences.getString("user_id", null)

        if (providerId != null) {
            ApiClient.instance.getProviderInterventions(providerId).enqueue(object : Callback<ProviderInterventionsResponse> {
                override fun onResponse(
                    call: Call<ProviderInterventionsResponse>,
                    response: Response<ProviderInterventionsResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        demands = response.body()!!.interventions
                        calculateDistancesAndUpdateList()
                        
                        // Show markers on map
                        showMarkersOnMap()
                    } else {
                        Toast.makeText(context, "Failed to load demands", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ProviderInterventionsResponse>, t: Throwable) {
                    Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun calculateDistancesAndUpdateList() {
        // Calculate distances from provider's location
        if (providerLatitude != null && providerLongitude != null) {
            val providerLocation = Location("provider")
            providerLocation.latitude = providerLatitude!!
            providerLocation.longitude = providerLongitude!!

            demands.forEach { demand ->
                if (demand.client_latitude != null && demand.client_longitude != null) {
                    val clientLocation = Location("client")
                    clientLocation.latitude = demand.client_latitude
                    clientLocation.longitude = demand.client_longitude
                    val distanceMeters = providerLocation.distanceTo(clientLocation)
                    demand.distance_km = distanceMeters / 1000f
                }
            }
        }

        // Sort: pending first, then by distance (nearest first)
        val sortedDemands = demands.sortedWith(compareBy(
            // Priority 1: Status (en_attente = 0, acceptee = 1, arrive = 2, others = 3)
            { demand ->
                when (demand.status) {
                    "en_attente" -> 0
                    "acceptee" -> 1
                    "arrive" -> 2
                    else -> 3
                }
            },
            // Priority 2: Distance (nearest first)
            { it.distance_km ?: Float.MAX_VALUE }
        ))

        demandAdapter.updateList(sortedDemands)
    }

    private fun updateInterventionStatus(interventionId: String, status: String) {
        val request = UpdateStatusRequest(interventionId, status)
        ApiClient.instance.updateInterventionStatus(request).enqueue(object : Callback<UpdateStatusResponse> {
            override fun onResponse(
                call: Call<UpdateStatusResponse>,
                response: Response<UpdateStatusResponse>
            ) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT).show()
                    fetchDemands() // Refresh list
                } else {
                    Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UpdateStatusResponse>, t: Throwable) {
                Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun completeIntervention(interventionId: String, resolution: String) {
        val request = CompleteInterventionRequest(interventionId, resolution)
        ApiClient.instance.completeIntervention(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(
                call: Call<GenericResponse>,
                response: Response<GenericResponse>
            ) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val message = if (resolution == "resolved") 
                        "Intervention marquée comme résolue" 
                    else 
                        "Intervention marquée comme non résolue"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    fetchDemands() // Refresh list
                } else {
                    Toast.makeText(context, "Échec de la mise à jour", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(context, "Erreur réseau: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun startLocationTrackingService() {
        // Check if we have necessary permissions
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasBackgroundPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required on older versions
        }
        
        if (hasLocationPermission && hasBackgroundPermission && !isTrackingServiceRunning) {
            val sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val providerId = sharedPreferences.getString("user_id", null)?.toIntOrNull()
            
            if (providerId != null) {
                LocationTrackingService.startService(requireContext(), providerId)
                isTrackingServiceRunning = true
                Toast.makeText(context, "Background location tracking started", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun stopLocationTrackingService() {
        if (isTrackingServiceRunning) {
            LocationTrackingService.stopService(requireContext())
            isTrackingServiceRunning = false
            Toast.makeText(context, "Background location tracking stopped", Toast.LENGTH_SHORT).show()
        }
    }
    fun startNavigation(destinationLat: Double, destinationLng: Double) {
        // Creates a URI that opens Google Maps in "Navigation" mode
        val gmmIntentUri = Uri.parse("google.navigation:q=$destinationLat,$destinationLng")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        
        if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(context, "Google Maps is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneNumber")
        startActivity(intent)
    }

    // Map of markers to interventions for click handling
    private val markerInterventionMap = mutableMapOf<Marker, ProviderIntervention>()

    private var currentMarkerIds = HashSet<String>()

    private fun showMarkersOnMap() {
        if (!::map.isInitialized) return

        val newMarkerSignature = demands.filter { it.client_latitude != null }
            .joinToString("|") { "${it.id}-${it.status}-${it.client_latitude}-${it.client_longitude}" }

        val currentSignature = currentMarkerIds.joinToString("|")
        if (newMarkerSignature == currentSignature) return

        map.clear()
        markerInterventionMap.clear()
        currentMarkerIds.clear()

        currentMarkerIds.add(newMarkerSignature)

        demands.forEach { intervention ->
            if (intervention.client_latitude != null && intervention.client_longitude != null) {
                val clientLocation = LatLng(intervention.client_latitude, intervention.client_longitude)
                
                // Choose marker icon based on status
                val iconResId = when (intervention.status) {
                    "en_attente" -> R.drawable.marker_pending_demand  // Orange for pending
                    else -> R.drawable.marker_client_location  // Blue for accepted/arrived
                }
                val icon = bitmapDescriptorFromVector(requireContext(), iconResId)
                
                val snippet = buildString {
                    append(intervention.problem_description)
                    if (intervention.distance_km != null) {
                        append(" • ")
                        if (intervention.distance_km!! < 1f) {
                            append("${(intervention.distance_km!! * 1000).toInt()} m")
                        } else {
                            append("%.1f km".format(intervention.distance_km))
                        }
                    }
                }
                
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(clientLocation)
                        .title(intervention.client_name)
                        .snippet(snippet)
                        .icon(icon)
                )
                
                marker?.let { markerInterventionMap[it] = intervention }
            }
        }

        // Set up marker click listener
        map.setOnInfoWindowClickListener { marker ->
            val intervention = markerInterventionMap[marker]
            if (intervention != null && intervention.status == "en_attente") {
                updateInterventionStatus(intervention.id, "acceptee")
            } else if (intervention != null && 
                       (intervention.status == "acceptee" || intervention.status == "arrive")) {
                if (intervention.client_latitude != null && intervention.client_longitude != null) {
                    startNavigation(intervention.client_latitude, intervention.client_longitude)
                }
            }
        }

        map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoContents(marker: Marker): View? = null
            
            override fun getInfoWindow(marker: Marker): View? {
                val intervention = markerInterventionMap[marker] ?: return null
                
                val view = layoutInflater.inflate(R.layout.marker_info_window, null)
                val titleText = view.findViewById<android.widget.TextView>(R.id.info_title)
                val snippetText = view.findViewById<android.widget.TextView>(R.id.info_snippet)
                val actionText = view.findViewById<android.widget.TextView>(R.id.info_action)
                
                titleText.text = marker.title
                snippetText.text = marker.snippet
                
                when (intervention.status) {
                    "en_attente" -> {
                        actionText.visibility = View.VISIBLE
                        actionText.text = "Appuyez pour accepter"
                        actionText.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                    }
                    "acceptee", "arrive" -> {
                        actionText.visibility = View.VISIBLE
                        actionText.text = "Appuyez pour l'itinéraire"
                        actionText.setTextColor(android.graphics.Color.parseColor("#2196F3"))
                    }
                    else -> {
                        actionText.visibility = View.GONE
                    }
                }
                
                return view
            }
        })

        if (demands.filter { it.client_latitude != null && it.client_longitude != null }.size > 1) {
            val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
            demands.forEach { intervention ->
                if (intervention.client_latitude != null && intervention.client_longitude != null) {
                    boundsBuilder.include(LatLng(intervention.client_latitude, intervention.client_longitude))
                }
            }
            if (providerLatitude != null && providerLongitude != null) {
                boundsBuilder.include(LatLng(providerLatitude!!, providerLongitude!!))
            }
            try {
                val bounds = boundsBuilder.build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (e: Exception) {
                // Bounds calculation failed
            }
        }
    }

    private fun showClientMarkersOnMap() {
        if (!::map.isInitialized) return

        map.clear()

        val activeInterventions = demands.filter { 
            it.status == "acceptee" || it.status == "arrive" 
        }

        activeInterventions.forEach { intervention ->
            if (intervention.client_latitude != null && intervention.client_longitude != null) {
                val clientLocation = LatLng(intervention.client_latitude, intervention.client_longitude)
                
                val icon = bitmapDescriptorFromVector(requireContext(), R.drawable.marker_client_location)
                
                map.addMarker(
                    MarkerOptions()
                        .position(clientLocation)
                        .title(intervention.client_name)
                        .snippet(intervention.problem_description)
                        .icon(icon)
                )
            }
        }

        if (activeInterventions.size == 1) {
            val intervention = activeInterventions[0]
            if (intervention.client_latitude != null && intervention.client_longitude != null) {
                val clientLocation = LatLng(intervention.client_latitude, intervention.client_longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(clientLocation, 14f))
            }
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

}
