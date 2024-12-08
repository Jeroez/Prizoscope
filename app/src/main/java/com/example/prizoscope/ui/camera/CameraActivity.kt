package com.example.prizoscope.ui.camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
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
import kotlin.math.sqrt
import android.view.View
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.maps.MapActivity
import com.example.prizoscope.ui.shopping.ShoppingActivity
import com.example.prizoscope.ui.settings.SettingsActivity

class CameraActivity : AppCompatActivity() {

    private lateinit var albumButton: ImageButton
    private lateinit var captureButton: ImageButton
    private lateinit var previewImageView: ImageView
    private lateinit var priceTextView: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private var bitmap: Bitmap? = null

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Initialize UI elements
        albumButton = findViewById(R.id.btn_album)
        captureButton = findViewById(R.id.btn_capture)
        previewImageView = findViewById(R.id.preview_image_view)
        priceTextView = findViewById(R.id.price_text_view)
        bottomNavigationView = findViewById(R.id.bottom_nav)

        setupLaunchers()
        setupBottomNav()

        // Button listeners
        captureButton.setOnClickListener { openCamera() }
        albumButton.setOnClickListener { openGallery() }
    }

    private fun setupLaunchers() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                handleImageResult(imageBitmap)
            } else {
                Toast.makeText(this, "Camera operation cancelled", Toast.LENGTH_SHORT).show()
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                uri?.let { loadImageFromUri(it) }
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
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
        try {
            val contentResolver = applicationContext.contentResolver
            val imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            handleImageResult(imageBitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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
                Log.d("TextRecognition", "Recognized text: $recognizedText")
                if (recognizedText.isNotEmpty()) {
                    searchInCSV(recognizedText)
                } else {
                    Toast.makeText(this, "No text detected in the image", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to recognize text: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun searchInCSV(query: String) {
        try {
            val csvInputStream = assets.open("items.csv")
            val reader = BufferedReader(InputStreamReader(csvInputStream))
            val queryProcessed = preprocessText(query)

            // Define main terms
            val mainTerms = setOf("monitor", "cpu", "gpu", "headphone", "headset", "ram", "hdd", "ssd", "keyboard", "mouse")

            val detectedMainTerm = mainTerms.find { queryProcessed.contains(it) }
            val filteredRows = mutableListOf<List<String>>()
            val allRows = mutableListOf<List<String>>()

            reader.forEachLine { line ->
                val columns = line.split(",").map { it.trim() }
                if (columns.size >= 6) {
                    val itemName = preprocessText(columns[1])
                    allRows.add(columns)

                    if (detectedMainTerm != null && itemName.contains(detectedMainTerm)) {
                        filteredRows.add(columns)
                    }
                }
            }
            reader.close()

            val searchSpace = if (filteredRows.isNotEmpty()) filteredRows else allRows
            val bestMatch = searchSpace.maxByOrNull { row ->
                val itemName = preprocessText(row[1])
                calculateCosineSimilarity(queryProcessed, itemName)
            }

            if (bestMatch != null) {
                val price = bestMatch[2]
                val url = bestMatch[4]
                updatePriceText(price, url)
            } else {
                updatePriceText(null, null)
                Toast.makeText(this, "No matching item found", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error reading CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun preprocessText(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun calculateCosineSimilarity(query: String, itemName: String): Double {
        val queryWords = query.split(" ")
        val itemWords = itemName.split(" ")

        val allWords = (queryWords + itemWords).toSet()
        val queryVector = allWords.map { word -> queryWords.count { it == word } }
        val itemVector = allWords.map { word -> itemWords.count { it == word } }

        val dotProduct = queryVector.zip(itemVector).sumOf { it.first * it.second }
        val magnitudeQuery = sqrt(queryVector.sumOf { it * it.toDouble() })
        val magnitudeItem = sqrt(itemVector.sumOf { it * it.toDouble() })

        return if (magnitudeQuery > 0 && magnitudeItem > 0) {
            dotProduct / (magnitudeQuery * magnitudeItem)
        } else 0.0
    }

    @SuppressLint("SetTextI18n")
    private fun updatePriceText(price: String?, itemUrl: String?) {
        if (price != null) {
            priceTextView.text = "Price: ₱ $price"
            priceTextView.visibility = View.VISIBLE
            priceTextView.setOnClickListener {
                itemUrl?.let { url ->
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(browserIntent)
                } ?: run {
                    Toast.makeText(this, "No URL available for this item", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            priceTextView.visibility = View.GONE
        }
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
