package com.salexvik.viewnet;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GCM {

	private static final String CLASS_NAME = GCM.class.getSimpleName();
	
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	String SENDER_ID = "330011584029"; // номер проекта на Google
	SharedPreferences preferences;
	SharedPreferences.Editor editor;

	Context context;

	GCM(Context ctx){
		context = ctx;
		preferences = context.getSharedPreferences("SAVED_VALUES",Context.MODE_PRIVATE);
		editor = preferences.edit();
	}



	// ѕроверка регистрации устройства на сервисе GCM

	public void checkGcmRegistration(){

		if (checkPlayServices()) {
			// пробуем получить сохраненный регистрационный идентификатор
			Log.i(CLASS_NAME, "ѕроверка регистрации устройства...");
			String sevedRegistrationId = getSavedRegistrationId(context);
			if (sevedRegistrationId.isEmpty()) {
				// если не получилось получить сохраненный регистрационный идентификатор, регистрируемс€...
				Log.i(CLASS_NAME, "–егистраци€ устройства...");
				getRegistrationId();
			}
			else{
				Log.i(CLASS_NAME, "”стройство зарегистрировано, ID=" +sevedRegistrationId );
			}
		} else {
			Log.i(CLASS_NAME, "Ќе найдена правильна€ верси€ Google Play Service");
		}

	}

	// проверка телефона на совместимость с сервисом GCM

	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, (Activity)context,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i(CLASS_NAME, "”стройство не поддерживаетс€");
			}
			return false;
		}
		return true;
	}

	// получение сохраненного регистрационного идентификатора устройства

	private String getSavedRegistrationId(Context context) {
		String savedRegistrationId = preferences.getString(PROPERTY_REG_ID, "");
		if (savedRegistrationId.isEmpty()) {
			Log.i(CLASS_NAME, "—охраненный регистрационный идентификатор не найден");
			return "";
		}
		// проверка на изменение версии приложени€
		int savedRegisteredVersion = preferences.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
		if (savedRegisteredVersion != getAppVersion(context)) {
			Log.i(CLASS_NAME, "¬ерси€ приложени€ изменилась");
			return "";
		}
		return savedRegistrationId;
	}


	// получение версии приложени€

	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}


	// отправл€ем запрос на регистрацию устройства в системе GCM

	public void getRegistrationId(){
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String msg = "";
				try {					
					GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);					
					String regid = gcm.register(SENDER_ID);
					msg = "”стройство зарегистрировано, registration ID=" + regid;					
					storeRegistrationId(regid); // сохран€ем в настройках
				} catch (IOException ex) {
					msg = "ќшибка регистрации устройства: " + ex.getMessage();
				}
				return msg;
			}

			@Override
			protected void onPostExecute(String msg) {
				Log.i(CLASS_NAME, msg);
			}
		}.execute(null, null, null);
	}


	private void storeRegistrationId(String regid) {
		Log.i(CLASS_NAME, "—охранение регистрационного идентификатора дл€ версии приложени€ - " + getAppVersion(context));		
		editor.putString(PROPERTY_REG_ID, regid);
		editor.putInt(PROPERTY_APP_VERSION, getAppVersion(context));
		editor.commit();

	}





}
