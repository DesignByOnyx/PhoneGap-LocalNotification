package com.phonegap.plugins.localnotification;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This plugin utilizes the Android AlarmManager in combination with StatusBar
 * notifications. When a local notification is scheduled the alarm manager takes
 * care of firing the event. When the event is processed, a notification is put
 * in the Android status bar.
 * 
 * @author Daniel van 't Oever (original author)
 * 
 * @author Wang Zhuochun(https://github.com/zhuochun)
 */
public class LocalNotification extends CordovaPlugin {

	public static final String PLUGIN_NAME = "PhonegapLocalNotification";
	public static final String KEY_STARTUP_CLASS = "PhonegapLocalNotification_StartupClass";
	//public static final String PLUGIN_PREFIX = "LocalNotification_";

	/**
	 * Delegate object that does the actual alarm registration. Is reused by the
	 * AlarmRestoreOnBoot class.
	 */
	private AlarmHelper alarm = null;
	private Context appContext = null;
	private static LocalNotification instance;
	
	public static LocalNotification getInstance() {
	    return instance;
	}
	
	public LocalNotification() {
	    instance = this;
	}

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		appContext = this.cordova.getActivity().getApplicationContext();
		boolean success = false;
		String message = "There was an error.";

		final CordovaInterface cordova = this.cordova;

		alarm = new AlarmHelper(cordova.getActivity().getBaseContext());
		
		Log.i(PLUGIN_NAME, "Plugin execute called with action: " + action);
		
		if (action.equalsIgnoreCase("addNotification")) {
			success = this.addNotification(args);
			message = success ? args.getString(0) : "Error adding notification.";
			
		} else if (action.equalsIgnoreCase("cancelNotification")) {
			success = this.cancelNotification(args.getString(0));
			
		} else if (action.equalsIgnoreCase("cancelAllNotifications")) {
			success = this.cancelAllNotifications();
			
		}

		if (success) {
			callbackContext.success(message);
		} else {
			callbackContext.error(message);
		}

		return success;
	}

	/**
	 * Set an alarm
	 */
	//success = this.add(notificationId, fireDate, title, body, repeatInterval, callbackData);
	public boolean addNotification(JSONArray args) throws JSONException {
		String notificationId = args.getString(0);
		long fireDate = args.getLong(1);
		//String title = args.getString(2);
		String alertBody = args.getString(2);
		String repeatInterval = args.getString(3);
		String callbackData = args.getString(4);
		
		// userInfo is an ios convention, just trying to be consistent
		JSONObject userInfo = new JSONObject();
		userInfo.put("notificationId", notificationId);
		userInfo.put("fireDate", fireDate);
		userInfo.put("alertBody", alertBody);
		userInfo.put("repeatInterval", repeatInterval);
		userInfo.put("callbackData", callbackData);
		
		Map<String, Long> repeatDict = new HashMap<String, Long>();
		repeatDict.put("hourly"		, 3600000L); // 1000 x 60 x 60
		repeatDict.put("daily"		, 43200000L); // x 24
		repeatDict.put("weekly"		, 302400000L); // x 7
		repeatDict.put("monthly"	, 1313999712L); // daily x 30.41666
		repeatDict.put("quarterly"	, 3942000000L); // daily x 91.25
		repeatDict.put("yearly"		, 15768000000L); // daily x 365
		
		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		//Log.i(PLUGIN_NAME, " ---- checkpoint 1");
		
		date.setTime(fireDate);
		calendar.setTime(date);

		//Log.i(PLUGIN_NAME, " ---- checkpoint 2");
		
		long repeatMillis = 0;
		if(repeatDict.containsKey(repeatInterval)) {
			repeatMillis = repeatDict.get(repeatInterval);
		}
		//Log.i(PLUGIN_NAME, " ---- checkpoint 3");
		
		boolean result = alarm.addAlarm(
				notificationId, 
				calendar, 
				null, 
				alertBody, 
				repeatMillis,
				userInfo
			);
		
		//Log.i(PLUGIN_NAME, " ---- checkpoint 4: " + result);

		this.persistAlarm(notificationId, args);
		return result;
	}
	
	public void didReceiveLocalNotification(String userInfo) {
		webView.loadUrl("javascript:(function() {" +
				"var fn = function() {" +
					"LocalNotification.receiveNotification(" + userInfo + "); " +
					"document.removeEventListener('deviceready', fn)" +
				"};" +
				"document.addEventListener('deviceready', fn);" +
			"}());");
	}

	/**
	 * Cancel a specific notification that was previously registered.
	 * 
	 * @param notificationId
	 *            The original ID of the notification that was used when it was
	 *            registered using addNotification()
	 */
	public boolean cancelNotification(String id) {
		Log.d(PLUGIN_NAME, "cancel Notification with id: " + id);
		this.unpersistAlarm(id);
		return alarm.cancelAlarm(id);
	}

	/**
	 * Cancel all notifications that were created by this plugin.
	 */
	public boolean cancelAllNotifications() {
		Log.d(PLUGIN_NAME,
				"cancelAllNotifications: cancelling all events for this application");
		/*
		 * Android can only unregister a specific alarm. There is no such thing
		 * as cancelAll. Therefore we rely on the Shared Preferences which holds
		 * all our alarms to loop through these alarms and unregister them one
		 * by one.
		 */
		final CordovaInterface cordova = this.cordova;
		
		final SharedPreferences alarmSettings = cordova.getActivity().getBaseContext().getSharedPreferences(
				PLUGIN_NAME, Context.MODE_PRIVATE);
		
		this.unpersistAlarmAll();
		return alarm.cancelAll(alarmSettings);
	}
	
	// Returns the Startup Class Name saved when a notification was created
	public static String getSavedStartupClassName(Context context) {
		SharedPreferences sharedPrefs = getSharedPreferences(context);
		return sharedPrefs.getString(KEY_STARTUP_CLASS, null);
	}
	
	// Returns the Startup Class Name for a particular context
	public static String getContextStartupClassName(Context context) {
		PackageManager manager = context.getPackageManager();
    	String packageClassName = manager.getLaunchIntentForPackage(context.getPackageName()).getComponent().getClassName();
		
		return packageClassName;
	}

	/**
	 * Persist the information of this alarm to the Android Shared Preferences.
	 * This will allow the application to restore the alarm upon device reboot.
	 * Also this is used by the cancelAllNotifications method.
	 * 
	 * @see #cancelAllNotifications()
	 * 
	 * @param optionsArr
	 *            The assumption is that parseOptions has been called already.
	 * 
	 * @return true when successfull, otherwise false
	 */
	private boolean persistAlarm(String id, JSONArray args) {
		final Editor alarmSettingsEditor = getSharedPreferences(appContext).edit();
		
		alarmSettingsEditor.putString(KEY_STARTUP_CLASS, getContextStartupClassName(appContext));
		alarmSettingsEditor.putString(id, args.toString());

		return alarmSettingsEditor.commit();
	}

	/**
	 * Remove a specific alarm from the Android shared Preferences
	 * 
	 * @param alarmId
	 *            The Id of the notification that must be removed.
	 * 
	 * @return true when successfull, otherwise false
	 */
	private boolean unpersistAlarm(String id) {
		final Editor alarmSettingsEditor = getSharedPreferences(appContext).edit();

		alarmSettingsEditor.remove(id);

		return alarmSettingsEditor.commit();
	}

	/**
	 * Clear all alarms from the Android shared Preferences
	 * 
	 * @return true when successfull, otherwise false
	 */
	private boolean unpersistAlarmAll() {
		final Editor alarmSettingsEditor = getSharedPreferences(appContext).edit();

		alarmSettingsEditor.clear();

		return alarmSettingsEditor.commit();
	}
	
	private static SharedPreferences getSharedPreferences(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}
}
