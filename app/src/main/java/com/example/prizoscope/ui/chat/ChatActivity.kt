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
import com.example.prizoscope.ui.auth.Login

import com.example.prizoscope.utils.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ChatActivity : AppCompatActivity() {

    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private val firestore = FirebaseFirestore.getInstance()

    // Replace "JohnDoe" with the user's actual name from the database
    private lateinit var userName: String // To be fetched dynamically

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val messageList = findViewById<RecyclerView>(R.id.message_list)
        val inputMessage = findViewById<EditText>(R.id.input_message)
        val btnSend = findViewById<Button>(R.id.btn_send)

        // Fetch the user's actual name
        userName = fetchUserName() // Dynamically fetch the username

        // Set up RecyclerView
        messageAdapter = MessageAdapter(messages)
        messageList.layoutManager = LinearLayoutManager(this)
        messageList.adapter = messageAdapter

        // Load messages for the user
        loadMessages()

        // Send a new message
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
            // If no username is found, redirect to login
            Log.e("ChatActivity", "No logged-in username found. Redirecting to login.")
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        return loggedInUsername ?: "Unknown User" // Fallback, but should rarely occur
    }



    private fun loadMessages() {
        val chatRef = firestore.collection("chats").document(userName)

        chatRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                messages.clear()

                // Extract, sort, and parse the messages
                val sortedEntries = document.data?.entries
                    ?.sortedBy { it.key.substringAfterLast('_').toIntOrNull() } // Sort by the numeric suffix

                sortedEntries?.forEach { entry ->
                    val sender = if (entry.key.startsWith("user_message")) "User" else "Admin"
                    val text = entry.value.toString()
                    messages.add(Message(sender = sender, text = text))
                }

                // Update adapter and scroll to bottom
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
            if (document.exists()) {
                // Document exists: Add the new message
                val messageCount = document.data?.keys
                    ?.filter { it.startsWith("user_message") || it.startsWith("admin_message") }
                    ?.size ?: 0 // Count all existing messages

                val newMessageKey = "user_message_${messageCount + 1}"

                val updateData = mapOf(newMessageKey to userMessage)

                chatRef.update(updateData)
                    .addOnSuccessListener {
                        Log.d("ChatActivity", "Message sent successfully")
                        loadMessages() // Reload messages after sending
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatActivity", "Error sending message", e)
                    }
            } else {
                // Document doesn't exist: Create it with the first message
                val initialData = mapOf("user_message_1" to userMessage)

                chatRef.set(initialData)
                    .addOnSuccessListener {
                        Log.d("ChatActivity", "New document created, and message sent successfully")
                        loadMessages() // Reload messages after sending
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatActivity", "Error creating new document", e)
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("ChatActivity", "Error accessing chat document", e)
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
