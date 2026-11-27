plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.fxcalc"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.fxcalc"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // 方便直接安装测试：使用 debug 签名。正式发布请换成自己的签名配置。
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { viewBinding = true }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            // 可选：指向已下载好的 Robolectric 运行时 jar，离线运行测试
            System.getenv("ROBOLECTRIC_DEPS_DIR")?.let {
                test.systemProperty("robolectric.offline", "true")
                test.systemProperty("robolectric.dependency.dir", it)
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
