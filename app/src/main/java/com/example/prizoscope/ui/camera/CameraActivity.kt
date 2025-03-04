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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.prizoscope.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.prizoscope.ui.shopping.ShoppingActivity
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.chat.ChatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.ByteArrayOutputStream
import java.io.File
import com.example.prizoscope.api.RetrofitClient
import com.example.prizoscope.api.ObjectDetectionResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                // Request camera permission when the surface is available
                requestCameraPermission()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastScanTime > 5000) { // Process frames every 5 seconds
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
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        try {
            if (cameraManager.cameraIdList.isEmpty()) {
                Log.e(TAG, "No cameras available.")
                Toast.makeText(this, "No cameras available.", Toast.LENGTH_SHORT).show()
                return
            }

            val cameraId = cameraManager.cameraIdList[0] // Assuming back camera
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(SurfaceTexture::class.java)

            // Choose the optimal preview size
            val previewSize = sizes?.maxByOrNull { it.width * it.height } ?: Size(1920, 1080)

            // Adjust TextureView layout to match the preview size
            val layoutParams = textureView.layoutParams as FrameLayout.LayoutParams
            layoutParams.width = previewSize.width
            layoutParams.height = previewSize.height
            layoutParams.gravity = Gravity.CENTER
            textureView.layoutParams = layoutParams

            // Open the camera
            cameraManager.openCamera(
                cameraId,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        val surfaceTexture = textureView.surfaceTexture
                        if (surfaceTexture == null) {
                            Log.e(TAG, "SurfaceTexture is null.")
                            return
                        }

                        // Set the default buffer size
                        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)

                        // Create a Surface for the preview
                        val surface = Surface(surfaceTexture)

                        // Create a capture request
                        val captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        captureRequest.addTarget(surface)

                        // Create a camera capture session
                        cameraDevice.createCaptureSession(
                            listOf(surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    session.setRepeatingRequest(captureRequest.build(), null, null)
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    Log.e(TAG, "Camera capture session configuration failed.")
                                }
                            },
                            handler
                        )
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        Log.e(TAG, "Camera device disconnected.")
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.e(TAG, "Camera device error: $error")
                    }
                },
                handler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera: ${e.message}")
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Request the camera permission
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 101)
        } else {
            // Permission already granted, open the camera
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, open the camera
            openCamera()
        } else {
            // Permission denied, show a message to the user
            Toast.makeText(this, "Camera permission is required to use this feature.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processFrame(bitmap: Bitmap) {
        try {
            processObjectDetection(bitmap)
            processTextRecognition(bitmap) { detectedText ->
                if (detectedText.isNotEmpty()) {
                    matchTextWithFirestore(detectedText) { matchedItem ->
                        if (matchedItem != null) {
                            redirectToShopping(matchedItem)
                        } else if (isKeyboardDetected(detectedText)) {
                            redirectToShopping("Keyboard")
                        } else {
                            Log.d(TAG, "No matching item found in Firestore.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in processFrame: ${e.message}")
            Toast.makeText(this, "Error processing frame: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            bitmap.recycle() // Recycle the bitmap to free memory
        }
    }

    private fun processObjectDetection(bitmap: Bitmap) {
        try {
            // Convert bitmap to byte array
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            // Create a file from the byte array
            val file = File.createTempFile("image", ".jpg", cacheDir)
            file.writeBytes(byteArray)

            // Create a MultipartBody.Part
            val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

            // Send the image to the API
            RetrofitClient.instance.detectObjects(body).enqueue(object : Callback<ObjectDetectionResponse> {
                override fun onResponse(call: Call<ObjectDetectionResponse>, response: Response<ObjectDetectionResponse>) {
                    if (response.isSuccessful) {
                        val detectedObjects = response.body()?.objects
                        if (!detectedObjects.isNullOrEmpty()) {
                            val bestMatch = detectedObjects.maxByOrNull { it.confidence }
                            if (bestMatch != null && bestMatch.confidence > 0.7) {
                                Log.d(TAG, "Detected object: ${bestMatch.label} with confidence ${bestMatch.confidence}")
                                redirectToShopping(bestMatch.label)
                            } else {
                                Log.w(TAG, "Low confidence. Falling back to OCR.")
                                Toast.makeText(this@CameraActivity, "Low confidence. Trying text recognition.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.w(TAG, "No objects detected.")
                            Toast.makeText(this@CameraActivity, "No objects detected.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e(TAG, "API call failed: ${response.errorBody()?.string()}")
                        Toast.makeText(this@CameraActivity, "API call failed.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ObjectDetectionResponse>, t: Throwable) {
                    Log.e(TAG, "API call failed: ${t.message}")
                    Toast.makeText(this@CameraActivity, "API call failed: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error in processObjectDetection: ${e.message}")
            Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
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

                if (filteredText.isNotEmpty()) {
                    if (isKeyboardDetected(filteredText)) {
                        Log.d(TAG, "Detected as a keyboard based on OCR.")
                        redirectToShopping("Keyboard")
                    } else if (isMouseDetected(filteredText)) {
                        Log.d(TAG, "Detected as a mouse based on OCR.")
                        redirectToShopping("Mouse")
                    } else {
                        onResult(filteredText) // Pass the filtered text for further processing
                    }
                } else {
                    Log.w(TAG, "No text detected.")
                    onResult("") // Return an empty result if no text is detected
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Text recognition failed: ${e.message}")
                onResult("") // Handle the failure gracefully
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
        val matches = keywords.filter { text.contains(it, ignoreCase = true) }

        Log.d(TAG, "Mouse Detection - Matched Keywords: $matches") // Debugging

        // Require at least 2 matching keywords
        return matches.size >= 2
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
        return inputWords.intersect(targetWords).size.toDouble() / targetWords.size
    }

    private fun redirectToShopping(searchTerm: String) {
        Log.d(TAG, "Redirecting to ShoppingActivity with term: $searchTerm") // Debug log
        val intent = Intent(this, ShoppingActivity::class.java).apply {
            putExtra("search_term", searchTerm) // Pass the detected search term
            putExtra("auto_search", true)      // Enable auto-search in ShoppingActivity
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