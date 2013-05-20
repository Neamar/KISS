package fr.neamar.summon.dataprovider;

import android.content.Context;
import android.util.Pair;

import java.util.ArrayList;
import java.util.regex.Pattern;

import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.task.LoadAliasHolders;
import fr.neamar.summon.task.LoadAliasHoldersFromDB;

public class AliasProvider extends Provider<Pair<String, String>> {
	private ArrayList<Provider> providers;

	public AliasProvider(final Context context, ArrayList<Provider> providers) {
		super(new LoadAliasHolders(context));
		this.providers = providers;
	}

    private AliasProvider(LoadAliasHoldersFromDB loader, ArrayList<Provider> providers) {
        super(loader);
        this.providers = providers;
    }

    public static AliasProvider fromDB(Context context, ArrayList<Provider> providers) {
        return new AliasProvider(new LoadAliasHoldersFromDB(context), providers);
    }

	public ArrayList<Holder> getResults(String query) {
		ArrayList<Holder> results = new ArrayList<Holder>();

		for (Pair<String, String> entry : holders) {
			if (entry.first.startsWith(query)) {
				for (int i = 0; i < providers.size(); i++) {
					if (providers.get(i).mayFindById(entry.second)) {
						Holder holder = providers.get(i).findById(entry.second);

						if (holder != null) {
							holder.displayName = holder.name
									+ " <small>("
									+ entry.first.replaceFirst(
											"(?i)(" + Pattern.quote(query) + ")", "{$1}")
									+ ")</small>";
							holder.relevance = 10;
							results.add(holder);
						}
					}
				}
			}
		}

		return results;
	}

    @Override
    public void saveProvider(Context context) {
        //Nothing to save here
    }
}
