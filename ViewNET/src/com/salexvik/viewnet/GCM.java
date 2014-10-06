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
	String SENDER_ID = "330011584029"; // ����� ������� �� Google
	SharedPreferences preferences;
	SharedPreferences.Editor editor;

	Context context;

	GCM(Context ctx){
		context = ctx;
		preferences = context.getSharedPreferences("SAVED_VALUES",Context.MODE_PRIVATE);
		editor = preferences.edit();
	}



	// �������� ����������� ���������� �� ������� GCM

	public void checkGcmRegistration(){

		if (checkPlayServices()) {
			// ������� �������� ����������� ��������������� �������������
			Log.i(CLASS_NAME, "�������� ����������� ����������...");
			String sevedRegistrationId = getSavedRegistrationId(context);
			if (sevedRegistrationId.isEmpty()) {
				// ���� �� ���������� �������� ����������� ��������������� �������������, ��������������...
				Log.i(CLASS_NAME, "����������� ����������...");
				getRegistrationId();
			}
			else{
				Log.i(CLASS_NAME, "���������� ����������������, ID=" +sevedRegistrationId );
			}
		} else {
			Log.i(CLASS_NAME, "�� ������� ���������� ������ Google Play Service");
		}

	}

	// �������� �������� �� ������������� � �������� GCM

	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, (Activity)context,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i(CLASS_NAME, "���������� �� ��������������");
			}
			return false;
		}
		return true;
	}

	// ��������� ������������ ���������������� �������������� ����������

	private String getSavedRegistrationId(Context context) {
		String savedRegistrationId = preferences.getString(PROPERTY_REG_ID, "");
		if (savedRegistrationId.isEmpty()) {
			Log.i(CLASS_NAME, "����������� ��������������� ������������� �� ������");
			return "";
		}
		// �������� �� ��������� ������ ����������
		int savedRegisteredVersion = preferences.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
		if (savedRegisteredVersion != getAppVersion(context)) {
			Log.i(CLASS_NAME, "������ ���������� ����������");
			return "";
		}
		return savedRegistrationId;
	}


	// ��������� ������ ����������

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


	// ���������� ������ �� ����������� ���������� � ������� GCM

	public void getRegistrationId(){
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String msg = "";
				try {					
					GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);					
					String regid = gcm.register(SENDER_ID);
					msg = "���������� ����������������, registration ID=" + regid;					
					storeRegistrationId(regid); // ��������� � ����������
				} catch (IOException ex) {
					msg = "������ ����������� ����������: " + ex.getMessage();
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
		Log.i(CLASS_NAME, "���������� ���������������� �������������� ��� ������ ���������� - " + getAppVersion(context));		
		editor.putString(PROPERTY_REG_ID, regid);
		editor.putInt(PROPERTY_APP_VERSION, getAppVersion(context));
		editor.commit();

	}





}
