plugins {
    `my-conventions`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

dependencies {
    implementation(project(":playerdata"))

    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
}

tasks.withType<JavaCompile>().configureEach {
    // Override release for newer MC
    options.release = 17
}
