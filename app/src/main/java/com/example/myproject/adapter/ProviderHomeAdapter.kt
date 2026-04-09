package com.example.myproject.adapter

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.R
import com.example.myproject.model.ServiceProvider
import com.google.android.material.button.MaterialButton

class ProviderHomeAdapter(
    private var providers: List<ServiceProvider>,
    private var userLocation: android.location.Location? = null,
    private val onProviderClick: (ServiceProvider) -> Unit,
    private val onDemanderClick: (ServiceProvider) -> Unit = {}
) : RecyclerView.Adapter<ProviderHomeAdapter.ProviderViewHolder>() {

    class ProviderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.img_provider_icon)
        val tvName: TextView = view.findViewById(R.id.tv_provider_name)
        val tvDistance: TextView = view.findViewById(R.id.tv_provider_distance)
        val ratingBar: RatingBar = view.findViewById(R.id.rating_bar)
        val tvReviewCount: TextView = view.findViewById(R.id.tv_review_count)
        val btnDemander: MaterialButton = view.findViewById(R.id.btn_demander)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_provider_home, parent, false)
        return ProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        val provider = providers[position]
        holder.tvName.text = provider.username
        
        // Calculate and display distance
        if (userLocation != null) {
            val distance = calculateDistance(
                userLocation!!.latitude,
                userLocation!!.longitude,
                provider.latitude,
                provider.longitude
            )
            holder.tvDistance.text = String.format("%.1f km", distance)
        } else {
            holder.tvDistance.text = "Distance inconnue"
        }

        // Set rating and review count
        holder.ratingBar.rating = provider.rating.toFloat()
        holder.tvReviewCount.text = "(${provider.reviewCount})"
        
        // Set icon based on profession
        val iconResId = when (provider.profession.lowercase()) {
            "électricien", "electricien" -> R.drawable.marker_electrician
            "plombier" -> R.drawable.marker_plumber
            "climatisation" -> R.drawable.marker_clim
            "mécanicien", "mecanicien" -> R.drawable.marker_mechanic
            else -> R.drawable.marker_electrician
        }
        holder.imgIcon.setImageResource(iconResId)

        // Handle item click (zoom to provider on map)
        holder.itemView.setOnClickListener { onProviderClick(provider) }
        
        // Handle demand button click
        holder.btnDemander.setOnClickListener { onDemanderClick(provider) }
    }

    override fun getItemCount() = providers.size

    fun updateList(newProviders: List<ServiceProvider>, newUserLocation: android.location.Location? = null) {
        providers = newProviders
        if (newUserLocation != null) {
            userLocation = newUserLocation
        }
        notifyDataSetChanged()
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000 // Convert meters to kilometers
    }
}

