package fr.neamar.summon.record;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import fr.neamar.summon.lite.R;
import fr.neamar.summon.holder.ContactHolder;
import fr.neamar.summon.ui.ImprovedQuickContactBadge;

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
		ImprovedQuickContactBadge contactIcon = (ImprovedQuickContactBadge) v
				.findViewById(R.id.item_contact_icon);
		if (contactHolder.icon != null)
			contactIcon.setImageURI(contactHolder.icon);
		else
			contactIcon.setImageDrawable(context.getResources().getDrawable(
					R.drawable.ic_contact));
		contactIcon.assignContactUri(Uri.withAppendedPath(
				ContactsContract.Contacts.CONTENT_LOOKUP_URI,
				String.valueOf(contactHolder.lookupKey)));
		contactIcon.setExtraOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				recordLaunch(v.getContext());
			}
		});

		// Phone action
		ImageButton phoneButton = (ImageButton) v
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
		ImageButton messageButton = (ImageButton) v
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
	public void doLaunch(Context context, View v) {
		Intent viewContact = new Intent(Intent.ACTION_VIEW);

		viewContact.setData(Uri.withAppendedPath(
				ContactsContract.Contacts.CONTENT_LOOKUP_URI,
				String.valueOf(contactHolder.lookupKey)));
		viewContact.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		viewContact.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		context.startActivity(viewContact);
	}

}
