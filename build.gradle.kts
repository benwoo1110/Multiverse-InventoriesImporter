import org.gradle.api.tasks.compile.JavaCompile

plugins {
    `my-conventions`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19" apply false
    id("com.gradleup.shadow") version "9.2.2"
}

group = "org.mvplugins.multiverse.inventoriesimporter"
version = "1.0.0"
description = "Multiverse-InventoriesImporter"

repositories {
    maven {
        name = "codemc"
        url = uri("https://repo.codemc.org/repository/maven-releases/")
    }

    maven {
        name = "helpchatRepoReleases"
        url = uri("https://repo.helpch.at/releases/")
    }

    maven {
        name = "onarandombox"
        url = uri("https://repo.onarandombox.com/content/groups/public/")
    }
}

dependencies {
    // server api
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")

    // plugins for migration
    compileOnly("uk.co:MultiInv:3.0.6") {
        exclude(group = "*", module = "*")
    }
    compileOnly("me.drayshak:WorldInventories:1.0.2") {
        exclude(group = "*", module = "*")
    }
    // perworldinventory is weird and has snakeyaml included in the jar, so we can only use compileOnly for build to work properly
    compileOnly("me.ebonjaeger:perworldinventory-kt:2.3.2") {
        exclude(group = "*", module = "*")
    }

    // playerdata nms
    implementation(project(":playerdata"))
    runtimeOnly(project(":playerdata_1_18_2", configuration = "reobf"))
    runtimeOnly(project(":playerdata_1_20_2", configuration = "reobf"))
    runtimeOnly(project(":playerdata_1_21_4", configuration = "reobf"))
    runtimeOnly(project(":playerdata_1_21_10"))

    // hk2 annotations
    compileOnly("org.glassfish.hk2:hk2-api:3.1.1") {
        exclude(group = "*", module = "*")
    }
    annotationProcessor("org.glassfish.hk2:hk2-metadata-generator:3.1.1")
    testAnnotationProcessor("org.glassfish.hk2:hk2-metadata-generator:3.1.1")
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version.toString()
    )

    inputs.properties(props)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(props)
    }

    outputs.upToDateWhen { false }
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
