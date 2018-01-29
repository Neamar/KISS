package fr.neamar.kiss.pojo;

import java.util.List;

import fr.neamar.kiss.normalizer.StringNormalizer;

/**
 * Created by TBog on 1/25/2018.
 */

public class PojoWithTags extends Pojo
{
	// tags normalized, for faster search
	public StringNormalizer.Result normalizedTags = null;
	// Tags assigned to this pojo
	protected String tags;
	// Variable to store the formatted (user selection in bold) tag
	public String displayTags = "";

	public String getTags()
	{
		return tags;
	}

	public void setTags(String tags) {
		if (tags != null) {
			// Set the actual user-friendly name
			this.tags = tags.replaceAll("<", "&lt;");

			this.normalizedTags = StringNormalizer.normalizeWithResult( this.tags, false );
		}
		else
		{
			this.tags = null;
			this.normalizedTags = null;
		}
	}

	public void setTagHighlight(int positionStart, int positionEnd) {
		int posStart = this.mapTagsPosition(positionStart);
		int posEnd = this.mapTagsPosition(positionEnd);

		this.displayTags = this.tags.substring(0, posStart)
						   + '{' + this.tags.substring(posStart, posEnd) + '}' + this.tags.substring(posEnd, this.tags.length());
	}

	public void setTagHighlight( List<Integer> matchPositions )
	{
		int startPos = matchPositions.get( 0 );
		int endPos = startPos + 1;
		StringBuilder sb = new StringBuilder( this.tags.length() + matchPositions.size() * 2 );
		int lastInsert = 0;
		for ( int i = 1; i < matchPositions.size(); i += 1 )
		{
			if ( (endPos == matchPositions.get( i )) )
			{
				endPos+= 1;
			}
			else
			{
				int mappedStartPos = mapTagsPosition( startPos );
				int mappedEndPos = mapTagsPosition( endPos );
				sb.append( this.tags.substring( lastInsert, mappedStartPos ) )
				  .append( "{" )
				  .append( this.tags.substring( mappedStartPos, mappedEndPos ) )
				  .append( "}" );
				lastInsert = mappedEndPos;
				startPos = matchPositions.get( i );
				endPos = startPos + 1;
			}
		}
		int mappedStartPos = mapTagsPosition( startPos );
		int mappedEndPos = mapTagsPosition( endPos );
		sb.append( this.tags.substring( lastInsert, mappedStartPos ) )
		  .append( "{" )
		  .append( this.tags.substring( mappedStartPos, mappedEndPos ) )
		  .append( "}" );
		lastInsert = mappedEndPos;
		sb.append( this.tags.substring( lastInsert ) );
		this.displayTags = sb.toString();
	}

	/**
	 * Map a position in the normalized tags string to a position in the standard tags string
	 *
	 * @param position Position in normalized tags string
	 * @return Position in standard tags string
	 */
	public int mapTagsPosition(int position) {
		if ( position < normalizedTags.mapPosition.length )
			return normalizedTags.mapPosition[position];
		return tags.length();
	}

}
