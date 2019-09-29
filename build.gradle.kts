buildscript {
    val kotlinVersion: String by project
    repositories {
        google()
        gradlePluginPortal()
        maven("https://maven.fabric.io/public")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.0")
        classpath(kotlin("gradle-plugin", kotlinVersion))

        classpath("com.google.gms:google-services:4.3.2")
        classpath("io.fabric.tools:gradle:1.31.1")
    }
}

plugins {
    base
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        jcenter()
        google()
    }
}
