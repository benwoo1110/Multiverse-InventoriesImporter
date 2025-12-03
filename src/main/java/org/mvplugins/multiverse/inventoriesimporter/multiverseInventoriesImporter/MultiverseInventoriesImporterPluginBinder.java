package org.mvplugins.multiverse.inventoriesimporter.multiverseInventoriesImporter;

import org.mvplugins.multiverse.core.module.MultiverseModuleBinder;
import org.mvplugins.multiverse.external.glassfish.hk2.utilities.binding.ScopedBindingBuilder;

final class MultiverseInventoriesImporterPluginBinder extends MultiverseModuleBinder<MultiverseInventoriesImporter> {
    MultiverseInventoriesImporterPluginBinder(MultiverseInventoriesImporter module) {
        super(module);
    }

    @Override
    protected ScopedBindingBuilder<MultiverseInventoriesImporter> bindPluginClass
            (ScopedBindingBuilder<MultiverseInventoriesImporter> bindingBuilder) {
        return super.bindPluginClass(bindingBuilder).to(MultiverseInventoriesImporter.class);
    }
}
