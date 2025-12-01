package org.mvplugins.multiverse.inventoriesimporter.playerdata_1_21_10;

import org.mvplugins.multiverse.inventoriesimporter.playerdata.PlayerDataImporter;
import org.mvplugins.multiverse.inventoriesimporter.playerdata.PlayerDataProvider;

public class PlayerDataProvider_1_21_10 implements PlayerDataProvider {

    private final PlayerDataImporter importer = new PlayerDataImporter_1_21_10();

    @Override
    public PlayerDataImporter getImporter() {
        return importer;
    }
}
