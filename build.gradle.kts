buildscript {
    val kotlinVersion: String by project
    repositories {
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.2")
        classpath(kotlin("gradle-plugin", kotlinVersion))
    }
}

plugins {
    base
}

tasks.named("clean") {
    delete(buildDir)
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
