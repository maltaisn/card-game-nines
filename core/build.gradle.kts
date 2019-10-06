plugins {
    kotlin("jvm")
    idea
}

idea {
    module {
        outputDir = file("build/classes/main")
        testOutputDir = file("build/classes/test")
    }
}

dependencies {
    val gdxVersion: String by project
    val ktxVersion: String by project
    val junitVersion: String by project
    val cardGameVersion: String by project
    val pcardVersion: String by project

    api("com.maltaisn.cardgame:core:$cardGameVersion")
    api("com.maltaisn.cardgame:pcard:$pcardVersion")

    implementation(kotlin("stdlib"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-RC")
    implementation("io.github.libktx:ktx-async:$ktxVersion")

    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")

    implementation("io.github.libktx:ktx-actors:$ktxVersion")
    implementation("io.github.libktx:ktx-assets:$ktxVersion")
    implementation("io.github.libktx:ktx-collections:$ktxVersion")
    implementation("io.github.libktx:ktx-json:$ktxVersion")
    implementation("io.github.libktx:ktx-log:$ktxVersion")
    implementation("io.github.libktx:ktx-math:$ktxVersion")
    implementation("io.github.libktx:ktx-style:$ktxVersion")

    compileOnly("com.gmail.blueboxware:libgdxpluginannotations:1.16")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_6
    targetCompatibility = JavaVersion.VERSION_1_6
}

// Tasks to copy the card game assets to the project assets folder
tasks.register("copyCardGameAssets") {
    file("../assets").mkdirs()
    copy {
        from("../../cardgame/assets/")
        include("core/**")
        include("pcard/**")
        into("../assets")
    }
}

tasks.register<Delete>("cleanCardGameAssets") {
    delete("../assets/core")
    delete("../assets/pcard")
}

tasks.clean {
    finalizedBy("cleanCardGameAssets")
}

tasks.build {
    finalizedBy("copyCardGameAssets")
}
