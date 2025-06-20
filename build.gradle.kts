// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {

    repositories {
        mavenCentral()
        mavenLocal()
        google()
        maven(url = "https://jitpack.io")
        maven (url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }

    dependencies {
        classpath(libs.android.gradle.plugin)
        classpath(libs.kotlin.gradle.plugin)
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        google()
        maven(url = "https://jitpack.io")
        maven (url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}
