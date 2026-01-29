plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // Might need if models use Compose types, keeping for safety or remove if unused.
    kotlin("plugin.serialization") version "1.9.22"
    alias(libs.plugins.ksp) // If Room/Parcelize is used
    id("kotlin-parcelize")
}

android {
    namespace = "com.suvojeet.suvmusic.model"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true // Keep if needed, otherwise false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Room annotations (if entities are moved here)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    
    // Gson
    implementation(libs.gson)

    // Media3 Common (if needed for definitions)
    implementation(libs.androidx.media3.common)

    // Compose (if needed for UI-related models like Icons/Colors)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
}
