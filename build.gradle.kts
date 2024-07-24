import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation


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

kotlin {
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    val desktop = jvm("desktop") {
        // cli.MainKt
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    sourceSets {
        val commonMain by getting  {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktor_version")
            }
        }

        val desktopMain: KotlinSourceSet by getting {
            dependsOn(commonMain)
            dependencies {

                implementation("io.ktor:ktor-client-cio:$ktor_version")
            }
        }

        val nativeMain by getting {
            dependsOn(commonMain)

            dependencies {
                implementation("io.ktor:ktor-client-winhttp:$ktor_version")
            }
        }


    }
    tasks.withType<JavaExec> {
        // code to make run task in kotlin multiplatform work
        val compilation = desktop.compilations.getByName<KotlinJvmCompilation>("main")

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