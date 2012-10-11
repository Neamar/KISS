package fr.neamar.summon.dataprovider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import fr.neamar.summon.holder.Holder;

public class AliasProvider extends Provider {
	private HashMap<String, String> alias = new HashMap<String, String>();
	private ArrayList<Provider> providers;

	public AliasProvider(Context context, ArrayList<Provider> providers) {
		super(context);

		this.providers = providers;
		Thread thread = new Thread(null, initSettingsList);
		thread.setPriority(Thread.NORM_PRIORITY + 1);
		thread.start();
	}

	protected Runnable initSettingsList = new Runnable() {
		public void run() {
			final PackageManager pm = context.getPackageManager();

			String contactApp = getAppByCategory(pm,
					Intent.CATEGORY_APP_CONTACTS);
			alias.put("contacts", contactApp);

			String phoneApp = getApp(pm, Intent.ACTION_DIAL);
			alias.put("dial", phoneApp);
			alias.put("compose", phoneApp);

			String browserApp = getAppByCategory(pm,
					Intent.CATEGORY_APP_BROWSER);
			alias.put("internet", browserApp);
			alias.put("web", browserApp);

			String mailApp = getAppByCategory(pm, Intent.CATEGORY_APP_EMAIL);
			alias.put("email", mailApp);
			alias.put("mail", mailApp);

			String marketApp = getAppByCategory(pm, Intent.CATEGORY_APP_MARKET);
			alias.put("market", marketApp);

			String messagingApp = getAppByCategory(pm,
					Intent.CATEGORY_APP_MESSAGING);
			alias.put("text", messagingApp);
			alias.put("sms", messagingApp);

		}

		private String getApp(PackageManager pm, String action) {
			Intent lookingFor = new Intent(action, null);
			return getApp(pm, lookingFor);
		}

		private String getAppByCategory(PackageManager pm, String category) {
			Intent lookingFor = new Intent(Intent.ACTION_MAIN, null);
			lookingFor.addCategory(category);
			return getApp(pm, lookingFor);
		}

		private String getApp(PackageManager pm, Intent lookingFor) {
			List<ResolveInfo> list = pm.queryIntentActivities(lookingFor, 0);
			if (list.size() == 0)
				return "(none)";
			else
				return "app://"
						+ list.get(0).activityInfo.applicationInfo.packageName
						+ "/" + list.get(0).activityInfo.name;

		}
	};

	public ArrayList<Holder> getResults(String query) {
		ArrayList<Holder> holders = new ArrayList<Holder>();

		for (Entry<String, String> entry : alias.entrySet()) {
			if (entry.getKey().startsWith(query)) {
				for (int i = 0; i < providers.size(); i++) {
					if(providers.get(i).mayFindById(entry.getValue()))
					{
						Holder holder = providers.get(i).findById(entry.getValue());
	
						if (holder != null) {
							holder.displayName = holder.name
									+ " <small>("
									+ entry.getKey().replaceFirst(
											"(?i)(" + Pattern.quote(query) + ")",
											"{$1}") + ")</small>";
							holder.relevance = 10;
							holders.add(holder);
						}
					}
				}
			}
		}

		return holders;
	}
}
