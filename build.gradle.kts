import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests


val ktor_version: String by project


plugins {
    application
    kotlin("multiplatform").version("2.0.0")
}

group = "com.qinxi1992"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

val hostOs = System.getProperty("os.name")
val isArm64 = System.getProperty("os.arch") == "aarch64"
val isMingwX64 = hostOs.startsWith("Windows")
val nativeTarget = when {
    hostOs == "Mac OS X" && isArm64 -> "MacosArm64"
    hostOs == "Mac OS X" && !isArm64 -> "MacosX64"
    hostOs == "Linux" && !isArm64 -> "LinuxX64"
    isMingwX64 -> "MingwX64"
    else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
}

fun KotlinNativeTargetWithHostTests.configureTarget()=
    binaries {
        executable {
            entryPoint = "main"
        }
    }
fun KotlinNativeTarget.configureTarget()=
    binaries {
        executable {
            entryPoint = "main"
        }
    }
kotlin {
    macosX64 { configureTarget() }
    macosArm64{ configureTarget() }
    mingwX64 { configureTarget() }
    linuxX64 { configureTarget() }
    linuxArm64 { configureTarget() }

    val jvmTarget = jvm()



    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }

        val commonMain by getting  {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktor_version")
            }
        }


        arrayOf("macosX64", "macosArm64").forEach { targetName ->
            getByName("${targetName}Main"){
                dependsOn(commonMain)
                dependencies {
                    implementation("io.ktor:ktor-client-darwin:$ktor_version")
                }
            }
        }

        val mingwX64Main by getting {
            dependsOn(commonMain)

            dependencies {
                implementation("io.ktor:ktor-client-winhttp:$ktor_version")
            }
        }

        val linuxX64Main by getting {
            dependsOn(commonMain)

            dependencies {
                implementation("io.ktor:ktor-client-curl:$ktor_version")
            }
        }

    }
    tasks.withType<JavaExec> {
        // code to make run task in kotlin multiplatform work
        val compilation = jvmTarget.compilations.getByName<KotlinJvmCompilation>("main")

        val classes = files(
            compilation.runtimeDependencyFiles,
            compilation.output.allOutputs
        )
        classpath(classes)
    }
}


application {
    mainClass.set("cli.MainKt")
}


tasks.register<Copy>("release") {
    group = "run"
    description = "Build the native executable"
    dependsOn("runReleaseExecutable$nativeTarget")
}

tasks.register("runOnGitHubAction") {
    group = "run"
    description = "CI with Github Actions : .github/workflows/release.yml"
    dependsOn("allTests", "allRun")
}