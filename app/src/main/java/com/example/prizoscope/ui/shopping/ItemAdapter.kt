package com.example.prizoscope.ui.shopping

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prizoscope.data.model.Item
import com.example.prizoscope.databinding.ItemShoppingBinding
import com.example.prizoscope.R

class ItemAdapter(private var items: List<Item>) :
    RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    fun updateData(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemShoppingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount() = items.size

    class ItemViewHolder(private val binding: ItemShoppingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item ) {
            binding.itemName.text = item.name
            binding.itemPrice.text = String.format("$%.2f", item.price)
            binding.itemRatings.text = String.format("%.1f/5", item.ratings)

            // Load image using Glide
            Glide.with(binding.itemImage.context)
                .load(item.imageLink)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(binding.itemImage)
        }
    }
}
