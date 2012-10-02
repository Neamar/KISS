package fr.neamar.summon.dataprovider;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import fr.neamar.summon.holder.AppHolder;
import fr.neamar.summon.record.AppRecord;
import fr.neamar.summon.record.Record;

public class AppProvider extends Provider{
	private ArrayList<AppHolder> apps = new ArrayList<AppHolder>();
	
	public AppProvider(Context context) {
		super(context);
		
		List<ApplicationInfo> packs = context.getPackageManager().getInstalledApplications(0);
		for(int i=0; i<packs.size(); i++)
		{
			ApplicationInfo p = packs.get(i);

			AppHolder newInfo = new AppHolder();
			newInfo.appName = p.loadLabel(context.getPackageManager()).toString();
			newInfo.packageName = p.packageName;
			newInfo.icon = p.loadIcon(context.getPackageManager());
			apps.add(newInfo);
		}
	}

	public ArrayList<Record> getRecords(String query)
	{
		ArrayList<Record> records = new ArrayList<Record>();
		
		for(int i = 0; i < apps.size(); i++)
		{
			if(apps.get(i).appName.startsWith(query))
			{
				Record r = new AppRecord(apps.get(i));
				records.add(r);
			}
		}

		return records;
	}
}
