plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aistudio.yodha.kdjxms"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Dynamic Firebase and OAuth injector from project configuration
        val configFile = file("${project.rootDir}/firebase-applet-config.json")
        var projId = "yodha-music-67"
        var appIdVal = "1:1058333427757:android:6aed4f05765cc8b70510a4"
        var apiKeyVal = "AIzaSyAN5xiqIABkhaxlQ1dPzopFvJsb8E50JQg"
        var bucket = "yodha-music-67.firebasestorage.app"
        var senderId = "1058333427757"
        var oAuthId = "684033752161-rq07s0hfajbjfp7f53nq1a3ad8jdfk65.apps.googleusercontent.com"

        if (configFile.exists()) {
            val text = configFile.readText()
            val extractKey = { key: String ->
                val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
                val match = regex.find(text)
                match?.groups?.get(1)?.value
            }
            projId = extractKey("projectId") ?: projId
            appIdVal = extractKey("appId") ?: appIdVal
            apiKeyVal = extractKey("apiKey") ?: apiKeyVal
            bucket = extractKey("storageBucket") ?: bucket
            senderId = extractKey("messagingSenderId") ?: senderId
            oAuthId = extractKey("oAuthClientId") ?: oAuthId
        }

        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"$projId\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"$appIdVal\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"$apiKeyVal\"")
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", "\"$bucket\"")
        buildConfigField("String", "FIREBASE_MESSAGING_SENDER_ID", "\"$senderId\"")
        buildConfigField("String", "FIREBASE_OAUTH_CLIENT_ID", "\"$oAuthId\"")
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Firebase Platform
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Google Play Services Auth for Real Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // Media3 ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")
    implementation("androidx.media3:media3-session:1.5.0")

    // Google Mobile Ads (AdMob)
    implementation("com.google.android.gms:play-services-ads:23.5.0")

    // Coil Image Loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Retrofit & Moshi Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.11.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
