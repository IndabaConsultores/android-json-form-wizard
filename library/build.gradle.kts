plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.sonar.scanner)
    id("maven-publish")
}

val libVersionName: String by rootProject.extra
val libVersionCode: Int by rootProject.extra
version = "$libVersionCode-$libVersionName"

android {
    namespace = "com.vijay.jsonwizard"
    compileSdk =  36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        minSdk = 21
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

    lint {
        abortOnError = false
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.discrete.scrollview) {
        artifact { type = "aar" }
    }
    implementation(libs.html.textview) {
        artifact { type = "aar" }
    }

    implementation(libs.jsonpath)
    implementation(libs.glide)
    implementation(libs.ganfra.material.spinner) {
        exclude(group = "com.nineoldandroids", module = "library")
    }

    implementation(libs.firebase.ml.vision)
    implementation(libs.firebase.ml.vision.barcode.model)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera2)

    implementation(libs.google.services.vision)
    implementation(libs.google.services.maps)
    implementation(libs.google.services.location)

    implementation(libs.largetool)

    implementation(libs.androidx.material)

    implementation(libs.gson)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

sonarqube {
    properties {
        property("sonar.projectName", "Android JSON FORM Wizard")
        property("sonar.projectKey", "es.indaba.android-json-form-wizard")
        property("sonar.java.test.binaries", "$projectDir/build/intermediates/classes/androidTest/debug")
    }
}

androidComponents.onVariants { variant ->
    val variantName = variant.name
    val buildType = variant.buildType ?: ""
    println("Configuring publication for variant: $variantName with build type: $buildType")

    if (buildType.equals("release", ignoreCase = true)) {
        println("Creating publication for release variant: $variantName")
        publishing.publications.create<MavenPublication>("${variantName}Aar") {
            groupId = "com.orona.framework"
            artifactId = "help-lib"
            version = libVersionName

            afterEvaluate {
                println("Found components: ${components.names.joinToString(", ")}")
                println("Attempting to publish from component: $variantName")
                from(components[variantName])
            }
        }
    }
}