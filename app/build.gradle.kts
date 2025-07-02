plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.marouua.beztamy"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.marouua.beztamy"
        minSdk = 27
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.google.android.material:material:1.6.0")
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation ("com.google.firebase:firebase-firestore:24.9.0")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}