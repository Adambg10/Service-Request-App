package com.example.myproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.databinding.ItemInterventionBinding
import com.example.myproject.model.Intervention

class InterventionAdapter(
    private var items: List<Intervention>
) : RecyclerView.Adapter<InterventionAdapter.InterventionViewHolder>() {

    fun updateList(newItems: List<Intervention>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterventionViewHolder {
        val binding = ItemInterventionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InterventionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InterventionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class InterventionViewHolder(private val binding: ItemInterventionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Intervention) {
            binding.textProviderName.text = item.provider_name
            binding.textProfession.text = item.profession
            binding.textDate.text = item.scheduled_time
            binding.textStatus.text = item.status.replace("_", " ").capitalize()
            
            val statusColor = when (item.status) {
                "en_attente" -> android.graphics.Color.parseColor("#FFA000") // Orange
                "acceptee" -> android.graphics.Color.parseColor("#1976D2") // Blue
                "terminee" -> android.graphics.Color.parseColor("#388E3C") // Green
                "annulee" -> android.graphics.Color.parseColor("#D32F2F") // Red
                else -> android.graphics.Color.GRAY
            }
            binding.textStatus.setTextColor(statusColor)
        }
    }
}
