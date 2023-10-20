package fr.neamar.kiss.searcher;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoWithTags;

public class UntaggedSearcher extends Searcher {

    public UntaggedSearcher(MainActivity activity )
    {
        super( activity, "<untagged>" );
    }

    @Override
    public boolean addResult(Pojo... pojos) {
        for (Pojo pojo : pojos) {
            if (!(pojo instanceof PojoWithTags)) {
                continue;
            }
            PojoWithTags pojoWithTags = (PojoWithTags) pojo;
            if (pojoWithTags.getTags() != null && !pojoWithTags.getTags().isEmpty()) {
                continue;
            }

            super.addResult(pojo);
        }
        return false;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return null;

        KissApplication.getApplication(activity).getDataHandler().requestAllRecords(this);

        return null;
    }

}
