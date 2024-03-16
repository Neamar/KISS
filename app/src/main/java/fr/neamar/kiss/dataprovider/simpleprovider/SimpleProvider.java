package fr.neamar.kiss.dataprovider.simpleprovider;

import java.util.List;

import fr.neamar.kiss.dataprovider.IProvider;
import fr.neamar.kiss.pojo.Pojo;

/**
 * Unlike normal providers, simple providers are not Android Services but classic Android class
 * Android Services are expensive to create, and use a lot of memory,
 * so whenever we can, we avoid using them.
 */
public abstract class SimpleProvider<T extends Pojo> implements IProvider<T> {
    @Override
    public void reload() {
        // Simple providers can't be reloaded
    }

    @Override
    public final boolean isLoaded() {
        return true;
    }

    @Override
    public boolean mayFindById(String id) {
        return false;
    }

    @Override
    public T findById(String id) {
        return null;
    }

    @Override
    public List<T> getPojos() {
        return null;
    }
}
