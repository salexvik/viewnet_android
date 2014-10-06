package com.salexvik.viewnet;

import java.text.SimpleDateFormat;
import java.util.Locale;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsBroadcastReceiver extends WakefulBroadcastReceiver {
	private static final String CLASS_NAME = SmsBroadcastReceiver.class.getSimpleName();
	private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(CLASS_NAME, "ѕолучено SMS");
		if (intent != null && intent.getAction() != null &&
				ACTION.compareToIgnoreCase(intent.getAction()) == 0) {
			Object[] pduArray = (Object[]) intent.getExtras().get("pdus");
			SmsMessage[] messages = new SmsMessage[pduArray.length];
			for (int i = 0; i < pduArray.length; i++) {
				messages[i] = SmsMessage.createFromPdu((byte[]) pduArray[i]);
			}
			
			// получаем номер отправител€
			String sms_from = messages[0].getDisplayOriginatingAddress();
			Log.d(CLASS_NAME, "Ќомер отправител€: " +sms_from);
			
			// получаем текст сообщени€
			StringBuilder bodyText = new StringBuilder();
			for (int i = 0; i < messages.length; i++) {
				bodyText.append(messages[i].getMessageBody());
			}
			String smsBody = bodyText.toString();
			Log.d(CLASS_NAME, "Cообщение: " +smsBody);
			
			// получаем идентификатор устройства из текста сообщени€
			String idDevice = XML.getString(smsBody, "idD");
			
			// в случае успешного получени€ идентификатора устройства, 
			// делаем вывод, что это сообщение предназначено нашей программе
			if (!idDevice.equals("")) {
				
				// получаем текст уведомлени€
				String msg = XML.getString(smsBody, "msg");
				
				// получаем id уведомлени€
				String idMessage = XML.getString(smsBody, "idM");
				
				// получаем дату и врем€ сообщени€
				long timestamp = messages[0].getTimestampMillis();			
				SimpleDateFormat s = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss",Locale.getDefault());
				String timestampString = s.format(timestamp); // преобразуем в строку
				
				
				intent.putExtra("receiverName", CLASS_NAME); // помещаем в intent им€ вызываемого рецейвера 
				intent.putExtra("msg", msg); // помещаем в intent текст sms уведомлени€
				intent.putExtra("idMessage", idMessage); // помещаем в intent id сообщени€
				intent.putExtra("timestamp", timestampString); // помещаем в intent дату и врем€ сообщени€
				intent.putExtra("idDevice", idDevice); // помещаем в intent идентификатор устройства
				
				ComponentName comp = new ComponentName(context.getPackageName(),
		                EventIntentService.class.getName());
				startWakefulService(context, (intent.setComponent(comp)));
		        setResultCode(Activity.RESULT_OK);
				

				abortBroadcast(); // прерываем дальнейший broadcast
			}
		}
	}
}
