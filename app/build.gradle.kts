import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Đọc thông tin keystore từ biến môi trường (được set bởi GitHub Actions secrets)
// hoặc từ file keystore.properties nếu build ở máy local.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

fun envOrProp(envKey: String, propKey: String): String =
    System.getenv(envKey) ?: keystoreProperties.getProperty(propKey) ?: ""

android {
    namespace = "com.duyvietnambeta.tvbrowser"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.duyvietnambeta.tvbrowser"
        minSdk = 21          // Android TV chạy từ 5.0 (API 21) trở lên
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = envOrProp("KEYSTORE_PATH", "storeFile")
            if (storeFilePath.isNotBlank()) {
                storeFile = file(storeFilePath)
                storePassword = envOrProp("KEYSTORE_PASSWORD", "storePassword")
                keyAlias = envOrProp("KEY_ALIAS", "keyAlias")
                keyPassword = envOrProp("KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Chỉ áp dụng signingConfig nếu đã có storeFile hợp lệ (tránh lỗi khi build local chưa cấu hình keystore)
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
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
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
