plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.devtools.ksp)

    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.android.gms.oss-licenses-plugin")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "org.nunocky.sudokusolver"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.nunocky.sudokusolver"
        minSdk = 21
        targetSdk = 34
        versionCode = 3
        versionName = "1.0"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }

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
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn"
        )
    }

    buildFeatures {
        buildConfig = true
        dataBinding = true
    }
}

dependencies {
    implementation(project(":sudokulib"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // LiveData
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.legacy.support.v4)

    implementation(libs.androidx.cardview)

    // RecyclerView
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.recyclerview.selection)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.dynamic.features.fragment)
    androidTestImplementation(libs.androidx.navigation.testing)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    testImplementation(libs.androidx.room.testing)

    // Copyright
    implementation(libs.play.services.oss.licenses)

    // Moshi
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

    // Kotshi
    implementation(libs.kotshiApi)
    ksp(libs.kotshiCompiler)

    // test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.core.testing)
}