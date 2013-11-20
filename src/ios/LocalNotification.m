/*!
 * Cordova 2.3.0+ LocalNotification plugin
 * Original author: Olivier Lesnicki
 */

#import "LocalNotification.h"
#import <Cordova/CDV.h>

@implementation LocalNotification

-(void)addNotification:(CDVInvokedUrlCommand*)command {
        
    NSMutableDictionary *repeatDict = [[NSMutableDictionary alloc] init];
    [repeatDict setObject:[NSNumber numberWithInt:NSHourCalendarUnit    ] forKey:@"hourly"  ];
    [repeatDict setObject:[NSNumber numberWithInt:NSDayCalendarUnit     ] forKey:@"daily"   ];
    [repeatDict setObject:[NSNumber numberWithInt:NSWeekCalendarUnit    ] forKey:@"weekly"  ];
    [repeatDict setObject:[NSNumber numberWithInt:NSMonthCalendarUnit   ] forKey:@"monthly" ];
    [repeatDict setObject:[NSNumber numberWithInt:NSQuarterCalendarUnit ] forKey:@"quarterly" ];
    [repeatDict setObject:[NSNumber numberWithInt:NSYearCalendarUnit    ] forKey:@"yearly"  ];
    [repeatDict setObject:[NSNumber numberWithInt:0] forKey:@""         ];
    
    UILocalNotification* notif = [[UILocalNotification alloc] init];

	double fireDate             = [[command.arguments objectAtIndex:0] doubleValue];
    NSString *alertBody         =  [command.arguments objectAtIndex:1];
    NSNumber *repeatInterval    =  [command.arguments objectAtIndex:2];
    //NSString *soundName         =  [command.arguments objectAtIndex:3];
    NSString *notificationId    =  [command.arguments objectAtIndex:3];
    NSString *callbackData    =  [command.arguments objectAtIndex:4];
    
    notif.alertBody         = ([alertBody isEqualToString:@""]) ? nil : alertBody;
    notif.fireDate          = [NSDate dateWithTimeIntervalSince1970:fireDate];
    notif.repeatInterval    = [[repeatDict objectForKey:repeatInterval] intValue];
    //notif.soundName         = soundName;
    notif.timeZone          = [NSTimeZone defaultTimeZone];
    
	notif.userInfo = [NSDictionary dictionaryWithObjectsAndKeys:
                                notificationId    , @"notificationId",
                                callbackData, @"callbackData",
                                nil
                              ];
    
    CDVPluginResult* pluginResult = nil;
    
    @try {
        [[UIApplication sharedApplication] scheduleLocalNotification:notif];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK  messageAsString:notificationId];
        [pluginResult setKeepCallback:[NSNumber numberWithBool:YES]];
    } @catch (NSException* exception) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR  messageAsString:[exception reason]];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)cancelNotification:(CDVInvokedUrlCommand*)command {
    
    NSString *notificationId    = [command.arguments objectAtIndex:0];
	NSArray *notifications      = [[UIApplication sharedApplication] scheduledLocalNotifications];
    
	for (UILocalNotification *notification in notifications) {
        
		NSString *notId = [notification.userInfo objectForKey:@"notificationId"];
        
		if ([notificationId isEqualToString:notId]) {
			[[UIApplication sharedApplication] cancelLocalNotification: notification];
		}
        
	}

}

- (void)cancelAllNotifications:(CDVInvokedUrlCommand*)command {
    
    [[UIApplication sharedApplication] cancelAllLocalNotifications];
 
}

- (void)didReceiveLocalNotification:(NSNotification *)notification
{
    UILocalNotification* uiNotification  = [notification object];

    // return a javascript object with notification userInfo
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:uiNotification.userInfo options:0 error:nil];
    NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    NSString *jsStatement = [NSString stringWithFormat:@"document.addEventListener('deviceready', function() { localNotifier.receiveNotification(%@); });", jsonString];
    
    [self writeJavascript:jsStatement];
}


@end