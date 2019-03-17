package fr.neamar.kiss.dataprovider.simpleprovider;

import fr.neamar.kiss.dataprovider.IProvider;
import fr.neamar.kiss.pojo.Pojo;

public abstract class SimpleProvider implements IProvider {
    @Override
    public void reload() {
        // Simple providers can't be reloaded
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public boolean mayFindById(String id) {
        return false;
    }

    @Override
    public Pojo findById(String id) {
        return null;
    }
}
