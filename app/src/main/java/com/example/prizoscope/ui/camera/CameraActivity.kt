package com.example.prizoscope.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.prizoscope.BuildConfig
import com.example.prizoscope.R
import com.example.prizoscope.api.RetrofitClient
import com.example.prizoscope.api.VisionApiRequest
import com.example.prizoscope.api.VisionApiResponse
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.chat.ChatActivity
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.shopping.ShoppingActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class CameraActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraManager: CameraManager
    private lateinit var handler: Handler
    private lateinit var firestore: FirebaseFirestore
    private val TAG = "CameraActivity"
    private var lastScanTime = System.currentTimeMillis()
    private var lastRedirectTime = 0L
    private var lastDetectedObject: String? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        textureView = findViewById(R.id.textureView)
        firestore = FirebaseFirestore.getInstance()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        setupBottomNav()

        val handlerThread = HandlerThread("CameraThread").apply { start() }
        handler = Handler(handlerThread.looper)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                texture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                Log.d(TAG, "SurfaceTexture available - w: $width, h: $height")
                surfaceTexture = texture

                // Wait for layout to complete and validate dimensions
                textureView.post {
                    if (textureView.width > 0 && textureView.height > 0) {
                        Log.d(
                            TAG,
                            "Valid view dimensions - w: ${textureView.width}, h: ${textureView.height}"
                        )
                        if (hasCameraPermission()) {
                            openCamera()
                        }
                    } else {
                        Log.e(TAG, "Invalid view dimensions - retrying in 300ms")
                        textureView.postDelayed({
                            if (hasCameraPermission()) openCamera()
                        }, 300)
                    }
                }
            }

            override fun onSurfaceTextureSizeChanged(
                texture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                Log.d(TAG, "Surface texture size changed - w: $width, h: $height")
            }

            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                Log.d(TAG, "Surface texture destroyed")
                surfaceTexture = null
                return false
            }

            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
                Log.d(TAG, "Frame updated - Checking if scanning is running")

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastScanTime > 5000) { // Scan every 5 sec
                    lastScanTime = currentTime
                    textureView.bitmap?.let { bitmap ->
                        Log.d(TAG, "Processing frame for scanning...")
                        processFrame(bitmap)
                    } ?: Log.e(TAG, "Failed to capture bitmap from TextureView")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        if (surfaceTexture == null || textureView.width == 0 || textureView.height == 0) {
            showError("Camera preview not ready - retrying")
            Log.e(TAG, "SurfaceTexture is null or TextureView has invalid dimensions, retrying in 500ms")

            // Retry opening the camera after a short delay
            textureView.postDelayed({
                if (surfaceTexture != null && textureView.width > 0 && textureView.height > 0) {
                    openCamera()
                } else {
                    showError("Camera preview failed after retry")
                }
            }, 500)
            return
        }

        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: run {
                showError("No cameras available")
                return
            }

            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val previewSize = map?.getOutputSizes(SurfaceTexture::class.java)
                ?.maxByOrNull { it.width * it.height }
                ?: Size(1920, 1080)

            surfaceTexture!!.setDefaultBufferSize(previewSize.width, previewSize.height)
            surface = Surface(surfaceTexture!!)

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    cameraDevice = device
                    try {
                        val captureRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(surface!!)
                        }

                        device.createCaptureSession(
                            listOf(surface!!),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    try {
                                        session.setRepeatingRequest(
                                            captureRequest.build(),
                                            null,
                                            handler
                                        )
                                        Log.d(TAG, "Camera preview started successfully")
                                    } catch (e: CameraAccessException) {
                                        showError("Failed to start preview: ${e.message}")
                                    }
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    showError("Camera session configuration failed")
                                    device.close()
                                }
                            },
                            handler
                        )
                    } catch (e: CameraAccessException) {
                        showError("Camera access error: ${e.message}")
                        device.close()
                    }
                }

                override fun onDisconnected(device: CameraDevice) {
                    device.close()
                    cameraDevice = null
                }

                override fun onError(device: CameraDevice, error: Int) {
                    showError("Camera error: $error")
                    device.close()
                    cameraDevice = null
                }
            }, handler)
        } catch (e: CameraAccessException) {
            showError("Camera access failed: ${e.message}")
        } catch (e: IllegalArgumentException) {
            showError("Invalid camera configuration")
        }
    }



    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called, isAvailable: ${textureView.isAvailable}")

        if (textureView.isAvailable) {
            Log.d(TAG, "TextureView is available, opening camera")
            openCamera()
        } else {
            Log.d(TAG, "Waiting for TextureView to be ready")
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                    Log.d(TAG, "SurfaceTexture available - w: $width, h: $height")
                    surfaceTexture = texture

                    // Ensure TextureView has valid dimensions before opening the camera
                    textureView.postDelayed({
                        if (hasCameraPermission() && textureView.width > 0 && textureView.height > 0) {
                            Log.d(TAG, "Opening camera after delay")
                            openCamera()
                        } else {
                            Log.e(TAG, "SurfaceTexture available but TextureView has invalid dimensions")
                        }
                    }, 500) // Adjust delay if necessary
                }

                override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
                    Log.d(TAG, "Surface texture size changed - w: $width, h: $height")
                }

                override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                    Log.d(TAG, "Surface texture destroyed")
                    surfaceTexture = null
                    return false
                }

                override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
                    Log.d(TAG, "Frame updated - Checking if scanning is running")

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastScanTime > 5000) { // Scan every 5 sec
                        lastScanTime = currentTime
                        textureView.bitmap?.let { bitmap ->
                            Log.d(TAG, "Processing frame for scanning...")
                            processFrame(bitmap)
                        } ?: Log.e(TAG, "Failed to capture bitmap from TextureView")
                    }
                }
            }

        }
    }



    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called, releasing camera resources")

        cameraDevice?.close()
        cameraDevice = null
        surface?.release()
        surface = null
    }



    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called, releasing resources")

        surfaceTexture?.release()
        surfaceTexture = null
        surface?.release()
        surface = null
        cameraDevice?.close()
    }


    // Fixed object detection processing
    private fun processFrame(bitmap: Bitmap) {
        try {
            processObjectDetection(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Frame processing error: ${e.message}")
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun processObjectDetection(bitmap: Bitmap) {
        val byteArray = ByteArrayOutputStream().apply {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, this)
        }.toByteArray()

        val request = VisionApiRequest(
            requests = listOf(
                VisionApiRequest.AnnotateImageRequest(
                    image = VisionApiRequest.Image(
                        Base64.encodeToString(byteArray, Base64.NO_WRAP)
                    ),
                    features = listOf(VisionApiRequest.Feature("OBJECT_LOCALIZATION"))
                )
            )
        )

        Log.d(TAG, "Sending API request with key: ${BuildConfig.GOOGLE_CLOUD_VISION_API_KEY}")  // ✅ Log API Key
        Log.d(TAG, "Request Body: $request")  // ✅ Log full request body

        RetrofitClient.instance.detectObjects(request).enqueue(object : Callback<VisionApiResponse> {
            override fun onResponse(call: Call<VisionApiResponse>, response: Response<VisionApiResponse>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "API Response received: ${response.body()}")
                } else {
                    Log.e(TAG, "API Response failed: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VisionApiResponse>, t: Throwable) {
                Log.e(TAG, "API call failed: ${t.message}")
            }
        })
    }


    // Permission handling (unchanged)
    private fun requestCameraPermission() {
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                101
            )
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission granted - checking surface state")
            textureView.postDelayed({
                if (surfaceTexture == null) {
                    Log.d(TAG, "Waiting for surface texture...")
                    textureView.surfaceTextureListener = textureView.surfaceTextureListener
                } else {
                    openCamera()
                }
            }, 500)
        }
    }

    // Navigation (unchanged)
    private fun redirectToShopping(itemName: String) {
        if (System.currentTimeMillis() - lastRedirectTime < 5000) return
        lastRedirectTime = System.currentTimeMillis()

        Intent(this, ShoppingActivity::class.java).apply {
            putExtra("ITEM_QUERY", itemName)
            startActivity(this)
        }
    }

    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottom_nav).apply {
            selectedItemId = R.id.nav_camera
            setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_shopping -> navigateToActivity(ShoppingActivity::class.java)
                    R.id.nav_bookmarks -> navigateToActivity(BookmarkActivity::class.java)
                    R.id.nav_settings -> navigateToActivity(SettingsActivity::class.java)
                    R.id.nav_chat -> navigateToActivity(ChatActivity::class.java)
                }
                true
            }
        }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }

    // Helper functions
    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this,
        android.Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun showError(message: String) {
        Log.e(TAG, message)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}