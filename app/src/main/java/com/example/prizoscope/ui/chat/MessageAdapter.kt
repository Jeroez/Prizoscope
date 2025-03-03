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

class MessageAdapter(
    private val messages: List<Message>,
    private val onImageClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val TYPE_USER = 0
        const val TYPE_ADMIN = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sender.equals("user", true)) TYPE_USER else TYPE_ADMIN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = when (viewType) {
            TYPE_USER -> R.layout.item_message_user
            else -> R.layout.item_message_admin
        }

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as MessageViewHolder).bind(messages[position])
    }

    override fun getItemCount() = messages.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val messageImage: ImageView = itemView.findViewById(R.id.message_image)

        fun bind(message: Message) {
            message.imageUrl?.let { url ->
                // Handle image message
                messageText.visibility = View.GONE
                messageImage.visibility = View.VISIBLE
                Glide.with(itemView)
                    .load(url)
                    .into(messageImage)

                // Set click listener for images
                messageImage.setOnClickListener {
                    onImageClick(url)
                }
            } ?: run {
                // Handle text message
                messageText.visibility = View.VISIBLE
                messageImage.visibility = View.GONE
                messageImage.setOnClickListener(null)
                messageText.text = message.text ?: ""
            }
        }
    }
}