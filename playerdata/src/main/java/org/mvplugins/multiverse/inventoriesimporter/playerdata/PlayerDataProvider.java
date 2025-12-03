package org.mvplugins.multiverse.inventoriesimporter.playerdata;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.external.vavr.collection.HashMap;
import org.mvplugins.multiverse.external.vavr.collection.Map;
import org.mvplugins.multiverse.inventories.utils.InvLogging;

public interface PlayerDataProvider {
    PlayerDataImporter getImporter();

    static PlayerDataProvider get() {
        return Holder.INSTANCE;
    }

    final class Holder {
        private static final PlayerDataProvider INSTANCE;

        private static final Map<MinecraftVersion.Range, String> VERSION_MAPPING = HashMap.of(
                MinecraftVersion.rangeAtMost("1.19.3"), "1_18_2",
                MinecraftVersion.range("1.19.4", "1.20.2"), "1_20_2",
                MinecraftVersion.range("1.20.3", "1.21.4"), "1_21_4",
                MinecraftVersion.rangeAtLeast("1.21.5"), "1_21_10"
        );

        static {
            @NotNull String minecraftVersionString = Bukkit.getServer().getMinecraftVersion();
            MinecraftVersion minecraftVersion = MinecraftVersion.fromString(minecraftVersionString);
            String targetVersionPackage = VERSION_MAPPING
                    .filter((range, s) -> range.includes(minecraftVersion))
                    .head()
                    ._2();
            try {
                INSTANCE = Class
                        .forName("org.mvplugins.multiverse.inventoriesimporter.playerdata_"
                                + targetVersionPackage + ".PlayerDataProvider_" + targetVersionPackage)
                        .asSubclass(PlayerDataProvider.class)
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize PlayerDataProvider for Minecraft version "
                        + minecraftVersionString, e);
            }
        }

        private Holder() {/* no instance */}
    }
}
