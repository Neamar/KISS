package fr.neamar.kiss.pojo;

import fr.neamar.kiss.utils.UserHandle;

public class AppPojo extends PojoWithTags {
    public final String packageName;
    public final String activityName;
    public final UserHandle userHandle;

    public AppPojo(String packageName, String activityName, UserHandle userHandle) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.userHandle = userHandle;
    }

    public String getComponentName() {
        return userHandle.addUserSuffixToString(packageName + "/" + activityName, '#');
    }
}
