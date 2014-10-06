package com.salexvik.viewnet;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
 
public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
	
	private static final String CLASS_NAME = GcmBroadcastReceiver.class.getSimpleName();
  
    @Override
    public void onReceive(Context context, Intent intent) {
    	
    	Log.d(CLASS_NAME, "Получено GCM уведомление");
    	
        // Explicitly specify that GcmIntentService will handle the intent.    	
    	intent.putExtra("receiverName", CLASS_NAME); // помещаем в intent имя вызываемого рецейвера   	
        ComponentName comp = new ComponentName(context.getPackageName(),
                EventIntentService.class.getName());
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
    }
}
