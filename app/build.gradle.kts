plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}
val SERVER_IP: String by project

android {
    namespace = "com.example.mymusic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mymusic"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "SERVER_IP", "\"${SERVER_IP}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
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


dependencies {
    // Lifecycle ViewModel + LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // Lifecycle Runtime（核心库）
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // 可选：ViewModel 支持库（如 SavedStateHandle）
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.6.2")
    // 使用较新的 Data Binding 依赖
    implementation("androidx.databinding:databinding-runtime:8.10.1")
    implementation("androidx.databinding:databinding-common:8.10.1")

    // fresco
    implementation("com.facebook.fresco:fresco:2.6.0")
    // 可选扩展（如GIF支持）
    implementation("com.facebook.fresco:animated-gif:2.0.0")

    // TabLayout / 指示器CircleIndicator
    implementation("me.relex:circleindicator:2.1.6")

    // okHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    //ConverterFactory 的 String 依赖包
    //拦截器 OkHttp库的Logging Interceptor模块。
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // 或最新版


    // lottie
    implementation("com.airbnb.android:lottie:6.0.0")
    implementation("com.squareup.retrofit2:adapter-rxjava3:2.9.0")
    // 添加 RxAndroid 库（提供 Android 主线程调度器）
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("androidx.media3:media3-session:1.3.1")
//    implementation("androidx.media:media:1.6.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.service)
//    implementation(libs.androidx.ui.desktop)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}