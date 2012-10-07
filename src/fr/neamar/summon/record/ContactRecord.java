package fr.neamar.summon.record;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import fr.neamar.summon.R;
import fr.neamar.summon.holder.ContactHolder;

public class ContactRecord extends Record {
	public ContactHolder contactHolder;

	public ContactRecord(ContactHolder contactHolder) {
		super();
		this.holder = this.contactHolder = contactHolder;
	}

	@Override
	public View display(Context context, View v) {
		if (v == null)
			v = inflateFromId(context, R.layout.item_contact);

		// Contact name
		TextView contactName = (TextView) v
				.findViewById(R.id.item_contact_name);
		contactName.setText(enrichText(contactHolder.displayName));

		// Contact phone
		TextView contactPhone = (TextView) v
				.findViewById(R.id.item_contact_phone);
		contactPhone.setText(contactHolder.phone);

		// Contact photo
		ImageView appIcon = (ImageView) v.findViewById(R.id.item_contact_icon);
		if (contactHolder.icon != null)
			appIcon.setImageURI(contactHolder.icon);
		else
			appIcon.setImageDrawable(context.getResources().getDrawable(
					R.drawable.ic_contact));

		// Phone action
		Button phoneButton = (Button) v
				.findViewById(R.id.item_contact_action_phone);
		phoneButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// We need to manually call this function to ensure the system
				// store this contact in history
				recordLaunch(v.getContext());
				String url = "tel:" + contactHolder.phone;
				Intent i = new Intent(Intent.ACTION_CALL, Uri.parse(url));
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				v.getContext().startActivity(i);
			}
		});

		// Message action
		Button messageButton = (Button) v
				.findViewById(R.id.item_contact_action_message);
		messageButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// We need to manually call this function to ensure the system
				// store this contact in history
				recordLaunch(v.getContext());

				String url = "sms:" + contactHolder.phone;
				Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				v.getContext().startActivity(i);
			}
		});

		return v;
	}

	@Override
	public void doLaunch(Context context) {
		Intent viewContact = new Intent(Intent.ACTION_VIEW);

		viewContact.setData(Uri.withAppendedPath(
				ContactsContract.Contacts.CONTENT_LOOKUP_URI,
				String.valueOf(contactHolder.lookupKey)));
		viewContact.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		viewContact.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);// TODO: check
																	// if
																	// working
		context.startActivity(viewContact);

	}

}
