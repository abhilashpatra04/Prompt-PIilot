//buildscript {
//    ext {
//        compose_version = "2.2.0"
//    }
//    dependencies {
//        // Add the dependency for the Google services Gradle plugin
//        classpath (libs.google.services)
//        // Other classpath declarations
//        classpath (libs.kotlin.serialization)
//    }
//}// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {

    id ("com.android.application") version "8.11.1" apply false
    id ("com.android.library") version "8.11.1" apply false
    id ("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id ("com.google.dagger.hilt.android") version "2.56.2" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
//    alias(libs.plugins.kotlin.compose.compiler) apply false
}

//tasks.register('clean', Delete) {
//    delete rootProject.layout.buildDirectory
//}
