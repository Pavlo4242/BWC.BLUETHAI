import java.util.Properties
import java.io.FileInputStream
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ksp)
}

// Function to read the API key from local.properties
fun getApiKey(keyName: String): String {
    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(FileInputStream(localPropertiesFile))
        return properties.getProperty(keyName, "")
    }
    return ""
}


android {
    namespace = "com.bwc.bluethai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bwc.bluethai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Expose all API Keys to the app via BuildConfig
        // Expose all API Keys to the app via BuildConfig

        buildConfigField("String", "GEMINI_API_KEY", "\"${getApiKey("GEMINI_API_KEY")}\"")
        buildConfigField("String", "GEMINI_API_KEY_DEBUG_1", "\"${getApiKey("GEMINI_API_KEY_DEBUG_1")}\"")
        buildConfigField("String", "GEMINI_API_KEY_DEBUG_2", "\"${getApiKey("GEMINI_API_KEY_DEBUG_2")}\"")
        buildConfigField("String", "GEMINI_API_KEY_DEBUG_3", "\"${getApiKey("GEMINI_API_KEY_DEBUG_3")}\"") // Added Debug Key 3
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
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

androidComponents {
    onVariants { variant ->
        // No change needed for this part
        val apkDirectory = variant.artifacts.get(SingleArtifact.APK)
        val outputDirectory = project.layout.buildDirectory.dir("renamed_apks/${variant.name}")

        tasks.register("rename${variant.name.replaceFirstChar { it.uppercase() }}Apk", Copy::class.java) {
            from(apkDirectory)
            into(outputDirectory)

            // Correct way to get the versionName
            val versionName = variant.outputs.first().versionName.get()

            // Construct the new file name using the retrieved versionName
            val newName = "BWCTrans-${variant.buildType}-v${versionName}.apk"

            rename {
                newName
            }
        }
    }
}


dependencies {
    // Core & UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.5.0")


    // Gemini AI
    implementation(libs.generativeai)

    // ViewModel & Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
