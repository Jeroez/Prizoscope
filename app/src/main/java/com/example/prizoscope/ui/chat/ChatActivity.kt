package com.example.prizoscope.ui.chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Message
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.example.prizoscope.ui.camera.CameraActivity
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.shopping.ShoppingActivity
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.auth.Login

class ChatActivity : AppCompatActivity() {

    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var userName: String // User's name, fetched dynamically

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val messageList = findViewById<RecyclerView>(R.id.message_list)
        val inputMessage = findViewById<EditText>(R.id.input_message)
        val btnSend = findViewById<Button>(R.id.btn_send)

        // Fetch user's actual name
        userName = fetchUserName()

        // Set up RecyclerView
        messageAdapter = MessageAdapter(messages)
        messageList.layoutManager = LinearLayoutManager(this)
        messageList.adapter = messageAdapter

        // Handle any incoming extras (e.g., imageUrl and price)
        val imageUrl = intent.getStringExtra("imageUrl")
        val price = intent.getStringExtra("price")

        if (!imageUrl.isNullOrEmpty()) {
            sendImageMessage(imageUrl, price)
        }

        // Load messages from Firestore
        loadMessages()

        // Send a new text message
        btnSend.setOnClickListener {
            val userMessage = inputMessage.text.toString()
            if (userMessage.isNotBlank()) {
                sendMessage(userMessage)
                inputMessage.text.clear()
            }
        }

        setupBottomNav()
    }

    private fun fetchUserName(): String {
        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        val loggedInUsername = sharedPreferences.getString("username", null)

        if (loggedInUsername == null) {
            Log.e("ChatActivity", "No logged-in username found. Redirecting to login.")
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        return loggedInUsername ?: "Unknown User"
    }

    private fun loadMessages() {
        val chatRef = firestore.collection("chats").document(userName)

        chatRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                messages.clear()

                val sortedEntries = document.data?.entries
                    ?.sortedBy { it.key.substringAfterLast('_').toIntOrNull() }

                sortedEntries?.forEach { entry ->
                    val key = entry.key
                    val value = entry.value.toString()

                    // Simplified logic for handling text and image messages
                    if (key.startsWith("user_message") || key.startsWith("admin_message")) {
                        if (value.startsWith("http")) {
                            messages.add(Message(sender = "User", imageUrl = value))
                        } else {
                            val sender = if (key.startsWith("user_message")) "User" else "Admin"
                            messages.add(Message(sender = sender, text = value))
                        }
                    }
                }

                messageAdapter.notifyDataSetChanged()
                scrollToBottom()
            } else {
                Log.d("ChatActivity", "No chat history found for $userName")
            }
        }.addOnFailureListener { e ->
            Log.e("ChatActivity", "Error loading messages", e)
        }
    }

    private fun scrollToBottom() {
        val messageList = findViewById<RecyclerView>(R.id.message_list)
        messageList.scrollToPosition(messages.size - 1)
    }

    private fun sendMessage(userMessage: String) {
        val chatRef = firestore.collection("chats").document(userName)

        chatRef.get().addOnSuccessListener { document ->
            val messageCount = document.data?.keys
                ?.filter { it.startsWith("user_message") || it.startsWith("admin_message") }
                ?.size ?: 0

            val newMessageKey = "user_message_${messageCount + 1}"

            val updateData = mapOf(newMessageKey to userMessage)

            chatRef.update(updateData)
                .addOnSuccessListener {
                    Log.d("ChatActivity", "Message sent successfully")
                    loadMessages()
                }
                .addOnFailureListener { e ->
                    Log.e("ChatActivity", "Error sending message", e)
                }
        }.addOnFailureListener { e ->
            Log.e("ChatActivity", "Error accessing chat document", e)
        }
    }

    private fun sendImageMessage(imageUrl: String, price: String?) {
        val chatRef = firestore.collection("chats").document(userName)

        chatRef.get().addOnSuccessListener { document ->
            val messageCount = document.data?.keys
                ?.filter { it.startsWith("user_message") || it.startsWith("admin_message") }
                ?.size ?: 0

            val newMessageKey = "user_message_${messageCount + 1}"

            val messageData = mutableMapOf<String, Any>(
                newMessageKey to imageUrl // Store the image URL
            )

            // Optionally include the price as part of the message
            price?.let {
                messageData["${newMessageKey}_price"] = it
            }

            chatRef.update(messageData).addOnSuccessListener {
                Log.d("ChatActivity", "Image message sent successfully")
                loadMessages() // Reload the chat to display the new image message
            }.addOnFailureListener { e ->
                Log.e("ChatActivity", "Failed to send image message", e)
            }
        }
    }


    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.selectedItemId = R.id.nav_chat

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_chat -> true
                R.id.nav_camera -> {
                    startActivity(Intent(this, CameraActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_shopping -> {
                    startActivity(Intent(this, ShoppingActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_bookmarks -> {
                    startActivity(Intent(this, BookmarkActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
