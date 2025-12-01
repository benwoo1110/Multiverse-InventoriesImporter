package org.mvplugins.multiverse.inventoriesimporter.playerdata_1_20_2;

import org.mvplugins.multiverse.inventoriesimporter.playerdata.PlayerDataImporter;
import org.mvplugins.multiverse.inventoriesimporter.playerdata.PlayerDataProvider;

public class PlayerDataProvider_1_20_2 implements PlayerDataProvider {

    private final PlayerDataImporter playerDataImporter = new PlayerDataImporter_1_20_2();

    @Override
    public PlayerDataImporter getImporter() {
        return playerDataImporter;
    }
}
