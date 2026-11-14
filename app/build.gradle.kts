plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.adtech.alayeman"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.adtech.alayeman"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "GROQ_KEY", "\"${System.getenv("GROQ_KEY") ?: ""}\"")
        buildConfigField("String", "GEMINI_KEY", "\"${System.getenv("GEMINI_KEY") ?: ""}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
