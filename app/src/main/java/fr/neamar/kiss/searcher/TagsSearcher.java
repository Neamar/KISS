package fr.neamar.kiss.searcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
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
	public TagsSearcher( MainActivity activity )
	{
		super( activity, "<tags>" );
	}

	@Override
	protected Void doInBackground(Void... voids )
	{
		MainActivity activity = activityWeakReference.get();
		if ( activity == null )
			return null;
		List<Pojo> results = KissApplication.getDataHandler(activity).getApplications();

		Pattern patternTagSplit = Pattern.compile("\\s+");
		for( Iterator<Pojo> iterator = results.iterator(); iterator.hasNext(); )
		{
			Pojo pojo = iterator.next();
			if ( !(pojo instanceof PojoWithTags))
				continue;
			PojoWithTags pojoWithTags = (PojoWithTags) pojo;
			if ( pojoWithTags.getTags() != null && !pojoWithTags.getTags().isEmpty() )
			{
				// split tags string so we can search faster
				TreeSet<String> tagList = new TreeSet<>();
				Collections.addAll( tagList, patternTagSplit.split( pojoWithTags.getTags() ) );

				// remove pojos that contain tags that should be hidden
				boolean remove = false;
				for( String tag : tagList )
				{
					if( activity.getExcludeTags()
								.contains( tag ) )
					{
						remove = true;
						break;
					}
				}
				if ( remove )
				{
					iterator.remove();
				}
				else if ( !activity.getIncludeTags().isEmpty() )
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
			else if ( !activity.getIncludeTags().isEmpty() )
			{
				// if we have "must have" tags but the app has no tags, remove it
				iterator.remove();
			}
		}

		this.addResult(results.toArray(new Pojo[0]));
		return null;
	}
}
