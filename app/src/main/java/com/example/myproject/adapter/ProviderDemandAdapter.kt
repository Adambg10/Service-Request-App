package com.example.myproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.R
import com.example.myproject.model.ProviderIntervention
import com.google.android.material.button.MaterialButton

class ProviderDemandAdapter(
    private var demands: List<ProviderIntervention>,
    private val onAcceptClick: (ProviderIntervention) -> Unit,
    private val onDenyClick: (ProviderIntervention) -> Unit,
    private val onItineraryClick: (Double, Double) -> Unit,
    private val onCallClick: (String) -> Unit,
    private val onResolvedClick: (ProviderIntervention) -> Unit = {},
    private val onFailedClick: (ProviderIntervention) -> Unit = {}
) : RecyclerView.Adapter<ProviderDemandAdapter.ProviderViewHolder>() {

    fun updateList(newDemands: List<ProviderIntervention>) {
        demands = newDemands
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_provider_demand, parent, false)
        return ProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        val demand = demands[position]
        holder.bind(demand)
    }

    override fun getItemCount(): Int = demands.size

    inner class ProviderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textClientName: TextView = itemView.findViewById(R.id.textClientName)
        private val chipDistance: com.google.android.material.chip.Chip = itemView.findViewById(R.id.chipDistance)
        private val textProblemDescription: TextView = itemView.findViewById(R.id.textProblemDescription)
        private val textScheduledTime: TextView = itemView.findViewById(R.id.textScheduledTime)
        private val layoutButtons: LinearLayout = itemView.findViewById(R.id.layoutButtons)
        private val btnAccept: MaterialButton = itemView.findViewById(R.id.btnAccept)
        private val btnDeny: MaterialButton = itemView.findViewById(R.id.btnDeny)
        private val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        private val btnShowItinerary: MaterialButton = itemView.findViewById(R.id.btnShowItinerary)
        private val btnCall: MaterialButton = itemView.findViewById(R.id.btnCall)
        private val layoutResolution: LinearLayout = itemView.findViewById(R.id.layoutResolution)
        private val btnResolutionSuccess: MaterialButton = itemView.findViewById(R.id.btnResolutionSuccess)
        private val btnResolutionFailed: MaterialButton = itemView.findViewById(R.id.btnResolutionFailed)

        fun bind(demand: ProviderIntervention) {
            textClientName.text = demand.client_name
            textProblemDescription.text = demand.problem_description
            textScheduledTime.text = demand.scheduled_time

            // Show distance if available
            if (demand.distance_km != null) {
                chipDistance.visibility = View.VISIBLE
                val distanceText = if (demand.distance_km!! < 1f) {
                    "${(demand.distance_km!! * 1000).toInt()} m"
                } else {
                    "%.1f km".format(demand.distance_km)
                }
                chipDistance.text = distanceText
            } else {
                chipDistance.visibility = View.GONE
            }

            // Reset all views to default state
            layoutButtons.visibility = View.GONE
            btnShowItinerary.visibility = View.GONE
            btnCall.visibility = View.GONE
            layoutResolution.visibility = View.GONE
            textStatus.visibility = View.GONE

            when (demand.status) {
                "en_attente" -> {
                    // Pending - show Accept/Deny buttons
                    layoutButtons.visibility = View.VISIBLE
                    btnAccept.setOnClickListener { onAcceptClick(demand) }
                    btnDeny.setOnClickListener { onDenyClick(demand) }
                }
                "acceptee" -> {
                    // Accepted - show Itinerary and Call buttons
                    if (demand.client_latitude != null && demand.client_longitude != null) {
                        btnShowItinerary.visibility = View.VISIBLE
                        btnShowItinerary.setOnClickListener {
                            onItineraryClick(demand.client_latitude, demand.client_longitude)
                        }
                    }
                    if (!demand.client_phone.isNullOrEmpty()) {
                        btnCall.visibility = View.VISIBLE
                        btnCall.setOnClickListener { onCallClick(demand.client_phone) }
                    }
                }
                "arrive" -> {
                    // Arrived - show Resolution buttons
                    layoutResolution.visibility = View.VISIBLE
                    btnResolutionSuccess.setOnClickListener { onResolvedClick(demand) }
                    btnResolutionFailed.setOnClickListener { onFailedClick(demand) }
                    
                    // Also keep call button visible
                    if (!demand.client_phone.isNullOrEmpty()) {
                        btnCall.visibility = View.VISIBLE
                        btnCall.setOnClickListener { onCallClick(demand.client_phone) }
                    }
                }
                "terminee" -> {
                    textStatus.visibility = View.VISIBLE
                    textStatus.text = "✓ Problème résolu"
                    textStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                }
                "echouee" -> {
                    textStatus.visibility = View.VISIBLE
                    textStatus.text = "✗ Non résolu"
                    textStatus.setTextColor(android.graphics.Color.parseColor("#F44336"))
                }
                "annulee" -> {
                    textStatus.visibility = View.VISIBLE
                    textStatus.text = "Annulée"
                    textStatus.setTextColor(android.graphics.Color.GRAY)
                }
                else -> {
                    textStatus.visibility = View.VISIBLE
                    textStatus.text = "Status: ${demand.status}"
                    textStatus.setTextColor(android.graphics.Color.GRAY)
                }
            }
        }
    }
}
