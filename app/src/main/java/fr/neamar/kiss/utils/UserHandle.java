package fr.neamar.kiss.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Process;


/**
 * Wrapper class for `android.os.UserHandle` that works with all Android versions
 */
public class UserHandle {
	private Object handle; // android.os.UserHandle on Android 4.2 and newer
	
	public UserHandle() {
		this(null);
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public UserHandle(Object user) {
		if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			// OS does not provide any APIs for multi-user support
			this.handle = null;
		} else if(user != null && Process.myUserHandle().equals(user)) {
			// For easier processing the current user is also stored as `null`, even
			// if there is multi-user support
			this.handle = null;
		} else {
			// Store the given user handle
			assert (user instanceof android.os.UserHandle);
			this.handle = user;
		}
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public android.os.UserHandle getRealHandle() {
		if(this.handle != null) {
			return (android.os.UserHandle) this.handle;
		} else {
			return Process.myUserHandle();
		}
	}
}
