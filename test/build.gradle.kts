plugins {
    kotlin("jvm")
    application
}

dependencies {
    val gdxVersion: String by project
    val coroutinesVersion: String by project
    val mockitoKotlinVersion: String by project

    implementation(project(":core"))

    implementation(kotlin("stdlib-jdk8"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")

    implementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

application {
    mainClass = "com.maltaisn.nines.test.Main"
}

tasks.named<JavaExec>("run") {
    workingDir = file("../assets")
    standardInput = System.`in`
    jvmArgs = listOf("-ea")
}
