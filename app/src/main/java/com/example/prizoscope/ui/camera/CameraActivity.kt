package com.example.prizoscope.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
import android.view.Gravity
import android.util.Size
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
import org.tensorflow.lite.support.common.ops.NormalizeOp

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

        labels = FileUtil.loadLabels(this, "labels.txt")
        val model = FileUtil.loadMappedFile(this, "model.tflite")
        interpreter = Interpreter(model)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        setupBottomNav()

        val handlerThread = HandlerThread("CameraThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                val previewSize = getOptimalPreviewSize(cameraManager, width, height)
                val layoutParams = textureView.layoutParams as FrameLayout.LayoutParams
                layoutParams.width = previewSize.width
                layoutParams.height = previewSize.height
                layoutParams.gravity = Gravity.CENTER
                textureView.layoutParams = layoutParams
                openCamera()
            }

            private fun getOptimalPreviewSize(cameraManager: CameraManager, width: Int, height: Int): Size {
                val cameraId = cameraManager.cameraIdList[0] // Assuming back camera
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val sizes = map?.getOutputSizes(SurfaceTexture::class.java)

                val targetRatio = width.toFloat() / height
                return sizes?.minByOrNull {
                    val ratio = it.width.toFloat() / it.height
                    Math.abs(ratio - targetRatio)
                } ?: Size(width, height)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastScanTime > 5000) {
                    lastScanTime = currentTime
                    val bitmap = textureView.bitmap
                    if (bitmap == null) {
                        Log.e(TAG, "Bitmap is null.")
                        return
                    }
                    Log.d(TAG, "Captured bitmap size: ${bitmap.width}x${bitmap.height}")
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

                    val captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
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
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    private fun processFrame(bitmap: Bitmap) {
        processObjectDetection(bitmap) // Attempt object detection first
        processTextRecognition(bitmap) { detectedText ->
            if (detectedText.isNotEmpty()) {
                matchTextWithFirestore(detectedText) { matchedItem ->
                    if (matchedItem != null) {
                        redirectToShopping(matchedItem)
                    } else if (isKeyboardDetected(detectedText)) {
                        redirectToShopping("Keyboard")
                    } else if (isMouseDetected(detectedText)) {
                        redirectToShopping("Mouse")
                    } else {
                        Toast.makeText(this, "No matching items found.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }


    private fun processTextRecognition(bitmap: Bitmap, onResult: (String) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { result ->
                val filteredText = result.textBlocks
                    .joinToString(" ") { it.text }
                    .filter { it.isLetterOrDigit() || it.isWhitespace() }
                    .trim()

                Log.d(TAG, "Filtered OCR text: $filteredText")

                if (isKeyboardDetected(filteredText)) {
                    Log.d(TAG, "Detected as a keyboard based on OCR.")
                    redirectToShopping("Keyboard")
                } else if (isMouseDetected(filteredText)) {
                    Log.d(TAG, "Detected as a mouse based on OCR.")
                    redirectToShopping("Mouse")
                } else {
                    onResult(filteredText)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Text recognition failed: ${it.message}")
                onResult("")
            }
    }


    private fun isKeyboardDetected(text: String): Boolean {
        val keywords = listOf("Shift", "Ctrl", "Alt", "CapsLk", "Tab", "Enter", "Backspace")
        val matches = keywords.count { text.contains(it, ignoreCase = true) }
        val textLength = text.length
        val density = if (textLength > 0) text.split(" ").size.toDouble() / textLength else 0.0

        return matches >= 3 && density > 0.05
    }

    private fun isMouseDetected(text: String): Boolean {
        val keywords = listOf("Logitech", "DPI", "Wireless", "Gaming Mouse", "Mouse")
        return keywords.any { text.contains(it, ignoreCase = true) }
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
        try {
            // Step 1: Preprocess the input image
            val inputShape = interpreter.getInputTensor(0).shape() // [1, 224, 224, 3]
            val inputDataType = interpreter.getInputTensor(0).dataType()

            // Create TensorImage with the expected data type
            val tensorImage = TensorImage(inputDataType)
            tensorImage.load(bitmap)

            // Resize and normalize the image
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR)) // Resize to 224x224
                .add(NormalizeOp(0f, 1f)) // Normalize pixel values to [0, 1]
                .build()
            val processedImage = imageProcessor.process(tensorImage)

            // Step 2: Prepare the output buffer
            val outputShape = interpreter.getOutputTensor(0).shape() // e.g., [1, 35]
            val outputBuffer = Array(outputShape[0]) { FloatArray(outputShape[1]) }

            // Step 3: Run inference
            interpreter.run(processedImage.buffer, outputBuffer)

            // Step 4: Interpret the results
            val detectedIndex = outputBuffer[0].indices.maxByOrNull { outputBuffer[0][it] } ?: -1
            val confidence = outputBuffer[0][detectedIndex]

            if (confidence > 0.3) {
                val label = labels[detectedIndex]
                Log.d(TAG, "Detected object: $label with confidence $confidence")
                redirectToShopping(label)
            } else {
                Toast.makeText(this, "Unable to identify the item.", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during object detection: ${e.message}")
            Toast.makeText(this, "Inference failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun matchLabelWithFirestore(label: String) {
        firestore.collection("items")
            .whereEqualTo("type", label)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "No matching items found.", Toast.LENGTH_SHORT).show()
                } else {
                    showViewSimilarPrompt(label)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to search Firestore.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showViewSimilarPrompt(label: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Similar items found")
            .setMessage("Would you like to view similar items?")
            .setPositiveButton("Yes") { _, _ -> redirectToShopping(label) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun redirectToShopping(searchTerm: String) {
        val intent = Intent(this, ShoppingActivity::class.java).apply {
            putExtra("search_term", searchTerm)
            putExtra("auto_search", true)
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
