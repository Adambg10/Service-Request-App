package com.example.myproject.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.myproject.MainActivity
import com.example.myproject.R
import com.example.myproject.model.LocationUpdateRequest
import com.example.myproject.model.LocationUpdateResponse
import com.example.myproject.network.ApiClient
import com.google.android.gms.location.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 5 * 60 * 1000L // 5 minutes
    private var currentLocation: Location? = null
    private var providerId: Int = -1

    companion object {
        const val CHANNEL_ID = "LocationTrackingChannel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_PROVIDER_ID = "provider_id"
        private const val TAG = "LocationTrackingService"

        fun startService(context: Context, providerId: Int) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                putExtra(EXTRA_PROVIDER_ID, providerId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        
        providerId = intent?.getIntExtra(EXTRA_PROVIDER_ID, -1) ?: -1
        
        if (providerId == -1) {
            Log.e(TAG, "Invalid provider ID, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        // Start foreground with notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Start location updates
        startLocationUpdates()

        return START_STICKY // Restart service if killed
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracking your location in the background"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText("Your location is being shared with clients")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted, stopping service")
            stopSelf()
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            updateInterval
        ).apply {
            setMinUpdateIntervalMillis(updateInterval / 2)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    sendLocationToServer(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        Log.d(TAG, "Location updates started")
    }

    private fun sendLocationToServer(location: Location) {
        Log.d(TAG, "Sending location to server: ${location.latitude}, ${location.longitude}")
        
        val request = LocationUpdateRequest(
            user_id = providerId.toString(),
            latitude = location.latitude,
            longitude = location.longitude
        )

        ApiClient.instance.updateProviderLocation(request).enqueue(object :
            Callback<LocationUpdateResponse> {
            override fun onResponse(
                call: Call<LocationUpdateResponse>,
                response: Response<LocationUpdateResponse>
            ) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Log.d(TAG, "Location updated successfully")
                } else {
                    Log.e(TAG, "Failed to update location: ${response.body()?.message}")
                }
            }

            override fun onFailure(call: Call<LocationUpdateResponse>, t: Throwable) {
                Log.e(TAG, "Error updating location: ${t.message}")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacksAndMessages(null)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
