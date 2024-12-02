package com.example.prizoscope.ui.camera

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.prizoscope.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.BufferedReader
import java.io.InputStreamReader
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.maps.MapActivity
import com.example.prizoscope.ui.shopping.ShoppingActivity
import com.example.prizoscope.ui.bookmarks.BookmarkActivity

class CameraActivity : AppCompatActivity() {

    private lateinit var albumButton: ImageButton
    private lateinit var captureButton: ImageButton
    private lateinit var previewImageView: ImageView
    private lateinit var bottomNavigationView: BottomNavigationView
    private var bitmap: Bitmap? = null

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        albumButton = findViewById(R.id.btn_album)
        captureButton = findViewById(R.id.btn_capture)
        previewImageView = findViewById(R.id.preview_image_view)
        bottomNavigationView = findViewById(R.id.bottom_nav)

        setupLaunchers()
        setupBottomNav()

        captureButton.setOnClickListener { openCamera() }
        albumButton.setOnClickListener { openGallery() }
    }

    private fun setupLaunchers() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                handleImageResult(imageBitmap)
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                uri?.let { loadImageFromUri(it) }
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun loadImageFromUri(uri: Uri) {
        val contentResolver = applicationContext.contentResolver
        val imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        handleImageResult(imageBitmap)
    }

    private fun handleImageResult(imageBitmap: Bitmap) {
        bitmap = imageBitmap
        previewImageView.setImageBitmap(imageBitmap)
        performOCR(imageBitmap)
    }

    private fun performOCR(imageBitmap: Bitmap) {
        val image = InputImage.fromBitmap(imageBitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val recognizedText = visionText.text
                searchInCSV(recognizedText)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to recognize text: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun searchInCSV(query: String) {
        val csvInputStream = assets.open("items.csv")
        val reader = BufferedReader(InputStreamReader(csvInputStream))
        var foundItem: String? = null

        reader.forEachLine { line ->
            val columns = line.split(",")
            if (columns.size < 2) return@forEachLine
            val itemName = columns[0]
            val itemPrice = columns[1]
            if (itemName.contains(query, ignoreCase = true)) {
                foundItem = "Name: $itemName\nPrice: $itemPrice"
                return@forEachLine
            }
        }

        foundItem?.let {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        } ?: Toast.makeText(this, "No matching item found", Toast.LENGTH_LONG).show()
    }

    private fun setupBottomNav() {
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_camera -> true
                R.id.nav_shopping -> {
                    startActivity(Intent(this, ShoppingActivity::class.java))
                    true
                }
                R.id.nav_bookmarks -> {
                    startActivity(Intent(this, BookmarkActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.nav_maps -> {
                    startActivity(Intent(this, MapActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
