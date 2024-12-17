package com.example.prizoscope.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.prizoscope.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.prizoscope.ui.shopping.ShoppingActivity
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.chat.ChatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.common.FileUtil

class CameraActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraManager: CameraManager
    private lateinit var handler: Handler
    private lateinit var firestore: FirebaseFirestore
    private lateinit var labels: List<String>
    private lateinit var interpreter: Interpreter
    private val paint = Paint()
    private val TAG = "CameraActivity"
    private var lastScanTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        textureView = findViewById(R.id.textureView)
        firestore = FirebaseFirestore.getInstance()

        // Initialize TensorFlow Lite model and labels
        labels = FileUtil.loadLabels(this, "labels.txt")
        val model = FileUtil.loadMappedFile(this, "model.tflite")
        interpreter = Interpreter(model)

        // Camera setup
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager


        setupBottomNav()

        val handlerThread = HandlerThread("CameraThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastScanTime > 5000) { // Scan every 5 seconds
                    lastScanTime = currentTime
                    val bitmap = textureView.bitmap ?: return
                    processFrame(bitmap)
                }
            }
        }

        requestCameraPermission()
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        cameraManager.openCamera(
            cameraManager.cameraIdList[0],
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    val surfaceTexture = textureView.surfaceTexture
                    val surface = Surface(surfaceTexture)

                    val captureRequest =
                        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    captureRequest.addTarget(surface)

                    cameraDevice.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.setRepeatingRequest(captureRequest.build(), null, null)
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {}
                        },
                        handler
                    )
                }

                override fun onDisconnected(camera: CameraDevice) {}
                override fun onError(camera: CameraDevice, error: Int) {}
            },
            handler
        )
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    private fun processFrame(bitmap: Bitmap) {
        processTextRecognition(bitmap) { detectedText ->
            if (detectedText.isNotEmpty()) {
                matchTextWithFirestore(detectedText) { matchedItem ->
                    if (matchedItem != null) {
                        redirectToShopping(matchedItem)
                    } else {
                        // If no text match, fallback to object detection
                        processObjectDetection(bitmap)
                    }
                }
            } else {
                processObjectDetection(bitmap)
            }
        }
    }

    private fun processTextRecognition(bitmap: Bitmap, onResult: (String) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { result ->
                onResult(result.text)
            }
            .addOnFailureListener {
                Log.e(TAG, "Text recognition failed: ${it.message}")
                onResult("")
            }
    }

    private fun matchTextWithFirestore(text: String, onResult: (String?) -> Unit) {
        firestore.collection("items")
            .get()
            .addOnSuccessListener { documents ->
                var bestMatch: String? = null
                var highestMatchScore = 0.0

                for (doc in documents) {
                    val itemName = doc.getString("name") ?: ""
                    val matchScore = calculateMatchScore(text, itemName)
                    if (matchScore > 0.5 && matchScore > highestMatchScore) {
                        highestMatchScore = matchScore
                        bestMatch = itemName
                    }
                }
                onResult(bestMatch)
            }
            .addOnFailureListener {
                Log.e(TAG, "Firestore query failed: ${it.message}")
                onResult(null)
            }
    }

    private fun calculateMatchScore(input: String, target: String): Double {
        val inputWords = input.lowercase().split(" ")
        val targetWords = target.lowercase().split(" ")

        val commonWords = inputWords.intersect(targetWords)
        return commonWords.size.toDouble() / targetWords.size
    }

    private fun processObjectDetection(bitmap: Bitmap) {
        // Preprocess the bitmap to match the input dimensions of the TensorFlow model
        val tensorImage = TensorImage.fromBitmap(bitmap)

        // Resize the image to the dimensions expected by the model (e.g., 300x300)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        val processedImage = imageProcessor.process(tensorImage)

        // Prepare the input buffer
        val inputBuffer = processedImage.buffer

        // Prepare an output array for model inference
        val output = Array(1) { FloatArray(labels.size) } // Assuming one output array per label

        // Run inference using the interpreter
        interpreter.run(inputBuffer, output)

        // Find the label with the highest confidence
        val detectedIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1
        val confidence = output[0][detectedIndex]
        if (confidence > 0.5) {
            val label = labels[detectedIndex]
            Log.d(TAG, "Detected object: $label with confidence $confidence")
            matchLabelWithFirestore(label)
        } else {
            Toast.makeText(this, "Unable to scan an item.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun matchLabelWithFirestore(label: String) {
        firestore.collection("items")
            .whereEqualTo("type", label)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Unavailable", Toast.LENGTH_SHORT).show()
                } else {
                    showViewSimilarPrompt(label)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Firestore query failed: ${it.message}")
            }
    }

    private fun showViewSimilarPrompt(label: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Similar items found")
            .setMessage("Would you like to view similar items?")
            .setPositiveButton("Yes") { _, _ ->
                redirectToShopping(label)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun redirectToShopping(searchTerm: String) {
        val intent = Intent(this, ShoppingActivity::class.java).apply {
            putExtra("search_term", searchTerm)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        interpreter.close()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.selectedItemId = R.id.nav_camera

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_camera -> true
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