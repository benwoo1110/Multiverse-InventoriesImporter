package org.mvplugins.multiverse.inventoriesimporter.multiverseInventoriesImporter.commands;

import com.google.common.io.Files;
import org.bukkit.World;
import org.mvplugins.multiverse.core.command.MVCommandIssuer;
import org.mvplugins.multiverse.external.acf.commands.annotation.CommandCompletion;
import org.mvplugins.multiverse.external.acf.commands.annotation.CommandPermission;
import org.mvplugins.multiverse.external.acf.commands.annotation.Description;
import org.mvplugins.multiverse.external.acf.commands.annotation.Subcommand;
import org.mvplugins.multiverse.external.acf.commands.annotation.Syntax;
import org.mvplugins.multiverse.inventories.MultiverseInventoriesApi;
import org.mvplugins.multiverse.inventories.profile.ProfileDataSource;
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

public final class PlayerdataImportCommand extends MVInvImporterCommand {

    @Subcommand("playerdata import")
    @Syntax("<world>")
    @CommandPermission("multiverse.inventories.importplayerdata")
    @CommandCompletion("@worldwithplayerdata")
    @Description("Import player data from the world's playerdata folder.")
    void onCommand(MVCommandIssuer issuer, World world) {
        PlayerDataImporter playerDataExtractor = PlayerDataProvider.get().getImporter();
        ProfileDataSource profileDataSource = MultiverseInventoriesApi.get().getProfileDataSource();
        WorldGroupManager worldGroupManager = MultiverseInventoriesApi.get().getWorldGroupManager();
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

        for (File playerDataFile : files) {
            if (!Files.getFileExtension(playerDataFile.getName()).equals("dat")) {
                continue;
            }
            UUID playerUUID = UUID.fromString(Files.getNameWithoutExtension(playerDataFile.getName()));
            playerDataExtractor.readPlayerDataFromFile(playerDataFile)
                    .onSuccess(profileData -> playerDataFutures.add(profileDataSource
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
                                    .toArray(CompletableFuture[]::new)))));
        }
        CompletableFuture.allOf(playerDataFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> issuer.sendMessage("Successfully imported all player data from " + world.getName() + "."));
    }
}
