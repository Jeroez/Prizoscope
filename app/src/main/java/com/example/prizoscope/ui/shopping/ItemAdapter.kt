package com.example.prizoscope.ui.shopping

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Item

class ItemAdapter(
    private var items: List<Item>,
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemImage: ImageView = itemView.findViewById(R.id.item_image)
        private val itemName: TextView = itemView.findViewById(R.id.item_name)
        private val itemPrice: TextView = itemView.findViewById(R.id.item_price)
        private val itemRatings: TextView = itemView.findViewById(R.id.item_ratings)

        fun bind(item: Item) {
            // Load item image using Glide
            Glide.with(itemView.context)
                .load(item.imageLink)
                .placeholder(R.drawable.ic_launcher_foreground) 
                .into(itemImage)

            // Bind item details
            itemName.text = item.name
            itemPrice.text = "₱${item.price}"
            itemRatings.text = "Rating: ${item.rating} ★"

            // Handle item click
            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shopping, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }
}
