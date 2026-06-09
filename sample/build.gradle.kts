plugins {
    alias(libs.plugins.android.application)
}

val libVersionName: String by rootProject.extra
val libVersionCode: Int by rootProject.extra

android {
    namespace = "com.vijay.jsonwizard.demo"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.vijay.jsonwizard.demo"
        minSdk = 21
        targetSdk = 36
        versionCode = libVersionCode
        versionName = libVersionName
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":library"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
}
