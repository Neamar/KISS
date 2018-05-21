package fr.neamar.kiss.searcher;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoWithTags;

/**
 * Returns a list of all applications that match the tags toggled
 */

public class TagsSearcher extends Searcher
{
	private static final Pattern patternTagSplit = Pattern.compile("\\s+");

	public TagsSearcher( MainActivity activity, String query )
	{
		super( activity, query == null ? "<tags>" : query );
	}

	@Override
	protected Void doInBackground(Void... voids )
	{
		MainActivity activity = activityWeakReference.get();
		if ( activity == null )
			return null;
		List<Pojo> results = KissApplication.getApplication(activity).getDataHandler().getApplications();

		for( Iterator<Pojo> iterator = results.iterator(); iterator.hasNext(); )
		{
			Pojo pojo = iterator.next();
			if ( !(pojo instanceof PojoWithTags)) {
				iterator.remove();
				continue;
			}
			PojoWithTags pojoWithTags = (PojoWithTags) pojo;
			if ( pojoWithTags.getTags() == null || pojoWithTags.getTags().isEmpty() ) {
				iterator.remove();
				continue;
			}

            if (!pojoWithTags.getTags().contains(query))
                iterator.remove();

//			TreeSet<String> tagList = new TreeSet<>();
//			Collections.addAll( tagList, patternTagSplit.split( pojoWithTags.getTags() ) );
//
//            if (!tagList.contains(this.query))
//					iterator.remove();
		}

		this.addResult(results.toArray(new Pojo[0]));
		return null;
	}
}
