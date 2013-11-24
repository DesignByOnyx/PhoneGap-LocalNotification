package org.apache.cordova.localnotification;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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

	public static final String PLUGIN_NAME = "LocalNotification";
	public static final String PLUGIN_PREFIX = "LocalNotification_";

	/**
	 * Delegate object that does the actual alarm registration. Is reused by the
	 * AlarmRestoreOnBoot class.
	 */
	private AlarmHelper alarm = null;

	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {
		boolean success = false;

		final CordovaInterface cordova = this.cordova;

		alarm = new AlarmHelper(cordova.getActivity().getBaseContext());
		
		Log.d(PLUGIN_NAME, "Plugin execute called with action: " + action);
		
		if (action.equalsIgnoreCase("addNotification")) {
			persistAlarm(args.getInt(0), args);
			
			Log.d(PLUGIN_NAME, "Add Notification with Id: " + args.getInt(0));
			
			String notificationId = args.getString(0);
			long fireDate = args.getLong(1);
			String title = args.getString(2);
			String body = args.getString(3);
			String repeatInterval = args.getString(4);
			String callbackData = args.getString(5);
			
			success = this.add(notificationId, fireDate, title, body, repeatInterval, callbackData);
		} else if (action.equalsIgnoreCase("cancelNotification")) {
			unpersistAlarm(args.getInt(0));
			
			Log.d(PLUGIN_NAME, "Cancel Notification with Id: " + args.getInt(0));

			success = this.cancelNotification(args.getInt(0));
		} else if (action.equalsIgnoreCase("cancelAllNotifications")) {
			unpersistAlarmAll();

			success = this.cancelAllNotifications();
		}

		if (success) {
			callbackContext.success();
		}

		return success;
	}

	/**
	 * Set an alarm
	 */
	//success = this.add(notificationId, fireDate, title, body, repeatInterval, callbackData);
	public boolean add(
			String notificationId, 
			long fireDate, 
			String title, 
			String body, 
			String repeatInterval, 
			String callbackData
	) {
		Map<String, Long> repeatDict = new HashMap<String, Long>();
		repeatDict.put("hourly"		, 3600000L); // 1000 x 60 x 60
		repeatDict.put("daily"		, 43200000L); // x 24
		repeatDict.put("weekly"		, 302400000L); // x 7
		repeatDict.put("monthly"	, 1313999712L); // daily x 30.41666
		repeatDict.put("quarterly"	, 3942000000L); // daily x 91.25
		repeatDict.put("yearly"		, 15768000000L); // daily x 365
		
		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		
		date.setTime(fireDate);
		calendar.setTime(date);
		
		long repeatMillis = 0;
		if(repeatDict.containsKey(repeatInterval)) {
			repeatMillis = repeatDict.get(repeatInterval);
		}
		
		boolean result = alarm.addAlarm(
				PLUGIN_PREFIX + id, 
				calendar, 
				title, 
				body, 
				repeatMillis
			);
		
		return result;
	}

	/**
	 * Cancel a specific notification that was previously registered.
	 * 
	 * @param notificationId
	 *            The original ID of the notification that was used when it was
	 *            registered using addNotification()
	 */
	public boolean cancelNotification(int id) {
		Log.d(PLUGIN_NAME, "cancel Notification with id: " + id);

		boolean result = alarm.cancelAlarm(id);

		return result;
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
		
		final boolean result = alarm.cancelAll(alarmSettings);

		return result;
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
	private boolean persistAlarm(int id, JSONArray args) {
		final CordovaInterface cordova = this.cordova;
		
		final Editor alarmSettingsEditor = cordova.getActivity().getBaseContext().getSharedPreferences(
				PLUGIN_NAME, Context.MODE_PRIVATE).edit();

		alarmSettingsEditor.putString(PLUGIN_PREFIX + id, args.toString());

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
	private boolean unpersistAlarm(int id) {
		final CordovaInterface cordova = this.cordova;
		
		final Editor alarmSettingsEditor = cordova.getActivity().getBaseContext().getSharedPreferences(
				PLUGIN_NAME, Context.MODE_PRIVATE).edit();

		alarmSettingsEditor.remove(PLUGIN_PREFIX + id);

		return alarmSettingsEditor.commit();
	}

	/**
	 * Clear all alarms from the Android shared Preferences
	 * 
	 * @return true when successfull, otherwise false
	 */
	private boolean unpersistAlarmAll() {
		final CordovaInterface cordova = this.cordova;
		
		final Editor alarmSettingsEditor = cordova.getActivity().getBaseContext().getSharedPreferences(
				PLUGIN_NAME, Context.MODE_PRIVATE).edit();

		alarmSettingsEditor.clear();

		return alarmSettingsEditor.commit();
	}
}
