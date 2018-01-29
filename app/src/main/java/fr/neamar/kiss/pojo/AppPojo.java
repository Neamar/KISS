package fr.neamar.kiss.pojo;

import fr.neamar.kiss.utils.UserHandle;

public class AppPojo extends PojoWithTags {
    public String packageName;
    public String activityName;
    public int badgeCount;
    public UserHandle userHandle;

    public String displayBadge = "";

    public void setBadgeCount(int badgeCount){
        this.badgeCount = badgeCount;
        if (badgeCount > 99){
            displayBadge = "99";
        }else{
            displayBadge = String.valueOf(badgeCount);
        }
    }
}
