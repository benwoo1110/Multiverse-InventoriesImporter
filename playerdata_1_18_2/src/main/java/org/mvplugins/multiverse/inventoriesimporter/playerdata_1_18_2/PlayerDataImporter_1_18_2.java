package org.mvplugins.multiverse.inventoriesimporter.playerdata_1_18_2;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.storage.DataVersion;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.mvplugins.multiverse.external.vavr.control.Try;
import org.mvplugins.multiverse.inventories.profile.data.ProfileData;
import org.mvplugins.multiverse.inventories.profile.data.ProfileDataSnapshot;
import org.mvplugins.multiverse.inventories.share.Sharables;
import org.mvplugins.multiverse.inventories.util.PlayerStats;
import org.mvplugins.multiverse.inventories.utils.InvLogging;
import org.mvplugins.multiverse.inventoriesimporter.playerdata.PlayerDataImporter;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Optional;

public class PlayerDataImporter_1_18_2 implements PlayerDataImporter {

    public Try<ProfileData> readPlayerDataFromFile(File playerDataFile) {
        return loadAndUpgradePlayerData(playerDataFile)
                .flatMapTry(this::convertToProfileData)
                .onFailure(throwable -> InvLogging.warning("Failed to load player data from file %s: %s",
                        playerDataFile.getName(), throwable.getMessage()));
    }

    private Try<CompoundTag> loadAndUpgradePlayerData(File playerDataFile) {
        return Try.of(() -> NbtIo.readCompressed(playerDataFile))
                .mapTry(compoundTag -> {
                    int dataVersion = compoundTag.getInt("DataVersion");
                    Tag upgradedTag = DataFixers.getDataFixer()
                            .update(
                                    DataFixTypes.PLAYER.getType(),
                                    new Dynamic<>(NbtOps.INSTANCE, compoundTag),
                                    dataVersion,
                                    SharedConstants.getCurrentVersion().getDataVersion().getVersion())
                            .getValue();
                    if (!(upgradedTag instanceof CompoundTag upgradedCompoundTag)) {
                        throw new IllegalStateException("Upgraded player data is not a CompoundTag");
                    }
                    upgradedCompoundTag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
                    InvLogging.finest("Upgraded player data from version %d to %d", dataVersion, upgradedCompoundTag.getInt("DataVersion"));
                    return upgradedCompoundTag;
                });
    }

    private Try<ProfileData> convertToProfileData(CompoundTag playerData) {
        return Try.of(() -> {
            int dataVersion = playerData.getInt("DataVersion");
            InvLogging.finest("Data version: %s", dataVersion);
            ListTag inventory = playerData.getList("Inventory", Tag.TAG_COMPOUND);

            ProfileData profileData = new ProfileDataSnapshot();

            profileData.set(Sharables.ARMOR, new ItemStack[]{
                    extractItemStackFromSlot(inventory, dataVersion, 100),
                    extractItemStackFromSlot(inventory, dataVersion, 101),
                    extractItemStackFromSlot(inventory, dataVersion, 102),
                    extractItemStackFromSlot(inventory, dataVersion, 103)
            });
            // ADVANCEMENTS
            // BED_SPAWN
            profileData.set(Sharables.ENDER_CHEST, extractInventory(
                    playerData.getList("EnderItems", Tag.TAG_COMPOUND),
                    dataVersion,
                    PlayerStats.ENDER_CHEST_SIZE
            ));
            profileData.set(Sharables.EXHAUSTION, playerData.getFloat("foodExhaustionLevel"));
            profileData.set(Sharables.EXPERIENCE, playerData.getFloat("XpP"));
            profileData.set(Sharables.FALL_DISTANCE, (float) playerData.getDouble("fall_distance"));
            profileData.set(Sharables.FIRE_TICKS, (int) playerData.getShort("Fire"));
            profileData.set(Sharables.FOOD_LEVEL, playerData.getInt("foodLevel"));
            // GAME_STATISTICS
            profileData.set(Sharables.HEALTH, (double) playerData.getFloat("Health"));
            profileData.set(Sharables.INVENTORY, extractInventory(
                    inventory,
                    dataVersion,
                    PlayerStats.INVENTORY_SIZE
            ));
            // LAST_LOCATION
            profileData.set(Sharables.LEVEL, playerData.getInt("XpLevel"));
            // MAXIMUM_AIR
            // MAX_HEALTH
            profileData.set(Sharables.OFF_HAND, extractItemStackFromSlot(inventory, dataVersion, -106));
            // POTIONS
            // RECIPES
            profileData.set(Sharables.REMAINING_AIR, (int) playerData.getShort("Air"));
            profileData.set(Sharables.SATURATION, playerData.getFloat("foodSaturationLevel"));
            profileData.set(Sharables.TOTAL_EXPERIENCE, playerData.getInt("XpTotal"));

            return profileData;
        });
    }

    private ItemStack extractItemStackFromSlot(@Nullable ListTag inventoryList, int dataVersion, int targetSlot) {
        return Optional.ofNullable(inventoryList)
                .flatMap(list -> list.stream()
                        .filter(tagData -> tagData instanceof CompoundTag)
                        .map(tagData -> (CompoundTag) tagData)
                        .filter(invData -> invData.getByte("Slot") == targetSlot)
                        .findFirst()
                        .map(itemData -> extractItemStack(itemData, dataVersion)))
                .orElse(null);
    }

    private ItemStack[] extractInventory(@Nullable ListTag inventoryList, int dataVersion, int inventorySize) {
        return Optional.ofNullable(inventoryList)
                .map(list -> list.stream()
                        .filter(tagData -> tagData instanceof CompoundTag)
                        .map(tagData -> (CompoundTag) tagData)
                        .filter(invData -> {
                            int slot = invData.getByte("Slot");
                            return slot >= 0 && slot < inventorySize;
                        })
                        .map(itemData -> extractItemStack(itemData, dataVersion))
                        .toArray(size -> new ItemStack[inventorySize]))
                .orElseGet(() -> new ItemStack[inventorySize]);
    }

    private @Nullable ItemStack extractItemStack(@Nullable CompoundTag itemStackData, int dataVersion) {
        if (itemStackData == null) {
            return null;
        }
        itemStackData.putInt("DataVersion", dataVersion);
        return Try.of(() -> {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    NbtIo.writeCompressed(itemStackData, byteArrayOutputStream);
                    byteArrayOutputStream.close();
                    return Bukkit.getUnsafe().deserializeItem(byteArrayOutputStream.toByteArray());
                })
                .onFailure(throwable -> InvLogging.warning("Failed to deserialize item: %s", throwable.getMessage()))
                .andFinally(() -> itemStackData.remove("DataVersion"))
                .getOrNull();
    }
}
