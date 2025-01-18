package com.example.prizoscope.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Message

class MessageAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_USER = 0
    private val TYPE_ADMIN = 1

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sender == "User") TYPE_USER else TYPE_ADMIN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == TYPE_USER) {
            R.layout.item_message_user
        } else {
            R.layout.item_message_admin
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as MessageViewHolder).bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView? = itemView.findViewById(R.id.message_text)
        private val messageImage: ImageView? = itemView.findViewById(R.id.message_image)

        fun bind(message: Message) {
            if (message.imageUrl != null) {
                // Display image
                messageText?.visibility = View.GONE
                messageImage?.visibility = View.VISIBLE

                // Load the image using Glide
                Glide.with(itemView.context)
                    .load(message.imageUrl)
                    .placeholder(R.drawable.ic_launcher_foreground) // Placeholder image
                    .into(messageImage!!)
            } else {
                // Display text
                messageText?.visibility = View.VISIBLE
                messageImage?.visibility = View.GONE
                messageText?.text = message.text
            }
        }
    }
}
