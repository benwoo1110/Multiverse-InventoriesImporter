package org.mvplugins.multiverse.inventoriesimporter.multiverseInventoriesImporter;

import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.core.module.MultiverseModule;
import org.mvplugins.multiverse.inventoriesimporter.multiverseInventoriesImporter.commands.MVInvImporterCommand;

@Service
public final class MultiverseInventoriesImporter extends MultiverseModule {

    @Override
    public void onEnable() {
        initializeDependencyInjection(new MultiverseInventoriesImporterPluginBinder(this));
        registerCommands(MVInvImporterCommand.class);
    }

    @Override
    public void onDisable() {

    }

    @Override
    public double getTargetCoreVersion() {
        return 5.2;
    }
}
