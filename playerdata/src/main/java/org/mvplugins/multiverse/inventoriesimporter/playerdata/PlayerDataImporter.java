package org.mvplugins.multiverse.inventoriesimporter.playerdata;

import org.mvplugins.multiverse.external.vavr.control.Try;
import org.mvplugins.multiverse.inventories.profile.data.ProfileData;

import java.io.File;

public interface PlayerDataImporter {
    Try<ProfileData> readPlayerDataFromFile(File playerDataFile);
}
