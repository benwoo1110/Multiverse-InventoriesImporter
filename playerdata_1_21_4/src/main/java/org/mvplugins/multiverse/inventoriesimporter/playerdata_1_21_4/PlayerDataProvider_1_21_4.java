package org.mvplugins.multiverse.inventoriesimporter.playerdata_1_21_4;

import org.mvplugins.multiverse.inventoriesimporter.playerdata.PlayerDataImporter;
import org.mvplugins.multiverse.inventoriesimporter.playerdata.PlayerDataProvider;

public class PlayerDataProvider_1_21_4 implements PlayerDataProvider {

    private final PlayerDataImporter playerDataImporter = new PlayerDataImporter_1_21_4();

    @Override
    public PlayerDataImporter getImporter() {
        return playerDataImporter;
    }
}
