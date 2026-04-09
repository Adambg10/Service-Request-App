package com.example.myproject.service

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.R
import com.example.myproject.databinding.ItemServiceCardBinding

class ServiceAdapter(
    private var items: List<com.example.myproject.model.ServiceProvider>,
    private val onBookClicked: (com.example.myproject.model.ServiceProvider) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    fun updateList(newItems: List<com.example.myproject.model.ServiceProvider>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val binding = ItemServiceCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ServiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ServiceViewHolder(private val binding: ItemServiceCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: com.example.myproject.model.ServiceProvider) {
            binding.textServiceName.text = item.username
            binding.textServiceDescription.text = item.description
            binding.textServiceRating.text = String.format("%.1f", item.rating)
            binding.textServiceReviews.text = "(${item.reviewCount} avis)"
            binding.textProfession.text=item.profession
            if(item.profession=="Electricien"){
                binding.imageProfessionIcon.setImageResource(R.drawable.outline_bolt_24)
            }
            if(item.profession=="Plombier"){
                binding.imageProfessionIcon.setImageResource(R.drawable.outline_plumbing_24)
                }
            if(item.profession=="Climatisation"){
                binding.imageProfessionIcon.setImageResource(R.drawable.baseline_ac_unit_24)

            }
            if(item.profession=="Mecanicien") {
                binding.imageProfessionIcon.setImageResource(R.drawable.outline_car_repair_24)
            }

            binding.buttonBook.setOnClickListener { onBookClicked(item) }
        }
    }
}
