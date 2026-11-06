plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.ridedriver"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.ridedriver"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        val apiBase = providers.gradleProperty("rida.apiBaseUrl").orElse("https://rida-backend.ditronics.co.tz/api/v1/").get()
        require(apiBase.startsWith("https://") && apiBase.endsWith("/") && '"' !in apiBase) { "rida.apiBaseUrl must be an HTTPS base URL ending in /" }
        buildConfigField("String", "API_BASE_URL", "\"" + apiBase + "\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Use the simulator only when explicitly requested for development.
        buildConfigField("boolean", "USE_MOCK_DRIVER_API", providers.gradleProperty("rida.mockDriver").orNull.toBoolean().toString())
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.material)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.lucide.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Map tiles (osmdroid) - supports custom Google tile URLs
    implementation(libs.osmdroid.android)
    // Rida API + Search + Routing (Nominatim + OSRM)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    // Live driver location
    implementation(libs.play.services.location)
    implementation(libs.accompanist.permissions)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)

    // Credential Manager & Google Sign-In
    implementation(libs.credentials.core)
    implementation(libs.credentials.play)
    implementation(libs.google.id)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
