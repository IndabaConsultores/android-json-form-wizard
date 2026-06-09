pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven {
            setUrl("https://jitpack.io")
        }
        flatDir {
            dir("libs")
        }
    }
}

include(":library")
include("sample")