package org.mvplugins.multiverse.inventoriesimporter.multiverseInventoriesImporter;

import org.bukkit.plugin.java.JavaPlugin;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.command.MVCommandManager;
import org.mvplugins.multiverse.inventoriesimporter.multiverseInventoriesImporter.commands.PlayerdataImportCommand;

public final class MultiverseInventoriesImporter extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        MultiverseCoreApi mvCoreApi = MultiverseCoreApi.get();
        MVCommandManager commandManager = mvCoreApi.getServiceLocator().getService(MVCommandManager.class);
        commandManager.registerCommand(new PlayerdataImportCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
