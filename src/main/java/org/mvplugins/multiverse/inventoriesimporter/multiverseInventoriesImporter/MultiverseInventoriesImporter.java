package org.mvplugins.multiverse.inventoriesimporter.multiverseInventoriesImporter;

import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.core.inject.PluginServiceLocator;
import org.mvplugins.multiverse.core.inject.PluginServiceLocatorFactory;
import org.mvplugins.multiverse.core.module.MultiverseModule;
import org.mvplugins.multiverse.core.module.MultiverseModuleBinder;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.external.jakarta.inject.Provider;
import org.mvplugins.multiverse.inventories.MultiverseInventoriesApi;
import org.mvplugins.multiverse.inventories.dataimport.DataImportManager;
import org.mvplugins.multiverse.inventories.dataimport.DataImporter;
import org.mvplugins.multiverse.inventories.utils.InvLogging;
import org.mvplugins.multiverse.inventoriesimporter.multiverseInventoriesImporter.commands.MVInvImporterCommand;

@Service
public final class MultiverseInventoriesImporter extends MultiverseModule {

    @Inject
    private Provider<DataImportManager> dataImportManager;

    @Override
    public void onEnable() {
        initializeDependencyInjection(new MultiverseInventoriesImporterPluginBinder(this));
        registerCommands(MVInvImporterCommand.class);
        serviceLocator.getAllServices(DataImporter.class)
                .forEach(dataImporter -> dataImportManager.get().register(dataImporter));
    }

    @Override
    public void onDisable() {
        shutdownDependencyInjection();
    }

    @Override
    public double getTargetCoreVersion() {
        return 5.2;
    }

    @Override
    protected void initializeDependencyInjection(MultiverseModuleBinder<? extends MultiverseModule> pluginBinder) {
        serviceLocator = PluginServiceLocatorFactory.get()
                .registerPlugin(pluginBinder, MultiverseInventoriesApi.get().getServiceLocator())
                .flatMap(PluginServiceLocator::enable)
                .getOrElseThrow(exception -> {
                    InvLogging.severe("Failed to initialize dependency injection!");
                    getServer().getPluginManager().disablePlugin(this);
                    return new RuntimeException(exception);
                });
    }
}
