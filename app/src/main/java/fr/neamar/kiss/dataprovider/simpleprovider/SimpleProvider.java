package fr.neamar.kiss.dataprovider.simpleprovider;

import androidx.annotation.Nullable;

import java.util.List;

import fr.neamar.kiss.dataprovider.IProvider;
import fr.neamar.kiss.pojo.Pojo;

/**
 * Unlike normal providers, simple providers are not Android Services but classic Android class
 * Android Services are expensive to create, and use a lot of memory,
 * so whenever we can, we avoid using them.
 */
public abstract class SimpleProvider<T extends Pojo> implements IProvider<T> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void reload() {
        // Simple providers doesn't reload anything by default
    }

    /**
     * Indicate whether this provider has already loaded it's data.
     *
     * @return always true for simple provider
     */
    @Override
    public final boolean isLoaded() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mayFindById(String id) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public Pojo findById(String id) {
        List<T> pojos = getPojos();
        if (pojos != null) {
            for (T pojo : pojos) {
                if (pojo.id.equals(id)) {
                    return pojo;
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> getPojos() {
        return null;
    }
}
