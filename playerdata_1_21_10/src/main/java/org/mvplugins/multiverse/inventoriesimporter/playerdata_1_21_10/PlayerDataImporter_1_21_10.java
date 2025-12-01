package org.mvplugins.multiverse.inventoriesimporter.playerdata_1_21_10;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
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

public class PlayerDataImporter_1_21_10 implements PlayerDataImporter {

    public Try<ProfileData> readPlayerDataFromFile(File playerDataFile) {
        return loadAndUpgradePlayerData(playerDataFile)
                .flatMapTry(this::convertToProfileData)
                .onFailure(throwable -> InvLogging.warning("Failed to load player data from file %s: %s",
                        playerDataFile.getName(), throwable.getMessage()));
    }

    private Try<CompoundTag> loadAndUpgradePlayerData(File playerDataFile) {
        return Try.of(() -> NbtIo.readCompressed(playerDataFile.toPath(), NbtAccounter.unlimitedHeap()))
                .mapTry(compoundTag -> {
                    int dataVersion = compoundTag.getIntOr("DataVersion", -1);
                    return DataFixTypes.PLAYER.updateToCurrentVersion(DataFixers.getDataFixer(), compoundTag, dataVersion);
                });
    }

    private Try<ProfileData> convertToProfileData(CompoundTag playerData) {
        return Try.of(() -> {
            int dataVersion = playerData.getIntOr("DataVersion", -1);
            InvLogging.finest("Data version: %s", dataVersion);
            CompoundTag equipment = playerData.getCompoundOrEmpty("equipment");
            ListTag inventory = playerData.getListOrEmpty("Inventory");

            ProfileData profileData = new ProfileDataSnapshot();

            profileData.set(Sharables.ARMOR, new ItemStack[]{
                    extractItemStack(equipment.getCompoundOrEmpty("feet"), dataVersion),
                    extractItemStack(equipment.getCompoundOrEmpty("legs"), dataVersion),
                    extractItemStack(equipment.getCompoundOrEmpty("chest"), dataVersion),
                    extractItemStack(equipment.getCompoundOrEmpty("head"), dataVersion)
            });
            // ADVANCEMENTS
            // BED_SPAWN
            profileData.set(Sharables.ENDER_CHEST, extractInventory(
                    playerData.getListOrEmpty("EnderItems"),
                    dataVersion,
                    PlayerStats.ENDER_CHEST_SIZE
            ));
            profileData.set(Sharables.EXHAUSTION, playerData.getFloatOr("foodExhaustionLevel", PlayerStats.EXHAUSTION));
            profileData.set(Sharables.EXPERIENCE, playerData.getFloatOr("XpP", PlayerStats.EXPERIENCE));
            profileData.set(Sharables.FALL_DISTANCE, (float) playerData.getDoubleOr("fall_distance", PlayerStats.FALL_DISTANCE));
            profileData.set(Sharables.FIRE_TICKS, (int) playerData.getShortOr("Fire", (short) PlayerStats.FIRE_TICKS));
            profileData.set(Sharables.FOOD_LEVEL, playerData.getIntOr("foodLevel", PlayerStats.FOOD_LEVEL));
            // GAME_STATISTICS
            profileData.set(Sharables.HEALTH, (double) playerData.getFloatOr("Health", (float) PlayerStats.HEALTH));
            profileData.set(Sharables.INVENTORY, extractInventory(
                    inventory,
                    dataVersion,
                    PlayerStats.INVENTORY_SIZE
            ));
            // LAST_LOCATION
            profileData.set(Sharables.LEVEL, playerData.getIntOr("XpLevel", PlayerStats.LEVEL));
            // MAXIMUM_AIR
            // MAX_HEALTH
            profileData.set(Sharables.OFF_HAND, extractItemStack(equipment.getCompoundOrEmpty("offhand"), dataVersion));
            // POTIONS
            // RECIPES
            profileData.set(Sharables.REMAINING_AIR, (int) playerData.getShortOr("Air", (short) PlayerStats.REMAINING_AIR));
            profileData.set(Sharables.SATURATION, playerData.getFloatOr("foodSaturationLevel", PlayerStats.SATURATION));
            profileData.set(Sharables.TOTAL_EXPERIENCE, playerData.getIntOr("XpTotal", PlayerStats.TOTAL_EXPERIENCE));

            return profileData;
        });
    }

    private ItemStack extractItemStackFromSlot(@Nullable ListTag inventoryList, int dataVersion, int targetSlot) {
        return Optional.ofNullable(inventoryList)
                .flatMap(list -> list.stream()
                        .filter(tagData -> tagData instanceof CompoundTag)
                        .map(tagData -> (CompoundTag) tagData)
                        .filter(invData -> invData.getByteOr("Slot", (byte) -1) == targetSlot)
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
                            int slot = invData.getByteOr("Slot", (byte) -1);
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
        CompoundTag itemStackDataCopy = itemStackData.copy();
        itemStackDataCopy.remove("Slot");
        itemStackDataCopy.putInt("DataVersion", dataVersion);
        return Try.of(() -> {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            NbtIo.writeCompressed(itemStackDataCopy, byteArrayOutputStream);
            byteArrayOutputStream.close();
            return ItemStack.deserializeBytes(byteArrayOutputStream.toByteArray());
        }).onFailure(throwable -> InvLogging.warning("Failed to deserialize item: %s", throwable.getMessage()))
                .getOrNull();
    }
}
