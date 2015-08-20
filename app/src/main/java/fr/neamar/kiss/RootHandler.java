package fr.neamar.kiss;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class RootHandler {
	
	 private Boolean isRootAvailable = null;
	 private Boolean isRootActivated = null;
	 
	 public RootHandler(Context ctx) {
		 SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		 isRootActivated = prefs.getBoolean("root-mode", false);
	 }
		
     public boolean isRootAvailable(){
    	 if (!isRootActivated) 
    		 return false;
    	 
         if (isRootAvailable == null) {
        	 try {
        		 isRootAvailable = simpleExecuteCommand(null);
        	 } catch (Exception e) {
        		 isRootAvailable = false;
        	 }
         }
         
         return isRootAvailable;
     }
     
     public boolean hibernateApp(String packageName) {
    	 try {
    		 return simpleExecuteCommand("am force-stop "+packageName);
    	 } catch (Exception e) {
    		 return false;
    	 }
     }
     
     private boolean simpleExecuteCommand(String command){
         Process p = null;
         try{
            p = Runtime.getRuntime().exec("su");
            //put command 
            if (command != null && !command.trim().equals("")) {
            	p.getOutputStream().write((command + "\n").getBytes());
            	p.getOutputStream().flush();
            }
            //exit from su command
            p.getOutputStream().write(("exit\n").getBytes());
            p.getOutputStream().flush();
            
            int result = p.waitFor();
            if(result != 0)
                throw new Exception("Command execution failed " + result);
            return true;
         } catch (Exception e) {
        	 Log.e("simpleExecuteCommand", " " + e);
         }finally {
             if(p != null)
                 p.destroy();
         }
         return false;
     }    

}
