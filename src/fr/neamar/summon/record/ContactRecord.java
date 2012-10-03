package fr.neamar.summon.record;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import fr.neamar.summon.R;
import fr.neamar.summon.holder.ContactHolder;

public class ContactRecord extends Record {
	public ContactHolder contactHolder;
	
	public ContactRecord(ContactHolder contactHolder) {
		super();
		this.contactHolder = contactHolder;
	}

	@Override
	public View display(Context context) {
		View v = inflateFromId(context, R.layout.item_contact);
		
		//Contact name
		TextView contactName = (TextView) v.findViewById(R.id.item_contact_name);
		contactName.setText(enrichText(contactHolder.displayContactName));
		
		//Contact phone
		TextView contactPhone = (TextView) v.findViewById(R.id.item_contact_phone);
		contactPhone.setText(contactHolder.contactPhone);
		
		//Contact photo
		if(contactHolder.icon != null)
		{
			ImageView appIcon = (ImageView) v.findViewById(R.id.item_contact_icon);
			appIcon.setImageURI(contactHolder.icon);
		}
		
		return v;
	}

	@Override
	public void launch(Context context) {
		//context.startActivity(contactHolder.intent);
	}

}
