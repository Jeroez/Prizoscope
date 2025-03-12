package com.example.prizoscope.ui.chat

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

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
            openImagePicker() // Open gallery
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
                    onAdminSelected(selectedAdmin)
                }
            }
    }
    private fun updateAdminDisplay() {
        findViewById<TextView>(R.id.currentAdminText).text = currentAdmin
    }
    private fun checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), 100)
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 100)
        }
    }


    // Call this function whenever admin is changed
    private fun onAdminSelected(admin: String) {
        currentAdmin = admin
        getSharedPreferences("chat_prefs", MODE_PRIVATE).edit()
            .putString("last_admin", currentAdmin)
            .apply()

        currentChatId = "$userName | $currentAdmin"
        updateAdminDisplay()

        firestore.collection("chats").document(currentChatId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    openAdminChat(currentAdmin)
                } else {
                    firestore.collection("chats").document(currentChatId)
                        .set(hashMapOf<String, Any>())
                        .addOnSuccessListener {
                            openAdminChat(currentAdmin)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error creating chat: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error checking chat: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        drawerLayout.closeDrawer(GravityCompat.START)
    }

    // Only modified sections are shown above, everything below stays unchanged

    private fun openImagePicker() {
        checkAndRequestStoragePermission() // Ensure permissions before opening gallery

        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePicker.launch(intent)
    }




    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadImageToDiscord(uri) // ✅ Use Discord instead
            }
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
                        if (value is Map<*, *>) {
                            messages.add(Message(
                                sender = value["sender"].toString(),
                                text = value["content"]?.toString(),
                                imageUrl = if (value["type"] == "image") value["content"]?.toString() else null,
                                timestamp = (value["timestamp"] as? Long) ?: 0L,
                                adminUsername = null,
                                adminStore = null,
                                isSuperAdmin = null
                            ))
                        } else {
                            Log.e("ChatActivity", "Unexpected Firestore data format for message: $value")
                        }
                    }
                    messageAdapter.notifyDataSetChanged()
                }
            }

    }

    private fun sendMessage(content: String, isImage: Boolean = false) {
        val timestamp = System.currentTimeMillis()
        val messageKey = "message_${currentMessageNumber++}"

        val messageData = hashMapOf(
            "content" to content,  // This will be the image URL if it's an image
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
    fun getWebhookUrl(callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("settings").document("discord")

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val webhookUrl = document.getString("webhook_url")
                    callback(webhookUrl)  // Return webhook URL
                } else {
                    callback(null)  // No URL found
                }
            }
            .addOnFailureListener { e ->
                callback(null)  // Error fetching data
            }
    }
    private fun uploadImageToDiscord(uri: Uri) {
        getWebhookUrl { webhookUrl ->
            if (webhookUrl == null) {
                Log.e("DiscordUpload", "Webhook URL not found")
                Toast.makeText(this, "Webhook URL error", Toast.LENGTH_SHORT).show()
                return@getWebhookUrl
            }

            val filePath = getRealPathFromURI(uri)
            if (filePath == null) {
                Log.e("DiscordUpload", "Failed to get file path")
                Toast.makeText(this, "Error: Cannot access file", Toast.LENGTH_SHORT).show()
                return@getWebhookUrl
            }

            val file = File(filePath)
            if (!file.exists() || !file.canRead()) {
                Log.e("DiscordUpload", "File does not exist or cannot be read: ${file.path}")
                Toast.makeText(this, "File error: Image not found or unreadable", Toast.LENGTH_SHORT).show()
                return@getWebhookUrl
            }


            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                .build()

            val request = Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("DiscordUpload", "Upload failed: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(this@ChatActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    Log.d("DiscordUpload", "Response: $responseBody")

                    try {
                        val jsonResponse = JSONObject(responseBody ?: "{}")
                        val imageUrl = jsonResponse.getJSONArray("attachments").getJSONObject(0).getString("url")

                        runOnUiThread {
                            sendMessage(imageUrl, isImage = true)
                        }
                    } catch (e: Exception) {
                        Log.e("DiscordUpload", "Error parsing response: ${e.message}")
                        runOnUiThread {
                            Toast.makeText(this@ChatActivity, "Upload failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }



    private fun getRealPathFromURI(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(columnIndex)
        }
        return null
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