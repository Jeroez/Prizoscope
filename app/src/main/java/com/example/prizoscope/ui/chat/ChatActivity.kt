package com.example.prizoscope.ui.chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Message
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.prizoscope.ui.auth.Login
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.camera.CameraActivity
import com.example.prizoscope.ui.shopping.ShoppingActivity
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class ChatActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var userName: String
    private var messagesListener: ListenerRegistration? = null
    private var currentChatId: String = ""
    private var currentAdmin: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userName = fetchUserName()

        val sharedPreferences = getSharedPreferences("chat_prefs", MODE_PRIVATE)
        currentAdmin = sharedPreferences.getString("last_admin", "") ?: ""

        if (currentAdmin.isEmpty()) {
            // First time: Open the admin selector
            setContentView(R.layout.fragment_admin_selector)
            loadAdmins()
        } else {
            // If an admin was selected before, open the chat
            setContentView(R.layout.activity_chat)
            openAdminChat(currentAdmin)
        }

        // Setup "Select Admin" button
        findViewById<Button>(R.id.openDrawerBtn).setOnClickListener {
            val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        setupBottomNav()
    }

    private fun fetchUserName(): String {
        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        return sharedPreferences.getString("username", null) ?: run {
            startActivity(Intent(this, Login::class.java))
            finish()
            "Unknown"
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return true
    }

    private fun loadAdmins() {
        firestore.collection("admins").get()
            .addOnSuccessListener { result ->
                val adminList = result.documents.mapNotNull { it.getString("Store") }
                if (adminList.isNotEmpty()) {
                    runOnUiThread { setupAdminList(adminList) }
                } else {
                    Log.e("FirestoreDebug", "❌ No admins found!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDebug", "❌ Failed to load admins", e)
            }
    }

    private fun setupAdminList(adminList: List<String>) {
        val recyclerView = findViewById<RecyclerView>(R.id.admin_list)
        val adapter = AdminAdapter(adminList) { selectedAdmin ->
            currentAdmin = selectedAdmin
            getSharedPreferences("chat_prefs", MODE_PRIVATE).edit()
                .putString("last_admin", currentAdmin)
                .apply()
            openAdminChat(selectedAdmin)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }


    private fun openAdminChat(adminName: String) {
        setContentView(R.layout.activity_chat)

        val messageList = findViewById<RecyclerView>(R.id.message_list)
        val inputMessage = findViewById<EditText>(R.id.input_message)
        val btnSend = findViewById<Button>(R.id.btn_send)

        messageAdapter = MessageAdapter(messages)
        messageList.layoutManager = LinearLayoutManager(this)
        messageList.adapter = messageAdapter

        currentChatId = "$userName | $adminName"
        messagesListener?.remove()

        firestore.collection("chats").document(currentChatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatActivity", "Listen failed", error)
                    return@addSnapshotListener
                }
                snapshot?.let { doc ->
                    if (doc.exists()) {
                        processMessages(doc.data ?: emptyMap())
                    }
                }
            }

        btnSend.setOnClickListener {
            val userMessage = inputMessage.text.toString()
            if (userMessage.isNotBlank()) {
                sendMessage(userMessage)
                inputMessage.text.clear()
            }
        }

        setupBottomNav()
    }

    private fun processMessages(data: Map<String, Any>) {
        messages.clear()
        data.entries
            .sortedBy { it.key.substringAfterLast('_').toIntOrNull() }
            .forEach { (key, value) ->
                when {
                    key.startsWith("user_message") -> messages.add(
                        Message(sender = "User", text = value.toString())
                    )
                    key.startsWith("admin_message") -> messages.add(
                        Message(sender = "Admin", text = value.toString())
                    )
                }
            }
        messageAdapter.notifyDataSetChanged()
    }

    private fun sendMessage(userMessage: String) {
        if (currentAdmin.isEmpty()) {
            Toast.makeText(this, "Select an admin first", Toast.LENGTH_SHORT).show()
            return
        }
        val chatRef = firestore.collection("chats").document(currentChatId)
        chatRef.get().addOnSuccessListener { doc ->
            val messageCount = doc.data?.keys?.count { it.startsWith("user_message") || it.startsWith("admin_message") } ?: 0
            val newMessageKey = "user_message_${messageCount + 1}"
            if (doc.exists()) {
                chatRef.update(newMessageKey, userMessage)
                    .addOnFailureListener { e ->
                        Log.e("ChatActivity", "Error sending message", e)
                        Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
                    }
            } else {
                chatRef.set(mapOf(newMessageKey to userMessage))
                    .addOnFailureListener { e ->
                        Log.e("ChatActivity", "Error creating chat document", e)
                        Toast.makeText(this, "Failed to create chat", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.selectedItemId = R.id.nav_chat

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_camera -> {
                    navigateToActivity(CameraActivity::class.java)
                    true  // ✅ This ensures the function returns Boolean
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
                R.id.nav_chat -> true
                else -> false  // ✅ Return false for invalid cases
            }
        }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }



    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
    }
}
