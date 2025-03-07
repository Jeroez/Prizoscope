import java.util.Properties
import java.io.File
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    id("com.google.gms.google-services")
}

// ✅ Properly loading local.properties
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        load(FileInputStream(localFile))
    }
}

val googleApiKey: String = localProperties.getProperty("GOOGLE_CLOUD_VISION_API_KEY") ?: ""

android {
    namespace = "com.example.prizoscope"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.prizoscope"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "GOOGLE_CLOUD_VISION_API_KEY", "\"$googleApiKey\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "GOOGLE_CLOUD_VISION_API_KEY", "\"$googleApiKey\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    aaptOptions {
        noCompress("tflite")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.mlkit.text.recognition)
    implementation(libs.play.services.location)
    implementation(libs.volley)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Lifecycle & Navigation
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // RecyclerView & Room
    implementation(libs.recyclerview)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // Moshi JSON Parser
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Google Maps
    implementation(libs.maps)

    // Coroutines & Utilities
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.preference)
    implementation(libs.guava)

    // Glide for Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:compiler:4.16.0")

    // Google ML Kit
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // CameraX (Latest)
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")

    // OpenCSV for CSV Parsing
    implementation("com.opencsv:opencsv:5.8")

    // Apache Commons for Utility Functions
    implementation("org.apache.commons:commons-lang3:3.12.0")

    // Firebase Dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.mlkit:linkfirebase:16.0.0")

    // TensorFlow Lite for ML
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.3")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.3")

    // Retrofit for Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Glide Annotation Processor
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Lifecycle Runtime for Coroutines Support
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
}
