plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.chroniccare"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.chroniccare"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
}

dependencies {
    implementation(libs.firebase.database)
    // --- ROOM DATABASE ---
    val room_version = "2.8.2"
    implementation( "com.google.firebase:firebase-firestore:26.0.2")

    implementation("androidx.room:room-runtime:$room_version")

    // Use this for Java projects (annotation processor)
    annotationProcessor("androidx.room:room-compiler:$room_version")

    // Optional: Kotlin coroutines and LiveData extensions
    implementation("androidx.room:room-ktx:$room_version")

    // Optional: Paging, RxJava, Guava support
    implementation("androidx.room:room-paging:$room_version")
    implementation("androidx.room:room-rxjava3:$room_version")
    implementation("androidx.room:room-guava:$room_version")

    // Optional: Room testing utilities
    testImplementation("androidx.room:room-testing:$room_version")

    // --- UI + CORE DEPENDENCIES ---
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.graphplot)
    implementation(libs.circleimageview)

    // --- TESTING ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
