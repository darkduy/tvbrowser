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

fun envOrProp(envKey: String, propKey: String): String? =
    System.getenv(envKey)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propKey)?.takeIf { it.isNotBlank() }

// Chỉ bắt buộc phải có đủ thông tin ký khi task đang chạy thực sự là build Release
// (assembleRelease, bundleRelease...). Nhờ vậy các task khác (lint, testDebug...)
// không bị fail oan chỉ vì máy chưa cấu hình keystore.
val isReleaseBuild = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true)
}

val storeFilePath = envOrProp("KEYSTORE_PATH", "storeFile")
val storePasswordValue = envOrProp("KEYSTORE_PASSWORD", "storePassword")
val keyAliasValue = envOrProp("KEY_ALIAS", "keyAlias")
val keyPasswordValue = envOrProp("KEY_PASSWORD", "keyPassword")

if (isReleaseBuild) {
    // Fail cứng ngay tại lúc cấu hình nếu thiếu bất kỳ biến ký nào.
    //
    // BUG CŨ đã sửa ở đây: code trước kiểm tra "storeFile != null" rồi mới gán
    // signingConfig, nếu thiếu biến thì ÂM THẦM bỏ qua signingConfig. Android
    // Gradle Plugin khi đó tự fallback ký release build bằng debug key mặc định.
    // Kết quả: Gradle vẫn báo "BUILD SUCCESSFUL", APK vẫn được tạo ra, nhưng
    // KHÔNG được ký bằng keystore thật -> cài đè lên bản cũ (đã ký khác key)
    // sẽ báo lỗi chữ ký không khớp, hoặc APK không đáng tin cậy để phát hành.
    // Giờ nếu thiếu bất kỳ biến nào, build sẽ dừng ngay với thông báo rõ ràng
    // thay vì âm thầm tạo ra một APK ký sai.
    val missing = buildList {
        if (storeFilePath == null) add("KEYSTORE_PATH / storeFile")
        if (storePasswordValue == null) add("KEYSTORE_PASSWORD / storePassword")
        if (keyAliasValue == null) add("KEY_ALIAS / keyAlias")
        if (keyPasswordValue == null) add("KEY_PASSWORD / keyPassword")
    }
    check(missing.isEmpty()) {
        "Thiếu cấu hình ký release, dừng build: ${missing.joinToString(", ")}. " +
            "Kiểm tra lại GitHub Secrets (CI) hoặc file keystore.properties (build local)."
    }
    check(file(storeFilePath!!).exists()) {
        "File keystore không tồn tại tại đường dẫn: $storeFilePath. " +
            "Kiểm tra lại bước giải mã KEYSTORE_BASE64 trong workflow đã tạo đúng file chưa."
    }
}

android {
    namespace = "com.duyvietnambeta.tvbrowser"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.duyvietnambeta.tvbrowser"
        minSdk = 26          // Android 8.0 (Oreo) trở lên — phổ biến trên Android TV/Fire TV đời mới
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            if (isReleaseBuild) {
                storeFile = file(storeFilePath!!)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
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
            signingConfig = signingConfigs.getByName("release")
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