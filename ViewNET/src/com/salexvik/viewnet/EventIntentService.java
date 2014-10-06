package com.salexvik.viewnet;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class EventIntentService extends IntentService {
	public static final int NOTIFICATION_ID = 1;
	private static final String CLASS_NAME = EventIntentService.class.getSimpleName();
	private NotificationManager mNotificationManager;
	private long[] vibrate = { 0, 100, 200, 300 };
	NotificationCompat.Builder builder;


	public EventIntentService() {
		super("EventIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		Bundle extras = intent.getExtras();

		String receiverName = extras.getString("receiverName");
		Log.d(CLASS_NAME, "Получен intent из " +receiverName);
		

		// если пришло GCM уведомление...

		if (receiverName.equals(GcmBroadcastReceiver.class.getSimpleName())){

			GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
			// The getMessageType() intent parameter must be the intent you received
			// in your BroadcastReceiver.
			String messageType = gcm.getMessageType(intent);

			if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
				/*
				 * Filter messages based on message type. Since it is likely that GCM
				 * will be extended in the future with new message types, just ignore
				 * any message types you're not interested in, or that you don't
				 * recognize.
				 */
				if (GoogleCloudMessaging.
						MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
					sendNotification("Send error: " + extras.toString());
				} else if (GoogleCloudMessaging.
						MESSAGE_TYPE_DELETED.equals(messageType)) {
					sendNotification("Deleted messages on server: " +
							extras.toString());
					// If it's a regular GCM message, do some work.
				} else if (GoogleCloudMessaging.
						MESSAGE_TYPE_MESSAGE.equals(messageType)) {

					Log.i(CLASS_NAME, extras.toString());

					// получаем все сообщение
					String gcmBody = extras.getString("message");

					// получаем текст уведомления
					String msg = XML.getString(gcmBody, "msg");

					// получаем дату время СМС уведомление
					String timestamp = XML.getString(gcmBody, "timestamp");

					// получаем идентификатор сообщения
					String idMessage= XML.getString(gcmBody, "idM");

					// получаем идентификатор устройства
					String idDevice = XML.getString(gcmBody, "idD");

					// добавляем в базу
					addTobase(idDevice, idMessage, "GCM", timestamp, msg);
				}
			}	

			// Release the wake lock provided by the WakefulBroadcastReceiver.
			GcmBroadcastReceiver.completeWakefulIntent(intent);

		}


		// если пришло SMS уведомление...


		if (receiverName.equals(SmsBroadcastReceiver.class.getSimpleName())){

			// получаем текст сообщения
			String msg = extras.getString("msg");
			Log.d(CLASS_NAME, "Получео СМС уведомление: " +msg);

			// получаем дату время СМС уведомление
			String timestamp = extras.getString("timestamp");

			// получаем идентификатор сообщения
			String idMessage = extras.getString("idMessage");

			// получаем идентификатор устройства
			String idDevice = extras.getString("idDevice");

			// добавляем в базу
			addTobase(idDevice, idMessage, "SMS", timestamp, msg);

			// Release the wake lock provided by the WakefulBroadcastReceiver.
			SmsBroadcastReceiver.completeWakefulIntent(intent);
		}



	}



	// Добавляем сообщение в базу

	void addTobase(String aIdDevice, String aIdMessage, String aTypeMessage, String aTimestampMessage, String aMessage){
		
		DB db = new DB(this, DB.TABLE_NAME_NOTIFICATIONS); // создаем объект для работы с базой данных		    
		db.open(); // подключаемся к БД	
		
		Cursor cursor;
		
		// Проверяем разрешено ли принимать уведомления от этого устройства...
		int allowNotifications = 0;
		cursor =  db.getSelectionData(DB.TABLE_NAME_DEVICE_SETTINGS, new String[] {DB.COLUMN_ALLOW_NOTIFICATIONS},
				DB.COLUMN_ID_DEVICE + " = ?", new String[] {aIdDevice}); 
		if (cursor.moveToFirst()) {
			allowNotifications = cursor.getInt(cursor.getColumnIndex(DB.COLUMN_ALLOW_NOTIFICATIONS));
		}
		cursor.close();
		
		// если разрешено...
		if (allowNotifications != 0){
			
			cursor = db.getSelectionData(DB.TABLE_NAME_NOTIFICATIONS, null, 
					DB.COLUMN_ID_DEVICE + " = ? AND " +  DB.COLUMN_ID_MSG + " = ?", new String[] {aIdDevice, aIdMessage}); 
			// если сообщений с тиким id еще не существует, добавляем сообщение в базу
			if (cursor.getCount() == 0){	
							
				ContentValues cv = new ContentValues();
				cv.put(DB.COLUMN_ID_MSG, aIdMessage);
				cv.put(DB.COLUMN_TYPE_MSG, aTypeMessage);
				cv.put(DB.COLUMN_TIMESTAMP_MSG, aTimestampMessage);
				cv.put(DB.COLUMN_MSG, aMessage);
				cv.put(DB.COLUMN_ID_DEVICE, aIdDevice);
				db.addRec(cv);	
				
				sendNotification(aMessage); // отправляем уведомление системе
				
			}	
			cursor.close();
		}		
		
		db.close(); // закрываем базу
	}
	


	// отправка уведомления системе

	private void sendNotification(String msg) {
		mNotificationManager = (NotificationManager)
				this.getSystemService(Context.NOTIFICATION_SERVICE);


		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, AlarmsActivity.class), 0);

		Uri sound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.viewnet_sirena);
		
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
		// .setSmallIcon(R.drawable.ic_stat_gcm)
		.setContentTitle("Новое уведомление")
		.setSmallIcon(R.drawable.viewnet_icon)
		.setStyle(new NotificationCompat.BigTextStyle()
		.bigText(msg))
		.setContentText(msg)
		.setVibrate(vibrate)
		.setSound(sound)
		.setAutoCancel(true); // убираем уведомление после нажатия на него

		mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}


}
