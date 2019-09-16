plugins {
    id("com.android.application")
    kotlin("android")
}

val appVersionCode: Int by project
val appVersion: String by project

android {
    buildToolsVersion("29.0.2")
    compileSdkVersion(29)
    defaultConfig {
        applicationId = "com.maltaisn.nines.android"
        minSdkVersion(16)
        targetSdkVersion(29)
        versionCode = appVersionCode
        versionName = appVersion
    }
    buildTypes {
        named("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_6
        targetCompatibility = JavaVersion.VERSION_1_6
    }
    packagingOptions {
        exclude("META-INF/core.kotlin_module")
    }
    sourceSets {
        named("main") {
            java.srcDir("src/main/kotlin")  // Not necessary but works better with IntelliJ
        }
    }
}

val natives: Configuration by configurations.creating

dependencies {
    val gdxVersion: String by project

    implementation(project(":core"))

    implementation(kotlin("stdlib"))

    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")

    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi")
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
tasks.whenTaskAdded {
    if ("package" in name) {
        dependsOn("copyAndroidNatives")
    }
}

// Tasks to copy the assets to the android module assets dir
val assetsPath = android.sourceSets.named("main").get().assets.srcDirs.last().path

tasks.register("copyAssets") {
    file(assetsPath).mkdirs()
    copy {
        from("../assets")
        into(assetsPath)
        exclude("saved-game.json")
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
