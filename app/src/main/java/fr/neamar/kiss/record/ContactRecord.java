package fr.neamar.kiss.record;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.FileNotFoundException;

import fr.neamar.kiss.QueryInterface;
import fr.neamar.kiss.R;
import fr.neamar.kiss.holder.ContactHolder;
import fr.neamar.kiss.ui.ImprovedQuickContactBadge;

public class ContactRecord extends Record {
	public ContactHolder contactHolder;
	private QueryInterface queryInterface;

	public ContactRecord(QueryInterface queryInterface, ContactHolder contactHolder) {
		super();
		this.holder = this.contactHolder = contactHolder;
		this.queryInterface = queryInterface;

		// Try to pretty format phone number
		if (this.contactHolder.phone.matches("(\\+3)?[0-9]{10}")) {
			// Mise en forme du numéro de téléphone
			String formatted_phone = contactHolder.phone.replace(" ", "");
			int number_length = contactHolder.phone.length();
			for (int i = 1; i < 5; i++) {
				formatted_phone = formatted_phone.substring(0, number_length - 2 * i) + " "
						+ formatted_phone.substring(number_length - 2 * i);
			}

			contactHolder.phone = formatted_phone;
		}
	}

	@Override
	public View display(Context context, View v) {
		if (v == null)
			v = inflateFromId(context, R.layout.item_contact);

		// Contact name
		TextView contactName = (TextView) v.findViewById(R.id.item_contact_name);
		contactName.setText(enrichText(contactHolder.displayName));

		// Contact phone
		TextView contactPhone = (TextView) v.findViewById(R.id.item_contact_phone);
		contactPhone.setText(contactHolder.phone);

		// Contact photo
		ImprovedQuickContactBadge contactIcon = (ImprovedQuickContactBadge) v
				.findViewById(R.id.item_contact_icon);
		contactIcon.setImageDrawable(getDrawable(context));
			
		contactIcon.assignContactUri(Uri.withAppendedPath(
				ContactsContract.Contacts.CONTENT_LOOKUP_URI,
				String.valueOf(contactHolder.lookupKey)));
		contactIcon.setExtraOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				recordLaunch(v.getContext());
				queryInterface.launchOccured();
			}
		});

		// Phone action
		ImageButton phoneButton = (ImageButton) v.findViewById(R.id.item_contact_action_phone);
		// Message action
		ImageButton messageButton = (ImageButton) v.findViewById(R.id.item_contact_action_message);

		if (contactHolder.homeNumber)
			messageButton.setVisibility(View.INVISIBLE);
		else
			messageButton.setVisibility(View.VISIBLE);

		PackageManager pm = context.getPackageManager();

		if (pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
			phoneButton.setVisibility(View.VISIBLE);
			messageButton.setVisibility(View.VISIBLE);
			phoneButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					launchCall(v.getContext());
				}
			});

			messageButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					launchMessaging(v.getContext());
				}
			});
		} else {
			phoneButton.setVisibility(View.INVISIBLE);
			messageButton.setVisibility(View.INVISIBLE);
		}

		return v;
	}

	@Override
	public Drawable getDrawable(Context context) {
		if (contactHolder.icon != null) {
			try {
				return Drawable.createFromStream(
						context.getContentResolver().openInputStream(contactHolder.icon), null);
			} catch (FileNotFoundException e) {
			}
		}

		// Default icon
		return context.getResources().getDrawable(R.drawable.ic_contact);
	}

	@Override
	public void doLaunch(Context context, View v) {
		Intent viewContact = new Intent(Intent.ACTION_VIEW);

		viewContact.setData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI,
				String.valueOf(contactHolder.lookupKey)));
		viewContact.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		viewContact.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		context.startActivity(viewContact);
	}
	
	@Override
	public void fastLaunch(Context context)
	{
		launchMessaging(context);
	}
	
	protected void launchMessaging(Context context)
	{
		recordLaunch(context);
		queryInterface.launchOccured();
		String url = "sms:" + contactHolder.phone;
		Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}
	
	protected void launchCall(Context context)
	{
		recordLaunch(context);
		queryInterface.launchOccured();
		String url = "tel:" + contactHolder.phone;
		Intent i = new Intent(Intent.ACTION_CALL, Uri.parse(url));
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}
}
