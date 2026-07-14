import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.bundletool)
    id("project-report")
}

fun loadLocalProperties(): Properties {
    val props = Properties()

    // Primary location: ./app/secrets.properties
    val primaryFile = file("secrets.properties")
    if (primaryFile.exists()) {
        props.load(primaryFile.inputStream())
        println("Secrets loaded from app/secrets.properties")
        return props
    }

    // Fallback location: ~/.config/gfroerli-android/secrets.properties
    val homeDir = System.getProperty("user.home")
    val fallbackFile = File(homeDir, ".config/gfroerli-android/secrets.properties")
    if (fallbackFile.exists()) {
        props.load(fallbackFile.inputStream())
        println("Secrets loaded from ${fallbackFile.absolutePath}")
        return props
    }

    // Return empty properties if neither file exists
    return props
}

val localProperties = loadLocalProperties()

android {
    namespace = "ch.coredump.watertemp"

    defaultConfig {
        applicationId = "ch.coredump.watertemp"
        applicationIdSuffix = ".zh"
        minSdk = 26
        targetSdk = 36
        compileSdk = 36
        versionCode = 22
        versionName = "1.1.4"

        buildConfigField("String", "GFROERLI_API_KEY_PUBLIC",
            "\"${localProperties.getProperty("gfroerli_api_key_public")}\"")
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN",
            "\"${localProperties.getProperty("mapbox_access_token")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["disableAnalytics"] = "true"
    }

    dependenciesInfo {
        // Disable dependency metadata when building APKs
        includeInApk = false
        // Include dependency metadata when building Android App Bundles
        includeInBundle = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    signingConfigs {
        if (localProperties.getProperty("keystoreFile") != null) {
            create("release") {
                storeFile = file(localProperties.getProperty("keystoreFile")!!)
                storePassword = localProperties.getProperty("keystorePassword")
                keyAlias = localProperties.getProperty("keyAlias")
                keyPassword = localProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Use a separate application ID for debug builds, so that they can be
            // installed alongside the production app. The app name is overridden
            // in src/debug/res/values/strings.xml accordingly.
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (localProperties.getProperty("keystoreFile") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    splits {
        abi {
            // Detect app bundle and conditionally disable split abis
            // This is needed due to a "Sequence contains more than one matching element" error
            // present since AGP 8.9.0, for more info see:
            // https://issuetracker.google.com/issues/402800800

            // AppBundle tasks contain "bundle" or "apks" in their name (e.g. buildApksRelease)
            val isBuildingBundle = gradle.startParameter.taskNames.any {
                val lower = it.lowercase()
                lower.contains("bundle") || lower.contains("apks")
            }

            // Enable split ABIs unless building appBundle
            isEnable = !isBuildingBundle

            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")

            isUniversalApk = true
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.all {
            // Whether failing tests should fail the build
            it.ignoreFailures = false

            // Logging output
            it.testLogging {
                events("passed", "skipped", "failed", "standardOut", "standardError")
            }
        }
    }

    packaging {
        resources {
            // Exclude potentially conflicting files
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
}

dependencies {
    // Compose BOM, which ties together compatible versions
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    // Support lbiraries
    implementation(libs.appcompat)
    implementation(libs.material)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Compose: Integration with activities
    implementation(libs.activity.compose)
    // Compose: Compose Material Design
    implementation(libs.compose.material)
    // // Compose: Animations
    implementation(libs.compose.animation)
    // Compose: Tooling support (Previews, etc.)
    implementation(libs.compose.ui.tooling)
    // Compose: Material Icons
    implementation(libs.compose.material.icons.extended)
    // Compose: Integration with ViewModels
    implementation(libs.lifecycle.viewmodel.compose)
    // Compose: Image loading
    implementation(libs.landscapist.glide)
    // Compose: Various composables (used for scrollbars)
    implementation(libs.composables.core)

    // MapLibre SDK
    implementation(libs.maplibre)
    implementation(libs.maplibre.annotation)

    // REST client
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Relative time calculation
    implementation(libs.prettytime)

    // Charts
    implementation(libs.mpandroidchart)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso)
    // Test rules and transitive dependencies
    androidTestImplementation(libs.compose.ui.test.junit4)
    // Needed for createComposeRule, but not createAndroidComposeRule
    debugImplementation(libs.compose.ui.test.manifest)
}

bundletool {
    if (localProperties.getProperty("keystoreFile") != null) {
        signingConfig {
            storeFile = file(localProperties.getProperty("keystoreFile")!!)
            storePassword.set(localProperties.getProperty("keystorePassword")!!)
            keyAlias = localProperties.getProperty("keyAlias")
            keyPassword.set(localProperties.getProperty("keyPassword")!!)
        }
    }
}
