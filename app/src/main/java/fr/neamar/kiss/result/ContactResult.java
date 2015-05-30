package fr.neamar.kiss.result;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.FileNotFoundException;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.QueryInterface;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.ContactPojo;
import fr.neamar.kiss.ui.ImprovedQuickContactBadge;

public class ContactResult extends Result {
	public ContactPojo contactPojo;
	private QueryInterface queryInterface;

	public ContactResult(QueryInterface queryInterface, ContactPojo contactPojo) {
		super();
		this.pojo = this.contactPojo = contactPojo;
		this.queryInterface = queryInterface;

		// Try to pretty format phone number
		if (this.contactPojo.phone.matches("(\\+3)?[0-9]{10}")) {
			// Mise en forme du numéro de téléphone
			String formatted_phone = contactPojo.phone.replace(" ", "");
			int number_length = contactPojo.phone.length();
			for (int i = 1; i < 5; i++) {
				formatted_phone = formatted_phone.substring(0, number_length - 2 * i) + " "
						+ formatted_phone.substring(number_length - 2 * i);
			}

			contactPojo.phone = formatted_phone;
		}
	}

	@Override
	public View display(Context context, View v) {
		if (v == null)
			v = inflateFromId(context, R.layout.item_contact);

		// Contact name
		TextView contactName = (TextView) v.findViewById(R.id.item_contact_name);
		contactName.setText(enrichText(contactPojo.displayName));

		// Contact phone
		TextView contactPhone = (TextView) v.findViewById(R.id.item_contact_phone);
		contactPhone.setText(contactPojo.phone);

		// Contact photo
		ImprovedQuickContactBadge contactIcon = (ImprovedQuickContactBadge) v
				.findViewById(R.id.item_contact_icon);
		contactIcon.setImageDrawable(getDrawable(context));
			
		contactIcon.assignContactUri(Uri.withAppendedPath(
				ContactsContract.Contacts.CONTENT_LOOKUP_URI,
				String.valueOf(contactPojo.lookupKey)));
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

		if (contactPojo.homeNumber)
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
		if (contactPojo.icon != null) {
			try {
				return Drawable.createFromStream(
						context.getContentResolver().openInputStream(contactPojo.icon), null);
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
				String.valueOf(contactPojo.lookupKey)));
		viewContact.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		viewContact.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		context.startActivity(viewContact);
	}
	
	@Override
	public void fastLaunch(Context context)
	{
		launchMessaging(context);
	}
	
	protected void launchMessaging(final Context context)
	{
		String url = "sms:" + contactPojo.phone;
		Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);

		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				recordLaunch(context);
				queryInterface.launchOccured();
			}
		}, KissApplication.TOUCH_DELAY);

	}
	
	protected void launchCall(final Context context)
	{
		String url = "tel:" + contactPojo.phone;
		Intent i = new Intent(Intent.ACTION_CALL, Uri.parse(url));
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);

		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				recordLaunch(context);
				queryInterface.launchOccured();
			}
		}, KissApplication.TOUCH_DELAY);

	}
}
