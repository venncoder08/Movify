import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

// Backend URL
val BACKEND_URL = "http://192.168.1.226:3000"

android {
    namespace = "vnuhcm.hcmus.movify"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "vnuhcm.hcmus.movify"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["googleMapsApiKey"] =
            localProperties.getProperty("googleMapsApiKey", "")
    }

    flavorDimensions += "environment"
    productFlavors {
        create("emulator") {
            dimension = "environment"
            manifestPlaceholders["backendUrl"] = BACKEND_URL
        }
        create("localhost") {
            dimension = "environment"
            manifestPlaceholders["backendUrl"] = BACKEND_URL
        }
        create("devServer") {
            dimension = "environment"
            manifestPlaceholders["backendUrl"] = BACKEND_URL
        }
        create("production") {
            dimension = "environment"
            manifestPlaceholders["backendUrl"] = BACKEND_URL
        }
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.play.services.places)
    implementation(libs.maps.utils)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.mlkit.barcode.scanning)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation("androidx.work:work-runtime:2.9.1")
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation("androidx.lifecycle:lifecycle-runtime:2.6.1")
    implementation("io.socket:socket.io-client:2.1.1") {
        exclude(group = "org.json", module = "json")
    }
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
