import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// The signing key is also stored as base64 text (lightswitch.keystore.b64) so it
// can be uploaded with text-only tools. Decode it when the binary file is missing.
fun hobbyKeystore(): File {
    val bin = file("lightswitch.keystore")
    if (!bin.exists()) {
        val b64 = file("lightswitch.keystore.b64").readText().filterNot { it.isWhitespace() }
        val out = layout.buildDirectory.file("lightswitch.keystore").get().asFile
        out.parentFile.mkdirs()
        out.writeBytes(Base64.getDecoder().decode(b64))
        return out
    }
    return bin
}

android {
    namespace = "com.firstmobileapp.lightswitch"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.firstmobileapp.lightswitch"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // Hobby signing key kept in the repo so every build can update the
    // installed app without uninstalling it. Replace with a private key
    // before publishing on Google Play.
    signingConfigs {
        create("hobby") {
            storeFile = hobbyKeystore()
            storePassword = "android"
            keyAlias = "lightswitch"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("hobby")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("hobby")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            // Optional: use a pre-downloaded Robolectric runtime (for offline/proxied machines).
            System.getenv("ROBOLECTRIC_DEPS_DIR")?.let { dir ->
                test.systemProperty("robolectric.offline", "true")
                test.systemProperty("robolectric.dependency.dir", dir)
            }
        }
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
