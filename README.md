#PhoneGap 3.x Local Notifications

Supported Platforms:

 - Android
 - iOS

##Setup - iOS

1. In `CordovaLib/Classes/CDVPlugin.m` uncomment the following line in `initWithWebView`

		[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(didReceiveLocalNotification:) name:CDVLocalNotification object:nil];

2. In `CordovaLib/Classes/CDVPlugin.m` uncomment the following block at the end of the file

		- (void)didReceiveLocalNotification:(NSNotification *)notification {}

3. In `CordovaLib/Classes/CDVPlugin.h` uncomment the following line

		 - (void)didReceiveLocalNotification:(NSNotification *)notification;

4. In `Classes/AppDelegate.m` add the following to the `didFinishLaunchingWithOptions:` delegate

	UILocalNotification *notification = [launchOptions objectForKey:UIApplicationLaunchOptionsLocalNotificationKey];
    if (notification) {
        [[self.viewController settings] setValue:notification forKey:@"LaunchOptionsLocalNotificationKey"];
    }

5. In `Classes/MainViewController.m` add the following to the `webViewDidFinishLoad:` delegate

	UILocalNotification *notification = [[self settings] objectForKey:@"LaunchOptionsLocalNotificationKey"];
    if(notification) {
        [theWebView stringByEvaluatingJavaScriptFromString:[NSString stringWithFormat:@"setTimeout(function(){steal.dev.log('NOTIFICATION!!! - %@');}, 5000);", [notification.userInfo objectForKey:@"notificationId"]]];
        
        [[NSNotificationCenter defaultCenter] postNotificationName:CDVLocalNotification object:notification];
    }