package com.example.prizoscope.ui.camera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Item
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.chat.ChatActivity
import com.example.prizoscope.ui.shopping.ShoppingActivity
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.prizoscope.ui.settings.SettingsActivity

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var priceTagView: TextView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var recognizer: TextRecognizer
    private lateinit var firestore: FirebaseFirestore

    private val TAG = "CameraActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Initialize Views
        previewView = findViewById(R.id.preview_image_view)
        priceTagView = findViewById(R.id.price_text_view)
        priceTagView.visibility = View.GONE // Hide price tag initially

        // Initialize Firebase and ML Kit
        firestore = FirebaseFirestore.getInstance()
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Initialize Camera Executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Start Camera Immediately
        startCamera()

        // Enable Price Tag Movement
        setupPriceTagMovement()
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Configure Camera Preview
            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Configure Image Analysis for real-time text recognition
            val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // Attach the analyzer
            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val detectedText = visionText.text
                            if (detectedText.isNotEmpty()) {
                                searchFirestoreForItem(detectedText)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Text recognition failed: ${e.message}")
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                }
            }

            // Bind Preview and Analysis to the lifecycle
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Camera binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun searchFirestoreForItem(text: String) {
        firestore.collection("items")
            .whereEqualTo("name", text) // Adjust query logic as needed
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val item = documents.documents.first().toObject(Item::class.java)
                    if (item != null) {
                        updatePriceTag(item)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore query failed: ${e.message}")
            }
    }

    private fun updatePriceTag(item: Item) {
        priceTagView.visibility = View.VISIBLE

        // Determine the price to display
        val finalPrice = if (item.discount_price.isNullOrEmpty()) {
            item.discount_price
        } else {
            item.price
        }

        // Update the price tag with the item details
        val promotionMessage = if (item.discount_price.isNullOrEmpty()) {
            "\n(Promotion! Original: ₱${item.price})"
        } else {
            ""
        }

        priceTagView.text = """
        Name: ${item.name}
        Price: ₱$finalPrice$promotionMessage
        Rating: ${item.rating} ★
    """.trimIndent()
    }



    private fun setupPriceTagMovement() {
        priceTagView.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_MOVE -> {
                    view.x = motionEvent.rawX - view.width / 2
                    view.y = motionEvent.rawY - view.height / 2
                }
            }
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.selectedItemId = R.id.nav_camera

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_camera -> true // Stay on this activity
                R.id.nav_shopping -> {
                    Log.d("Navigation", "Switching to Shopping")
                    startActivity(Intent(this, ShoppingActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_bookmarks -> {
                    Log.d("Navigation", "Switching to Bookmarks")
                    startActivity(Intent(this, BookmarkActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    Log.d("Navigation", "Switching to Settings")
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_chat -> {
                    Log.d("Navigation", "Switching to Chat")
                    startActivity(Intent(this, ChatActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
