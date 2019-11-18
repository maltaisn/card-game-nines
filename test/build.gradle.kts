plugins {
    kotlin("jvm")
    application
}

dependencies {
    val gdxVersion: String by project
    val coroutinesVersion: String by project
    val mockitoKotlinVersion: String by project

    implementation(project(":core"))

    implementation(kotlin("stdlib"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")

    implementation("com.nhaarman.mockitokotlin2:mockito-kotlin:$mockitoKotlinVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_6
    targetCompatibility = JavaVersion.VERSION_1_6
}


application {
    mainClassName = "com.maltaisn.nines.test.Main"
}

tasks.named<JavaExec>("run") {
    workingDir = file("../assets")
    standardInput = System.`in`
    jvmArgs = listOf("-ea")
}
