package com.example.prizoscope.ui.shopping

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Review
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewsAdapter(private var reviews: List<Review>) :
    RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userName: TextView = itemView.findViewById(R.id.review_user)
        private val rating: TextView = itemView.findViewById(R.id.review_rating)
        private val comment: TextView = itemView.findViewById(R.id.review_comment)
        private val date: TextView = itemView.findViewById(R.id.review_date)

        fun bind(review: Review) {
            userName.text = review.user
            rating.text = "${review.rating} ★"
            comment.text = review.comment
            date.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(review.timestamp)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount() = reviews.size

    fun updateData(newReviews: List<Review>) {
        reviews = newReviews
        notifyDataSetChanged()
    }
}