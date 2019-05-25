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
    kotlin("jvm")
}

sourceSets {
    main {
        java.srcDir("src/main/kotlin")
    }
    test {
        java.srcDir("src/test/kotlin")
    }
}

dependencies {
    val gdxVersion: String by project
    val ktxVersion: String by project
    val junitVersion: String by project

    api("com.maltaisn:cardgame:0.0.1")

    implementation(kotlin("stdlib"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.1")
    implementation("io.github.libktx:ktx-async:$ktxVersion")

    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")

    implementation("io.github.libktx:ktx-actors:$ktxVersion")
    implementation("io.github.libktx:ktx-assets:$ktxVersion")
    implementation("io.github.libktx:ktx-collections:$ktxVersion")
    implementation("io.github.libktx:ktx-math:$ktxVersion")
    implementation("io.github.libktx:ktx-log:$ktxVersion")
    implementation("io.github.libktx:ktx-style:$ktxVersion")

    compileOnly("com.gmail.blueboxware:libgdxpluginannotations:1.16")
    
    testImplementation("junit:junit:$junitVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_6
    targetCompatibility = JavaVersion.VERSION_1_6
}

tasks.register<JavaExec>("runTest") {
    main = "com.maltaisn.nines.core.GameTestKt"
    classpath = sourceSets.test.get().runtimeClasspath
    standardInput = System.`in`
    isIgnoreExitValue = true
}

// Tasks to copy the card game assets to the project assets folder
tasks.register("copyCardGameAssets") {
    file("../assets").mkdirs()
    copy {
        from("../../cardgame/assets")
        into("../assets")
    }
}

tasks.register<Delete>("cleanCardGameAssets") {
    delete("assets")
}

tasks.named("clean") {
    finalizedBy("cleanCardGameAssets")
}

tasks.named("build") {
    finalizedBy("copyCardGameAssets")
}