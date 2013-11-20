/*
 * Cordova/PhoneGap 3.0.0+ LocalNotification Plugin
 * Original author: Olivier Lesnicki
 */
 
 function triggerEvent(elem, evtName, evtData) {
	 var evt;
	 evtData = evtData || {bubbles:true, cancelable:true, detail:undefined};
	 
	 if(window.CustomEvent) {
		 evt = new CustomEvent(evtName, evtData);
	 } else {
		 evt = document.createEvent( 'CustomEvent' );
		 evt.initCustomEvent( evtName, evtData.bubbles, evtData.cancelable, evtData.detail );
	 }
	 
	 elem.dispatchEvent(evt);
 }

//------------------------------------------------------------------------------
// object that we're exporting
//------------------------------------------------------------------------------
var localNotifier = module.exports;

localNotifier.REPEAT_INTERVAL = {
	"HOURLY"	: "hourly",
	"DAILY"		: "daily",
	"WEEKLY"	: "weekly",
	"MONTHLY"	: "monthly",
	"QUARTERLY"	: "quarterly",
	"YEARLY"	: "yearly",
};
localNotifier.queue = [];
localNotifier.flushQueue = function( callback ) {
	var n = localNotifier.queue.length;
	while(n--) {
		callback(this.queue.shift());
	}
};

localNotifier.receiveNotification = function(data) {
	// Delay needed to give "resume" time to finish
	window.setTimeout(function() {
		localNotifier.queue.push(data);
		triggerEvent(document, "notification-receive", {bubbles:true, cancelable:true, detail:data});
	}, 10);
};
localNotifier.addNotification = function(success, fail, options) {
	steal.dev.log("Adding Notification with options: " + JSON.stringify(options));
	
	if( Object.prototype.toString.call(options.fireDate) === '[object Date]' ) {
		options.fireDate = Math.round(options.fireDate.getTime()/1000);
	}
	
	if( !options.fireDate ) {
		options.fireDate = 0;
	}
	
	if( !options.alertBody && options.alertBody != 0) {
		options.alertBody = "";
	}
	
	options.repeatInterval = localNotifier.REPEAT_INTERVAL[ ('' + options.repeatInterval).toUpperCase() ];
	if( !options.repeatInterval ) {
		options.repeatInterval = 0;
	}
	
	if( !options.intervalId && options.intervalId != 0 ) {
		options.intervalId = "localnotification_" + Math.round(Math.random() * 100000000);
	}
	
	// "null" is valid json where "undefined" is not
	if( !options.callbackData && options.callbackData != 0 ) {
		options.callbackData = "null";
	}
	
	cordova.exec(success, fail, "LocalNotification", "addNotification", [
		options.fireDate,
		options.alertBody,
		options.repeatInterval,
		options.intervalId,
		options.callbackData
	]);
};

/*
//------------------------------------------------------------------------------
localNotifier.addNotification = function(options) {
        
    var defaults = {
                
        fireDate        : new Date(new Date().getTime() + 5000),
        alertBody       : "This is a local notification.",
        repeatInterval  : "0" ,
        soundName       : "horn.caf" ,
        badge           : 0  ,
        notificationId  : 1  ,
        background      : function(notificationId){},
        foreground      : function(notificationId){}                
    };
        
    if(options){
        for (var key in defaults) {
            if (typeof options[key] !== "undefined"){
            defaults[key] = options[key];
            }
        }
    }
    
    if (typeof defaults.fireDate == 'object') {
        defaults.fireDate = Math.round(defaults.fireDate.getTime()/1000);
    }
        
    cordova.exec(
        function(params) {
            window.setTimeout(function(){
                if(typeof defaults.foreground == 'function'){
                  if(params.appState == "active") {
                    defaults.foreground(params.notificationId);
                    return;
                  }
                }
                if(typeof defaults.background == 'function'){
                  if(params.appState != "active") {
                    defaults.background(params.notificationId);
                    return;
                  }
                }
            }, 1);
        }, 
        function(err){
          console.log("ERROR in cordova.exec");
          console.log(err);
        }, 
        "LocalNotification" , 
        "addNotification"   , 
        [
            defaults.fireDate        ,
            defaults.alertBody       ,
            defaults.repeatInterval  ,
            defaults.soundName       ,
            defaults.notificationId
        ]
    );
                    
};
*/

//------------------------------------------------------------------------------  
localNotifier.cancelNotification = function(str, callback) {
    cordova.exec(callback, null, "LocalNotification", "cancelNotification", [str]);
};

//------------------------------------------------------------------------------  
localNotifier.cancelAllNotifications = function(callback) {
    cordova.exec(callback, null, "LocalNotification", "cancelAllNotifications", []);
};
