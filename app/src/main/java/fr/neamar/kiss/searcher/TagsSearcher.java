package fr.neamar.kiss.searcher;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoWithTags;

/**
 * Returns a list of all applications that match the tags toggled
 */

public class TagsSearcher extends Searcher
{
	private final boolean showOneTag;

	public TagsSearcher( MainActivity activity, String query )
	{
		super( activity, query == null ? "<tags>" : query );
		showOneTag = query != null;
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

			// split tags string so we can search faster
			TreeSet<String> tagList = new TreeSet<>();
			Collections.addAll( tagList, patternTagSplit.split( pojoWithTags.getTags() ) );

			if (showOneTag)
			{
				if ( !tagList.contains(this.query) )
					iterator.remove();
				continue;
			}

			if ( !activity.getExcludeTags().isEmpty() ) {
				// remove pojos that contain tags that should be hidden
				boolean remove = false;
				for (String tag : tagList) {
					if (activity.getExcludeTags().contains(tag)) {
						remove = true;
						break;
					}
				}
				if (remove) {
					iterator.remove();
					continue;
				}
			}

			if ( !activity.getIncludeTags().isEmpty() )
			{
				// remove pojos if they don't have the include tags
				boolean bIncludeTagFound = false;
				for( String tag : activity.getIncludeTags() )
				{
					if( tagList.contains( tag ) )
					{
						bIncludeTagFound = true;
						break;
					}
				}
				if ( !bIncludeTagFound )
				{
					iterator.remove();
				}
			}
		}

		this.addResult(results.toArray(new Pojo[0]));
		return null;
	}
}
