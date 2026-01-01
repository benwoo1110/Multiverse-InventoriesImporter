package org.mvplugins.multiverse.inventoriesimporter.multiverseInventoriesImporter.commands;

import com.google.common.io.Files;
import org.bukkit.World;
import org.jspecify.annotations.NonNull;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.core.command.MVCommandIssuer;
import org.mvplugins.multiverse.external.acf.commands.annotation.CommandCompletion;
import org.mvplugins.multiverse.external.acf.commands.annotation.CommandPermission;
import org.mvplugins.multiverse.external.acf.commands.annotation.Description;
import org.mvplugins.multiverse.external.acf.commands.annotation.Subcommand;
import org.mvplugins.multiverse.external.acf.commands.annotation.Syntax;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.external.vavr.control.Try;
import org.mvplugins.multiverse.inventories.profile.ProfileDataSource;
import org.mvplugins.multiverse.inventories.profile.data.ProfileData;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroup;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroupManager;
import org.mvplugins.multiverse.inventories.profile.key.ContainerType;
import org.mvplugins.multiverse.inventories.profile.key.GlobalProfileKey;
import org.mvplugins.multiverse.inventories.profile.key.ProfileKey;
import org.mvplugins.multiverse.inventories.profile.key.ProfileTypes;
import org.mvplugins.multiverse.inventoriesimporter.playerdata.PlayerDataImporter;
import org.mvplugins.multiverse.inventoriesimporter.playerdata.PlayerDataProvider;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
final class PlayerdataImportCommand extends MVInvImporterCommand {

    private final ProfileDataSource profileDataSource;
    private final WorldGroupManager worldGroupManager;

    @Inject
    PlayerdataImportCommand(ProfileDataSource profileDataSource, WorldGroupManager worldGroupManager) {
        this.profileDataSource = profileDataSource;
        this.worldGroupManager = worldGroupManager;
    }

    @Subcommand("playerdata import")
    @Syntax("<world>")
    @CommandPermission("multiverse.inventories.importplayerdata")
    @CommandCompletion("@worldwithplayerdata")
    @Description("Import player data from the world's playerdata folder.")
    void onCommand(MVCommandIssuer issuer, World world) {
        PlayerDataImporter playerDataExtractor = PlayerDataProvider.get().getImporter();
        List<WorldGroup> groupsForWorld = worldGroupManager.getGroupsForWorld(world.getName());

        Path worldPath = world.getWorldFolder().toPath();
        File playerDataPath = worldPath.resolve("playerdata").toFile();
        if (!playerDataPath.isDirectory()) {
            issuer.sendMessage("World's playerdata folder does not exist: " + world.getName());
            return;
        }

        List<CompletableFuture<Void>> playerDataFutures = new ArrayList<>();
        File[] files = playerDataPath.listFiles();
        if (files == null) {
            issuer.sendMessage("No player data files found in the world's playerdata folder: " + world.getName());
            return;
        }

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();
        for (File playerDataFile : files) {
            if (!Files.getFileExtension(playerDataFile.getName()).equals("dat")) {
                continue;
            }
            Try.of(() -> UUID.fromString(Files.getNameWithoutExtension(playerDataFile.getName())))
                    .onFailure(ex -> issuer.sendError("Skipping player data file due to invalid UUID: " + playerDataFile.getName()))
                    .flatMap(playerUUID -> playerDataExtractor.readPlayerDataFromFile(playerDataFile)
                            .mapTry(profileData -> applyToRelevantProfiles(world, profileData, playerUUID, groupsForWorld))
                            .peek(future -> future
                                    .thenRun(successCount::incrementAndGet)
                                    .exceptionally(ex -> {
                                        issuer.sendError("Failed to import player data for UUID " + playerUUID + ": " + ex.getMessage());
                                        errorCount.incrementAndGet();
                                        return null;
                                    })))
                    .onFailure(ex -> errorCount.incrementAndGet())
                    .onSuccess(playerDataFutures::add);
        }
        CompletableFuture.allOf(playerDataFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    issuer.sendMessage("Successfully imported " + successCount.get() + " player data from '" + world.getName() + "' world.");
                    if (errorCount.get() > 0) {
                        issuer.sendError("Failed to import " + errorCount.get() + " player data files. Check the logs for details.");
                    }
                });
    }

    private @NonNull CompletableFuture<Void> applyToRelevantProfiles(World world, ProfileData profileData, UUID playerUUID, List<WorldGroup> groupsForWorld) {
        return profileDataSource
                .getGlobalProfile(GlobalProfileKey.of(playerUUID))
                .thenCompose(globalProfile -> {
                    globalProfile.setLoadOnLogin(true); //todo: make it only for affected players
                    return profileDataSource.updateGlobalProfile(globalProfile);
                })
                .thenCompose(ignore -> profileDataSource
                        .getPlayerProfile(ProfileKey.of(
                                ContainerType.WORLD,
                                world.getName(),
                                ProfileTypes.getDefault(),
                                playerUUID))
                        .thenCompose(playerProfile -> {
                            playerProfile.update(profileData);
                            return profileDataSource.updatePlayerProfile(playerProfile);
                        }))
                .thenCompose(ignore -> CompletableFuture.allOf(groupsForWorld.stream()
                        .map(group -> profileDataSource
                                .getPlayerProfile(ProfileKey.of(
                                        ContainerType.GROUP,
                                        group.getName(),
                                        ProfileTypes.getDefault(),
                                        playerUUID))
                                .thenCompose(playerProfile -> {
                                    playerProfile.update(profileData, group.getApplicableShares());
                                    return profileDataSource.updatePlayerProfile(playerProfile);
                                }))
                        .toArray(CompletableFuture[]::new)));
    }
}
