import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
}

android {
    compileSdk = 35
    namespace = "com.ticketmaster.sampleintegration.demo"

    defaultConfig {
        applicationId = "com.ticketmaster.sampleintegration.demo"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Load properties from local.properties
        if (rootProject.file("local.properties").exists()) {
            val properties = Properties().apply {
                rootProject.file("local.properties").inputStream().use { load(it) }
            }

            // Tickets SDK Setup
            val consumerKey = properties["config.consumer_key"] as String?
            val teamName = properties["config.team_name"] as String?
            val brandingColor = properties["config.branding_color"] as String?

            println("branding color: $brandingColor")

            buildConfigField("String", "CONSUMER_KEY", "\"$consumerKey\"")
            buildConfigField("String", "TEAM_NAME", "\"$teamName\"")
            buildConfigField("String", "BRANDING_COLOR", "\"$brandingColor\"")

        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    implementation(libs.material)
    implementation(libs.compose.material)
    // Tickets SDK
    implementation(libs.ticketmaster.tickets)
    // Retail SDK
    implementation(libs.ticketmaster.retail.purchase)
    implementation(libs.ticketmaster.retail.prepurchase)
    implementation(libs.ticketmaster.retail.discoveryapi)
    implementation(libs.ticketmaster.retail.foundation)
    implementation(libs.ticketmaster.purchase)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
