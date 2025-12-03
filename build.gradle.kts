import org.gradle.api.tasks.compile.JavaCompile

plugins {
    `my-conventions`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19" apply false
    id("com.gradleup.shadow") version "9.2.2"
}

group = "org.mvplugins.multiverse.inventoriesimporter"
version = "1.0-SNAPSHOT"
description = "Multiverse-InventoriesImporter"

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")

    implementation(project(":playerdata"))
    runtimeOnly(project(":playerdata_1_18_2", configuration = "reobf"))
    runtimeOnly(project(":playerdata_1_20_2", configuration = "reobf"))
    runtimeOnly(project(":playerdata_1_21_4", configuration = "reobf"))
    runtimeOnly(project(":playerdata_1_21_10", configuration = "reobf"))

    compileOnly("org.glassfish.hk2:hk2-api:3.1.1") {
        exclude(group = "*", module = "*")
    }
    annotationProcessor("org.glassfish.hk2:hk2-metadata-generator:3.1.1")
    testAnnotationProcessor("org.glassfish.hk2:hk2-metadata-generator:3.1.1")
}

tasks.withType<JavaCompile>().configureEach {
    doFirst {
        this@configureEach.options.compilerArgs.add(
            "-Aorg.glassfish.hk2.metadata.location=META-INF/hk2-locator/${project.description}"
        )
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    mergeServiceFiles()
    // Needed for mergeServiceFiles to work properly in Shadow 9+
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

tasks {
    compileJava {
        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release = 17
    }

    javadoc {
        options.encoding = "UTF-8"
    }
}
