buildscript {
    val appVersion by project.extra("0.0.1")
    val appVersionCode by project.extra(1)
    val kotlinVersion by project.extra("1.3.41")
    val gdxVersion by project.extra("1.9.10")
    val ktxVersion by project.extra("1.9.10-SNAPSHOT")
    val junitVersion by project.extra("4.12")
    
    repositories {
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.4.2")
        classpath(kotlin("gradle-plugin", kotlinVersion))
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
