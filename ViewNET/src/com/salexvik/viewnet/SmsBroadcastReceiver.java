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
		Log.d(CLASS_NAME, "�������� SMS");
		if (intent != null && intent.getAction() != null &&
				ACTION.compareToIgnoreCase(intent.getAction()) == 0) {
			Object[] pduArray = (Object[]) intent.getExtras().get("pdus");
			SmsMessage[] messages = new SmsMessage[pduArray.length];
			for (int i = 0; i < pduArray.length; i++) {
				messages[i] = SmsMessage.createFromPdu((byte[]) pduArray[i]);
			}
			
			// �������� ����� �����������
			String sms_from = messages[0].getDisplayOriginatingAddress();
			Log.d(CLASS_NAME, "����� �����������: " +sms_from);
			
			// �������� ����� ���������
			StringBuilder bodyText = new StringBuilder();
			for (int i = 0; i < messages.length; i++) {
				bodyText.append(messages[i].getMessageBody());
			}
			String smsBody = bodyText.toString();
			Log.d(CLASS_NAME, "C��������: " +smsBody);
			
			// �������� ������������� ���������� �� ������ ���������
			String idDevice = XML.getString(smsBody, "idD");
			
			// � ������ ��������� ��������� �������������� ����������, 
			// ������ �����, ��� ��� ��������� ������������� ����� ���������
			if (!idDevice.equals("")) {
				
				// �������� ����� �����������
				String msg = XML.getString(smsBody, "msg");
				
				// �������� id �����������
				String idMessage = XML.getString(smsBody, "idM");
				
				// �������� ���� � ����� ���������
				long timestamp = messages[0].getTimestampMillis();			
				SimpleDateFormat s = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss",Locale.getDefault());
				String timestampString = s.format(timestamp); // ����������� � ������
				
				
				intent.putExtra("receiverName", CLASS_NAME); // �������� � intent ��� ����������� ��������� 
				intent.putExtra("msg", msg); // �������� � intent ����� sms �����������
				intent.putExtra("idMessage", idMessage); // �������� � intent id ���������
				intent.putExtra("timestamp", timestampString); // �������� � intent ���� � ����� ���������
				intent.putExtra("idDevice", idDevice); // �������� � intent ������������� ����������
				
				ComponentName comp = new ComponentName(context.getPackageName(),
		                EventIntentService.class.getName());
				startWakefulService(context, (intent.setComponent(comp)));
		        setResultCode(Activity.RESULT_OK);
				

				abortBroadcast(); // ��������� ���������� broadcast
			}
		}
	}
}
