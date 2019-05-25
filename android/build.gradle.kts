/*
 * Copyright 2019 Nicolas Maltais
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("com.android.application")
    kotlin("android")
}

val appVersionCode: Int by project
val appVersion: String by project

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
        configurations.named("natives").get().files.forEach { jar ->
            val outputDir = file("libs/" + jar.nameWithoutExtension.substringAfterLast("natives-"))
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