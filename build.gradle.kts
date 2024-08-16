// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.plugins.android.tools.build.gradle.get().toString())
        classpath(libs.plugins.jetbrains.kotlin.gradle.get().toString())
        classpath(libs.plugins.dagger.hilt.android.gradle.get().toString())
        classpath(libs.plugins.androidx.navigation.safe.args.get().toString())
        classpath(libs.plugins.gms.oss.licenses.get().toString())

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}
