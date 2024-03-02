plugins {
    id("com.android.application")
    kotlin("android")
    id("com.github.triplet.play")
    id("com.github.breadmoirai.github-release")
}

val appVersion: String by project
val appVersionCode = appVersion.split('.')
        .joinToString("") { it.padStart(2, '0') }.toInt()

android {
    namespace = "com.maltaisn.nines.android"

    buildToolsVersion = "34.0.0"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.maltaisn.nines.android"
        minSdk = 16
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersion
    }
    signingConfigs {
        create("release") {
            storeFile = file(extra.get("releaseKeyStoreFile") as String)
            storePassword = extra["releaseKeyStorePassword"] as String
            keyAlias = extra["releaseKeyStoreKey"] as String
            keyPassword = extra["releaseKeyStoreKeyPassword"] as String
        }
    }
    buildTypes {
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.named("release").get()
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources.excludes.add("com/badlogic/**")  // Unused default shader files and assets
        resources.excludes.add("META-INF/*")  // Collision between core and cardgame/core modules
    }
    sourceSets {
        named("main") {
            java.srcDir("src/main/kotlin")  // Not necessary but works better with IntelliJ
            assets.srcDir("src/main/assets")
        }
    }
}

val natives: Configuration by configurations.creating

dependencies {
    val gdxVersion: String by project

    implementation(project(":core"))

    implementation(kotlin("stdlib-jdk8"))

    // LibGDX
    api("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

// Called every time gradle gets executed, takes the native dependencies of
// the natives configuration, and extracts them to the proper libs/ folders
// so they get packed with the APK.
tasks.register("copyAndroidNatives") {
    doFirst {
        val jniLibsPath = android.sourceSets.named("main").get().jniLibs.srcDirs.last().path
        natives.files.forEach { jar ->
            val nativeName = jar.nameWithoutExtension.substringAfterLast("natives-")
            val outputDir = File(jniLibsPath, nativeName)
            outputDir.mkdirs()
            copy {
                from(zipTree(jar))
                into(outputDir)
                include("*.so")
            }
        }
    }
}
tasks.configureEach {
    if ("package" in name) {
        dependsOn("copyAndroidNatives")
    }
}

// Tasks to copy the assets to the android module assets dir
val assetsPath = android.sourceSets.named("main").get().assets.srcDirs.last().path

tasks.register("copyAssets") {
    dependsOn(":core:copyCardGameAssets")
    doFirst {
        file(assetsPath).mkdirs()
        copy {
            from("../assets")
            into(assetsPath)
            exclude("saved-game.json")
        }
    }
}

tasks.register<Delete>("cleanAssets") {
    delete(assetsPath)
}

tasks.named("clean") {
    finalizedBy("cleanAssets")
}

tasks.named("build") {
    finalizedBy("copyAssets")
}

// Publishing
play {
    serviceAccountCredentials = file("fake-key.json")
}
if (file("publishing.gradle").exists()) {
    apply { from("publishing.gradle") }
}
