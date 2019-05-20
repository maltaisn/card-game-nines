plugins {
    id("com.android.application")
    kotlin("android")
}

tasks.whenTaskAdded {
    if ("package" in name) {
        dependsOn("copyAndroidNatives")
    }
}

android {
    buildToolsVersion("28.0.3")
    compileSdkVersion(28)
    sourceSets {
        named("main") {
            java.srcDir("src/main/kotlin")
            res.srcDir("res")
            assets.srcDir("assets")
            jniLibs.srcDir("libs")
        }
    }
    defaultConfig {
        applicationId = "com.maltaisn.nines.android"
        minSdkVersion(14)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "0.0.1"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
}

configurations.create("natives")

dependencies {
    val gdxVersion: String by project

    implementation(project(":core"))

    implementation(kotlin("stdlib"))

    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")

    "natives"("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi")
    "natives"("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    "natives"("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    "natives"("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    "natives"("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_6
    targetCompatibility = JavaVersion.VERSION_1_6
}

// Called every time gradle gets executed, takes the native dependencies of
// the natives configuration, and extracts them to the proper libs/ folders
// so they get packed with the APK.
tasks.register("copyAndroidNatives") {
    doFirst {
        file("libs/armeabi/").mkdirs()
        file("libs/armeabi-v7a/").mkdirs()
        file("libs/arm64-v8a/").mkdirs()
        file("libs/x86_64/").mkdirs()
        file("libs/x86/").mkdirs()

        configurations.named("natives").get().files.forEach { jar ->
            val outputDir = when {
                jar.name.endsWith("natives-arm64-v8a.jar") -> file("libs/arm64-v8a")
                jar.name.endsWith("natives-armeabi-v7a.jar") -> file("libs/armeabi-v7a")
                jar.name.endsWith("natives-armeabi.jar") -> file("libs/armeabi")
                jar.name.endsWith("natives-x86_64.jar") -> file("libs/x86_64")
                jar.name.endsWith("natives-x86.jar") -> file("libs/x86")
                else -> null
            }
            if (outputDir != null) {
                copy {
                    from(zipTree(jar))
                    into(outputDir)
                    include("*.so")
                }
            }
        }
    }
}
tasks.whenTaskAdded {
    if ("package" in name) {
        dependsOn("copyAndroidNatives")
    }
}

// Tasks to copy the tests assets to the android module assets dir
tasks.register("copyTestAssets") {
    file("assets").mkdirs()
    copy {
        from("../assets")
        into("assets")
    }
}

tasks.register<Delete>("cleanTestAssets") {
    delete("assets")
}

tasks.named("clean") {
    finalizedBy("cleanTestAssets")
}

tasks.named("build") {
    finalizedBy("copyTestAssets")
}