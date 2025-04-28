import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.awt.GraphicsEnvironment
import java.io.ByteArrayOutputStream
import java.util.*

plugins {
    application
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.collektive)
    alias(libs.plugins.kotlin.jvm)
 //   alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.multiJvmTesting)
    alias(libs.plugins.taskTree)
}

repositories {
    mavenCentral()
}
/*
 * Only required if you plan to use Protelis, remove otherwise
 */
sourceSets {
    main {
        dependencies {
            implementation(libs.bundles.alchemist)
            implementation(libs.bundles.collektive)
            implementation(libs.data2viz.geojson)
            implementation(libs.bundles.jackson)
            implementation(libs.bundles.aislib)
            testImplementation(kotlin("test"))
        }
        resources {
            srcDir("src/main/yaml")
        }
    }
    test {
        resources {
            srcDir("src/main/resources")

        }
    }
}

tasks.test {
    useJUnitPlatform()
}

multiJvm {
    jvmVersionForCompilation.set(21)
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.arrow.core)
    if (!GraphicsEnvironment.isHeadless()) {
        implementation("it.unibo.alchemist:alchemist-swingui:${libs.versions.alchemist.get()}")
    }
}

val exportMetricInCSV by tasks.registering(JavaExec::class) {
    dependsOn(tasks.withType<KotlinCompile>())
    description = "Exports the data rate metric into a csv"
    val fileName = "data/metric_data.csv"
    outputs.dir(file("data"))
    mainClass.set("it.unibo.clustered.seaborn.comm.Metric")
    classpath = sourceSets["main"].runtimeClasspath
    args(fileName)
}

val createGpxRoutes by tasks.registering(JavaExec::class) {
    dependsOn(tasks.withType<KotlinCompile>())
    group = alchemistGroupGraphic
    description = "Creates GPX routes given the raw AIS data"
    val resources = "ais-data"
    val inputFolder = "$resources/raw/202208" // August 2022
    val selectedDay = "20220818" // 18 August 2022
    val outputFolder = layout.buildDirectory.map { it.asFile.resolve("resources/main/navigation-routes") }
    inputs.dir(file(inputFolder))
    outputs.dir(outputFolder)
    mainClass.set("it.unibo.util.gpx.ParseRawNavigationData")
    classpath = sourceSets["main"].runtimeClasspath
    args(inputFolder, outputFolder.get().absolutePath, selectedDay)
}

// Heap size estimation for batches
val maxHeap: Long? by project
val heap: Long = maxHeap ?: if (System.getProperty("os.name").lowercase().contains("linux")) {
    ByteArrayOutputStream().use { output ->
        exec {
            executable = "bash"
            args = listOf("-c", "cat /proc/meminfo | grep MemAvailable | grep -o '[0-9]*'")
            standardOutput = output
        }
        output.toString().trim().toLong() / 1024
    }.also { println("Detected ${it}MB RAM available.") } * 9 / 10
} else {
    // Guess 16GB RAM of which 2 used by the OS
    14 * 1024L
}
val taskSizeFromProject: Int? by project
val taskSize = taskSizeFromProject ?: 512
val threadCount = maxOf(1, minOf(Runtime.getRuntime().availableProcessors(), heap.toInt() / taskSize))
val alchemistGroupBatch = "Run batch simulations"
val alchemistGroupGraphic = "Run graphic simulations with Alchemist"

/*
 * This task is used to run all experiments in sequence
 */
val runAllGraphic by tasks.register<DefaultTask>("runAllGraphic") {
    group = alchemistGroupGraphic
    description = "Launches all simulations with the graphic subsystem enabled"
}
val runAllBatch by tasks.register<DefaultTask>("runAllBatch") {
    group = alchemistGroupBatch
    description = "Launches all experiments"
}

fun String.capitalizeString(): String =
    this.replaceFirstChar {
        if (it.isLowerCase()) {
            it.titlecase(
                Locale.getDefault(),
            )
        } else {
            it.toString()
        }
    }

/*
 * Scan the folder with the simulation files, and create a task for each one of them.
 */
File(rootProject.rootDir.path + "/src/main/yaml").listFiles()
    ?.filter { it.extension == "yml" }
    ?.sortedBy { it.nameWithoutExtension }
    ?.forEach {
        fun basetask(name: String, additionalConfiguration: JavaExec.() -> Unit = {}) = tasks.register<JavaExec>(name) {
            description = "Launches graphic simulation ${it.nameWithoutExtension}"
            mainClass.set("it.unibo.alchemist.Alchemist")
            classpath = sourceSets["main"].runtimeClasspath
            dependsOn(createGpxRoutes)
            args("run", it.absolutePath)
            javaLauncher.set(
                javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(multiJvm.latestJava))
                },
            )
            if (System.getenv("CI") == "true") {
                args("--override", "terminate: { type: AfterTime, parameters: [2] } ")
            } else {
                this.additionalConfiguration()
            }
        }
        val capitalizedName = it.nameWithoutExtension.capitalizeString()
        val graphic by basetask("run${capitalizedName}Graphic") {
            group = alchemistGroupGraphic
            args(
                "--override",
                "monitors: { type: SwingGUI, parameters: { graphics: effects/${it.nameWithoutExtension}.json } }",
                "--override",
                "launcher: { parameters: { batch: [], autoStart: true } }",
                "--verbosity",
                "error"
            )
        }
        runAllGraphic.dependsOn(graphic)
        val batch by basetask("run${capitalizedName}Batch") {
            group = alchemistGroupBatch
            description = "Launches batch experiments for $capitalizedName"
            maxHeapSize = "${minOf(heap.toInt(), Runtime.getRuntime().availableProcessors() * taskSize)}m"
            File("data").mkdirs()
            args(
                "--verbosity",
                "error"
            )
        }
        runAllBatch.dependsOn(batch)
    }

tasks.withType(KotlinCompile::class).all {
    compilerOptions {
        allWarningsAsErrors = false
    }
}

tasks.withType<Test> {
    this.testLogging {
        this.showStandardStreams = true
    }
    jvmArgs("--add-opens=java.base/java.util=ALL-UNNAMED")
}
