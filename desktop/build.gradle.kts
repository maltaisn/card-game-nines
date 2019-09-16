plugins {
    kotlin("jvm")
}

val assetsDir = file("../assets")
val mainClassName = "com.maltaisn.nines.desktop.DesktopLauncher"

dependencies {
    val gdxVersion: String by project
    
    implementation(project(":core"))

    implementation(kotlin("stdlib"))
    
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_6
    targetCompatibility = JavaVersion.VERSION_1_6
}

sourceSets {
    named("main") {
        resources.srcDir("src/main/res")
    }
}

tasks.register<JavaExec>("run") {
    main = mainClassName
    classpath = sourceSets.main.get().runtimeClasspath
    standardInput = System.`in`
    workingDir = assetsDir
    isIgnoreExitValue = true
}

tasks.register<Jar>("dist") {
    from(files(sourceSets.main.get().output.classesDirs))
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    from(assetsDir)
    from(sourceSets.named("main").get().resources)

    manifest {
        attributes["Main-Class"] = mainClassName
    }
}
