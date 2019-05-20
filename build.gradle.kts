buildscript {
    val appVersion by project.extra("0.0.1")
    val appVersionCode by project.extra(1)
    val kotlinVersion by project.extra("1.3.31")
    val gdxVersion by project.extra("1.9.9")
    val ktxVersion by project.extra("1.9.9-b1")
    val junitVersion by project.extra("4.12")
    
    repositories {
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        jcenter()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.4.1")
        classpath(kotlin("gradle-plugin", kotlinVersion))
    }
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/releases/")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

buildDir = file("build/")