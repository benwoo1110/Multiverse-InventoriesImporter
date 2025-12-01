plugins {
    `my-conventions`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

dependencies {
    implementation(project(":playerdata"))

    paperweight.paperDevBundle("1.20.2-R0.1-SNAPSHOT")
}

tasks.withType<JavaCompile>().configureEach {
    // Override release for newer MC
    options.release = 17
}
