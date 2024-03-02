buildscript {
    val kotlinVersion: String by project
    val androidGradlePluginVersion: String by project
    repositories {
        gradlePluginPortal()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:$androidGradlePluginVersion")
        classpath(kotlin("gradle-plugin", kotlinVersion))
    }
}

plugins {
    base
}

tasks.clean {
    delete(rootProject.layout.buildDirectory)
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}
