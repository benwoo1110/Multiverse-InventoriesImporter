plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Multiverse-InventoriesImporter"

include("playerdata")
include("playerdata_1_21_10")
include("playerdata_1_21_4")
include("playerdata_1_20_2")
include("playerdata_1_18_2")