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
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.camera.CameraActivity
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.shopping.ShoppingActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: RecyclerView
    private lateinit var inputMessage: EditText
    private lateinit var btnSend: Button
    private val messages = mutableListOf<Message>()
    private val firestore = FirebaseFirestore.getInstance()

    // Replace with logged-in user's name
    private val userName: String by lazy { getLoggedInUserName() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Initialize UI components
        messageList = findViewById(R.id.message_list)
        inputMessage = findViewById(R.id.input_message)
        btnSend = findViewById(R.id.btn_send)

        // Setup RecyclerView
        messageAdapter = MessageAdapter(messages)
        messageList.layoutManager = LinearLayoutManager(this)
        messageList.adapter = messageAdapter

        // Setup bottom navigation
        setupBottomNav()

        // Load chat messages
        loadMessages()

        // Send message when button is clicked
        btnSend.setOnClickListener {
            val userMessage = inputMessage.text.toString()
            if (userMessage.isNotBlank()) {
                sendMessage(userMessage)
                inputMessage.text.clear()
            }
        }
    }

    private fun loadMessages() {
        val chatRef = firestore.collection("chats").document(userName)

        chatRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("ChatActivity", "Error fetching messages", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                messages.clear()

                snapshot.data?.forEach { (key, value) ->
                    if (key.startsWith("user_message_")) {
                        messages.add(Message(sender = "User", text = value.toString()))
                    } else if (key.startsWith("admin_message_")) {
                        messages.add(Message(sender = "Admin", text = value.toString()))
                    }
                }

                messages.sortBy { it.text } // Optional sorting logic
                messageAdapter.notifyDataSetChanged()
                messageList.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun sendMessage(userMessage: String) {
        val chatRef = firestore.collection("chats").document(userName)

        chatRef.get().addOnSuccessListener { document ->
            // Calculate the next message index based on the existing number of fields
            val nextMessageIndex = document?.data?.filterKeys { it.startsWith("user_message_") }?.size?.plus(1) ?: 1
            val newMessageKey = "user_message_$nextMessageIndex"

            // Create update data with explicit type casting
            val updateData = mapOf(
                newMessageKey to userMessage
            )

            chatRef.update(updateData)
                .addOnSuccessListener {
                    Log.d("ChatActivity", "Message sent successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("ChatActivity", "Error sending message", e)
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

    // Mock function: Replace with logic to fetch logged-in user's name
    private fun getLoggedInUserName(): String {
        // Fetch from SharedPreferences or SQLite
        return "JohnDoe"
    }
}
