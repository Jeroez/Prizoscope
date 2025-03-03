package com.example.prizoscope.ui.chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Message
import com.example.prizoscope.ui.auth.Login
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.camera.CameraActivity
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.shopping.ShoppingActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var userName: String
    private var messagesListener: ListenerRegistration? = null
    private var currentChatId: String = ""
    private var currentAdmin: String = ""
    private var currentMessageNumber = 0L
    private lateinit var drawerLayout: DrawerLayout

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadImageToStorage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        userName = fetchUserName()

        drawerLayout = findViewById(R.id.drawer_layout)
        val sharedPreferences = getSharedPreferences("chat_prefs", MODE_PRIVATE)

        // Set BTech as default admin
        currentAdmin = sharedPreferences.getString("last_admin", "BTech") ?: "BTech"
        sharedPreferences.edit().putString("last_admin", currentAdmin).apply()

        val isFromShopping = intent.getBooleanExtra("from_shopping", false)
        val selectedAdminFromShopping = intent.getStringExtra("admin_name")

        if (isFromShopping && !selectedAdminFromShopping.isNullOrEmpty()) {
            currentAdmin = selectedAdminFromShopping
            sharedPreferences.edit().putString("last_admin", currentAdmin).apply()
            openAdminChat(currentAdmin)
        } else {
            openAdminChat(currentAdmin)
            loadAdmins()
        }

        setupUI()
        setupBottomNav()
    }

    private fun setupUI() {
        findViewById<Button>(R.id.openDrawerBtn).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<ImageButton>(R.id.btn_attach).setOnClickListener {
            openImagePicker()
        }

        findViewById<Button>(R.id.btn_send).setOnClickListener {
            val userMessage = findViewById<EditText>(R.id.input_message).text.toString()
            if (userMessage.isNotBlank()) {
                sendMessage(userMessage)
                findViewById<EditText>(R.id.input_message).text.clear()
            }
        }
    }

    private fun loadAdmins() {
        firestore.collection("admins").get()
            .addOnSuccessListener { documents ->
                val adminList = mutableListOf<String>()
                documents.forEach { document ->
                    val username = document.getString("username") ?: document.id
                    adminList.add(username)
                }

                val recyclerView = findViewById<RecyclerView>(R.id.admin_list)
                recyclerView.layoutManager = LinearLayoutManager(this)
                recyclerView.adapter = AdminAdapter(adminList) { selectedAdmin ->
                    currentAdmin = selectedAdmin
                    getSharedPreferences("chat_prefs", MODE_PRIVATE).edit()
                        .putString("last_admin", currentAdmin)
                        .apply()
                    openAdminChat(currentAdmin)
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
            }
    }


    // REST OF THE CODE REMAINS EXACTLY THE SAME AS YOUR ORIGINAL VERSION
    // Only modified sections are shown above, everything below stays unchanged

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePicker.launch(intent)
    }

    private fun uploadImageToStorage(uri: Uri) {
        val storageRef = storage.reference.child("chat_images/${UUID.randomUUID()}")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    sendMessage(downloadUri.toString(), isImage = true)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUserName(): String {
        return getSharedPreferences("user_session", MODE_PRIVATE).getString("username", null) ?: run {
            startActivity(Intent(this, Login::class.java))
            finish()
            "Unknown"
        }
    }

    private fun openAdminChat(adminName: String) {
        if (adminName.isEmpty()) {
            findViewById<LinearLayout>(R.id.inputContainer).visibility = View.GONE
            return
        }
        findViewById<LinearLayout>(R.id.inputContainer).visibility = View.VISIBLE

        currentChatId = "$userName | $adminName"
        messagesListener?.remove()

        firestore.collection("chats").document(currentChatId).get()
            .addOnSuccessListener { document ->
                currentMessageNumber = document.data?.keys?.size?.toLong() ?: 0L
            }

        messageAdapter = MessageAdapter(messages) { url ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        findViewById<RecyclerView>(R.id.message_list).apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = messageAdapter
        }

        messagesListener = firestore.collection("chats").document(currentChatId)
            .addSnapshotListener { snapshot, error ->
                snapshot?.data?.let { data ->
                    messages.clear()
                    data.entries.sortedBy { it.key }.forEach { (key, value) ->
                        val msg = value as Map<*, *>
                        messages.add(Message(
                            sender = msg["sender"].toString(),
                            text = msg["content"]?.toString(),
                            imageUrl = if (msg["type"] == "image") msg["content"]?.toString() else null,
                            timestamp = (msg["timestamp"] as? Long) ?: 0L,
                            adminUsername = null,
                            adminStore = null,
                            isSuperAdmin = null
                        ))
                    }
                    messageAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun sendMessage(content: String, isImage: Boolean = false) {
        val timestamp = System.currentTimeMillis()
        val messageKey = "message_${currentMessageNumber++}"
        val messageData = hashMapOf(
            "content" to content,
            "sender" to "user",
            "timestamp" to timestamp,
            "type" to if (isImage) "image" else "text"
        )

        firestore.collection("chats").document(currentChatId)
            .update(messageKey, messageData)
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.selectedItemId = R.id.nav_chat

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
                R.id.nav_chat -> true
                else -> false
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