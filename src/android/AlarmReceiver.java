package com.phonegap.plugins.localnotification;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.jobclocker.app.R;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
//import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * The alarm receiver is triggered when a scheduled alarm is fired. This class
 * reads the information in the intent and displays this information in the
 * Android notification bar. The notification uses the default notification
 * sound and it vibrates the phone.
 * 
 * @author dvtoever (original author)
 * 
 * @author Wang Zhuochun(https://github.com/zhuochun)
 */
public class AlarmReceiver extends BroadcastReceiver {

	public static final String TITLE = "ALARM_TITLE";
	public static final String SUBTITLE = "ALARM_SUBTITLE";
	public static final String TICKER_TEXT = "ALARM_TICKER";
	public static final String NOTIFICATION_ID = "NOTIFICATION_ID";
	public static final String USER_INFO = "USER_INFO";

	/* Contains time in 24hour format 'HH:mm' e.g. '04:30' or '18:23' */
	public static final String HOUR_OF_DAY = "HOUR_OF_DAY";
	public static final String MINUTE = "MINUTES";
	
	//private Context mContext = null;
	//private ActivityManager mActivityManager = null;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("AlarmReceiver", "AlarmReceiver invoked!");
		//mContext = context;

		final Bundle bundle = intent.getExtras();

		// Retrieve notification details from the intent
		final String tickerText = bundle.getString(TICKER_TEXT);
		final String notificationTitle = bundle.getString(TITLE);
		final String notificationSubText = bundle.getString(SUBTITLE);
		final String notificationId = bundle.getString(NOTIFICATION_ID);
		final String userInfo = bundle.getString(USER_INFO);

		Log.i("AlarmReceiver", "Process alarm with id: " + notificationId);

		Calendar currentCal = Calendar.getInstance();
		int alarmHour = bundle.getInt(HOUR_OF_DAY);
		int alarmMin = bundle.getInt(MINUTE);
		int currentHour = currentCal.get(Calendar.HOUR_OF_DAY);
		int currentMin = currentCal.get(Calendar.MINUTE);

		if (currentHour != alarmHour && currentMin != alarmMin) {
			/*
			 * If you set a repeating alarm at 11:00 in the morning and it
			 * should trigger every morning at 08:00 o'clock, it will
			 * immediately fire. E.g. Android tries to make up for the
			 * 'forgotten' reminder for that day. Therefore we ignore the event
			 * if Android tries to 'catch up'.
			 */
			Log.d("LocalNotification AlarmReceiver", "AlarmReceiver, ignoring alarm since it is due");
			return;
		}
		
		String packageClassName = LocalNotification.getSavedStartupClassName(context);
		Log.i("LocalNotification AlarmReceiver", "'" + packageClassName + "'");
		
		// Get the class name from storage
		Class<?> appClass;
		try {
			appClass = Class.forName( packageClassName );
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		/*
		Method isActiveMethod;
		try {
			isActiveMethod = appClass.getMethod("isActive");
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		RunningAppProcessInfo activity = getActivityIfRunning(packageClassName, context);
		Boolean isActive = false;
		
		if(activity != null) {
			try {
				isActive = (Boolean)isActiveMethod.invoke(activity);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}

		*/
		
		
		//Log.i("LocalNotification AlarmReceiver", "'" + isActive + "'");
		//Log.i("LocalNotification AlarmReceiver", "SAME?: " + (packageClassName.equals(LocalNotification.getContextStartupClassName(context))));
		
		
		//if(packageClassName.equals(LocalNotification.getContextStartupClassName(context))) {
			// The app is currently running, skip the notification
			LocalNotification.getInstance().didReceiveLocalNotification(userInfo);
		//} else {
			Log.i("LocalNotification AlarmReceiver", "Creating Notification");
			
			// Construct the notification and notificationManager objects
			final NotificationManager notificationMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			
			//Create the Intent
			final Intent notificationIntent = new Intent(context, appClass);
			notificationIntent.putExtra("userInfo", userInfo);
			
			//final Intent notificationIntent = new Intent(context, com.jobclocker.app.JobClocker.class);
			final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			// Notification Builder
			// You will need Support Library to use NotificationCompat.Builder
			NotificationCompat.Builder mBuilder =
				    new NotificationCompat.Builder(context)
					    .setSmallIcon(R.drawable.icon)
					    .setContentTitle(notificationTitle)
					    .setContentText(notificationSubText)
					    .setTicker(tickerText)
					    .setDefaults(Notification.DEFAULT_ALL)
					    .setContentIntent(contentIntent)
					    .setAutoCancel(true);
	
			/*
			 * If you want all reminders to stay in the notification bar, you should
			 * generate a random ID. If you want do replace an existing
			 * notification, make sure the ID below matches the ID that you store in
			 * the alarm intent.
			 */
			/*final int id = Integer.parseInt(notificationId.substring(
					com.phonegap.plugins.localnotification.LocalNotification.PLUGIN_PREFIX.length()));
			notificationMgr.notify(id, mBuilder.build());*/
			
			// The Tag/Id pair is unique - in our case the (int)ID remains the same
			notificationMgr.notify(notificationId, 0, mBuilder.build());
		//}
	}
	
    // http://stackoverflow.com/questions/2166961/determining-the-current-foreground-application-from-a-background-task-or-service
	private RunningAppProcessInfo getActivityIfRunning(String packageClassName, Context context) {
	    RunningAppProcessInfo result = null, info = null;
	    ComponentName activity = null;

	    ActivityManager mgr = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
	    
	    List <RunningAppProcessInfo> l = mgr.getRunningAppProcesses();
	    Iterator <RunningAppProcessInfo> i = l.iterator();
	    
	    while(i.hasNext()){
	        info = i.next();
	        activity = getActivityForApp(info, mgr);
	        
	        if(activity != null) {
	        	if(activity.getClassName().equalsIgnoreCase(packageClassName)) {
	        		result = info;
	        		break;
	        	}
	        }
	    }
	    
	    return result;
	}
	
	private ComponentName getActivityForApp(RunningAppProcessInfo target, ActivityManager mgr){
	    ComponentName result=null;
	    ActivityManager.RunningTaskInfo info;

	    if(target==null)
	        return null;

	    List <ActivityManager.RunningTaskInfo> l = mgr.getRunningTasks(9999);
	    Iterator <ActivityManager.RunningTaskInfo> i = l.iterator();

	    while(i.hasNext()){
	        info=i.next();
	        if(info.baseActivity.getPackageName().equals(target.processName)){
	            result=info.topActivity;
	            break;
	        }
	    }

	    return result;
	}
}
