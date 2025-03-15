package com.example.prizoscope.ui.shopping

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Item
import com.example.prizoscope.data.model.Review
import com.example.prizoscope.ui.chat.ChatActivity
import com.example.prizoscope.databinding.ActivityItemDetailBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.util.Date
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.camera.CameraActivity
import com.example.prizoscope.ui.shopping.ShoppingActivity
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.util.Log

class ItemDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityItemDetailBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var currentItem: Item
    private lateinit var reviewsAdapter: ReviewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firestore = FirebaseFirestore.getInstance()

        currentItem = (intent.getSerializableExtra("item") as? Item) ?: run {
            Toast.makeText(this, "Invalid item data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupReviews()
        setupRatingSystem()
        setupBottomNav()
    }

    private fun setupUI() {
        Log.d("FirestoreDebug", "Current Item Data: $currentItem") //  Debug log

        Glide.with(this)
            .load(currentItem.img_url)
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(binding.itemImage)

        binding.itemName.text = currentItem.name
        binding.itemPrice.text = "${currentItem.getEffectivePrice()}"

        binding.itemRating.rating = currentItem.rating ?: 5f

        binding.purchaseButton.setOnClickListener {
            val itemName = currentItem.name // Use item name instead of ID

            // 1. Fetch the item document from Firestore
            firestore.collection("items")
                .whereEqualTo("name", itemName)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        Toast.makeText(this, "Item not found in database", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val document = querySnapshot.documents[0]
                    val storeName = document.getString("Store") ?: run {
                        Toast.makeText(this, "Store not assigned to this item", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // 2. Verify store exists in admins
                    firestore.collection("admins").document(storeName)
                        .get()
                        .addOnSuccessListener { adminDoc ->
                            if (!adminDoc.exists()) {
                                Toast.makeText(this, "Store admin does not exist", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            // 3. Get/Create chat document
                            val username = getSharedPreferences("user_session", MODE_PRIVATE)
                                .getString("username", "") ?: ""
                            val chatId = "$username | $storeName"

                            // 4. Add message to chat (new or existing)
                            val messageData = hashMapOf(
                                "content" to currentItem.img_url,
                                "sender" to "user",
                                "timestamp" to System.currentTimeMillis(),
                                "type" to "image"
                            )

                            // Update or create chat document atomically
                            firestore.collection("chats").document(chatId)
                                .update("message_${System.currentTimeMillis()}", messageData)
                                .addOnFailureListener {
                                    // Create new chat if document doesn't exist
                                    firestore.collection("chats").document(chatId)
                                        .set(hashMapOf("message_0" to messageData))
                                }

                            // 5. Open ChatActivity
                            Intent(this, ChatActivity::class.java).apply {
                                putExtra("admin_name", storeName)
                                putExtra("chat_id", chatId)
                                startActivity(this)
                            }
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error fetching item details", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Item lookup failed", e)
                }
        }

        binding.bookmarkButton.setOnClickListener {
            saveToBookmarks(currentItem)
        }
    }


    private fun setupReviews() {
        reviewsAdapter = ReviewsAdapter(emptyList())
        binding.reviewsList.apply {
            layoutManager = LinearLayoutManager(this@ItemDetailActivity)
            adapter = reviewsAdapter
        }

        // Debug log to check Firestore query values
        Log.d("FirestoreDebug", "Querying Reviews for itemName: ${currentItem.name}, Store: ${currentItem.store}")

        firestore.collection("Reviews")
            .whereEqualTo("itemName", currentItem.name)
            .whereEqualTo("store", currentItem.store) // Ensure case matches Firestore field name
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots.isEmpty) {
                    Log.d("FirestoreDebug", "No reviews found for this item.")
                } else {
                    val reviews = snapshots.documents.mapNotNull { it.toObject<Review>() }
                    Log.d("FirestoreDebug", "Fetched ${reviews.size} reviews")
                    reviewsAdapter.updateData(reviews)
                    updateAverageRating(reviews)
                }
            }
            .addOnFailureListener { error ->
                Log.e("FirestoreDebug", "Error fetching reviews: ", error)
            }
    }


    private fun updateAverageRating(reviews: List<Review>) {
        if (reviews.isEmpty()) {
            binding.itemRating.rating = 5f
            return
        }

        val average = reviews.map { it.rating }.average().toFloat()
        firestore.collection("items").document(currentItem.id)
            .update("rating", average)
            .addOnSuccessListener {
                currentItem = currentItem.copy(rating = average)
                binding.itemRating.rating = average
            }
    }

    private fun setupRatingSystem() {
        binding.ratingSubmit.setOnClickListener {
            val rating = binding.ratingBar.rating
            val comment = binding.ratingComment.text.toString()

            if (rating == 0f) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val review = Review(
                itemName = currentItem.name,
                store = currentItem.store,
                user = getCurrentUsername(),
                rating = rating,
                comment = comment,
                timestamp = Date()
            )

            firestore.collection("Reviews").add(review)
                .addOnSuccessListener {
                    binding.ratingComment.text.clear()
                    binding.ratingBar.rating = 0f
                    Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to submit review", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun getCurrentUsername(): String {
        return getSharedPreferences("user_session", MODE_PRIVATE)
            .getString("username", "Anonymous") ?: "Anonymous"
    }

    private fun saveToBookmarks(item: Item) {
        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)

        if (username != null) {
            Toast.makeText(this, "Item bookmarked", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Login to bookmark", Toast.LENGTH_SHORT).show()
        }
    }
    private fun setupBottomNav() {
        val bottomNav = binding.bottomNav
        bottomNav.selectedItemId = R.id.nav_shopping

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_camera -> {
                    navigateToActivity(CameraActivity::class.java)
                    true
                }
                R.id.nav_shopping -> {
                    navigateToActivity(ShoppingActivity::class.java)
                    true
                }
                R.id.nav_bookmarks -> {
                    navigateToActivity(BookmarkActivity::class.java)
                    true
                }
                R.id.nav_settings -> {
                    navigateToActivity(SettingsActivity::class.java)
                    true
                }
                R.id.nav_chat -> {
                    navigateToActivity(ChatActivity::class.java)
                    true
                }
                else -> false
            }
        }
    }
    private fun navigateToActivity(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }
}
