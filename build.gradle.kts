import com.android.build.api.dsl.CommonExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.sonar.scanner) apply true
}

subprojects {
    afterEvaluate {
        if(project.hasProperty("android")) {
            project.extensions.configure<CommonExtension>("android") {
                if(namespace == null) {
                    namespace = project.group.toString()
                }
            }
        }
    }
}

tasks.register<Delete>("clean") {
    description = "Deletes build directoy"
    group = "build"
    delete(rootProject.layout.buildDirectory)
}

//Keep subproject versions in common
val libVersionName: String by extra { "1.10.23" }
val libVersionCode: Int by extra { 27 }
