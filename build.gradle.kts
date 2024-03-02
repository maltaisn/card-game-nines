buildscript {
    val kotlinVersion: String by project
    val androidGradlePluginVersion: String by project
    val gppVersion: String by project
    val githubReleasePluginVersion: String by project
    repositories {
        gradlePluginPortal()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:$androidGradlePluginVersion")
        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath("com.github.triplet.gradle:play-publisher:$gppVersion")
        classpath("com.github.breadmoirai:github-release:$githubReleasePluginVersion")
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
