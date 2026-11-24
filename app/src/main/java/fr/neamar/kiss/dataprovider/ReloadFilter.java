package fr.neamar.kiss.dataprovider;

import java.util.List;
import java.util.function.Function;

import fr.neamar.kiss.pojo.Pojo;

/**
 * Filter for reload of pojos, this one can handle package names for all pojos implementint {@link fr.neamar.kiss.pojo.PojoWithPackageName}
 */
public interface ReloadFilter {
    /**
     * @param currentPojos list with current pojos
     * @param newPojos     list with all new pojos
     * @return merged list containing all pojos of {@code currentPojos} where matching ones were replaced by {@code newPojos}
     */
    <T extends Pojo> List<T> merge(List<T> currentPojos, List<T> newPojos);

    /**
     * @param fn function to execute with applied filter
     * @return all collected elements returned by {@code fn}
     */
    <T> List<T> execute(Function<String, List<T>> fn);

    /**
     * @param filter another filter
     * @return true, if this filter contains other filter
     */
    boolean contains(ReloadFilter filter);
}
