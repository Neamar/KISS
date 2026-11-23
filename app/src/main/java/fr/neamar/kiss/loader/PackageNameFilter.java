package fr.neamar.kiss.loader;

import androidx.annotation.NonNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.neamar.kiss.dataprovider.ReloadFilter;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoWithPackageName;
import fr.neamar.kiss.utils.Log;

public class PackageNameFilter implements ReloadFilter {

    private static final String TAG = PackageNameFilter.class.getSimpleName();

    private final Set<String> packageNames;

    public PackageNameFilter(@NonNull String[] packageNames) {
        this(new HashSet<>(Arrays.asList(packageNames)));
    }

    public PackageNameFilter(@NonNull String packageName) {
        this(new HashSet<>(Collections.singletonList(packageName)));
    }

    private PackageNameFilter(@NonNull Set<String> packageNames) {
        this.packageNames = packageNames;
    }

    public Set<String> getPackageNames() {
        return this.packageNames;
    }

    public <T extends Pojo> List<T> merge(List<T> currentPojos, List<T> newPojos) {
        List<T> merged = Stream.concat(currentPojos.stream().filter(this::canKeep), newPojos.stream()).collect(Collectors.toList());
        Log.d(TAG, "Merged " + newPojos.size() + "pojos into " + currentPojos.size() + " pojos, resulting in " + merged.size() + " pojos.");
        return merged;
    }

    public <T> List<T> execute(Function<String, List<T>> fn) {
        return packageNames.stream()
                .map(fn)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public boolean contains(ReloadFilter filter) {
        if (!(filter instanceof PackageNameFilter)) {
            return false;
        }
        PackageNameFilter packageNameFilter = (PackageNameFilter) filter;
        return this.packageNames.containsAll(packageNameFilter.packageNames);
    }

    private <T extends Pojo> boolean canKeep(T pojo) {
        if (pojo instanceof PojoWithPackageName) {
            return !packageNames.contains(((PojoWithPackageName) pojo).getPackageName());
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PackageNameFilter)) return false;
        PackageNameFilter that = (PackageNameFilter) o;
        return Objects.equals(packageNames, that.packageNames);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(packageNames);
    }

    @Override
    public String toString() {
        return "PackageNameFilter{" +
                "packageNames=" + packageNames +
                '}';
    }
}
