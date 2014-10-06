package com.salexvik.viewnet;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.videolan.vlc.VLCApplication;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class Other {
	
//	private static final String CLASS_NAME = Other.class.getSimpleName();

	private static final String mCHAR = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
	private static final int STR_LENGTH = 9; // длина генерируемой строки

	static Random random = new Random(); 


	// получение хеша пароля MD5

	public static String getMd5(String in) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
			digest.reset();
			digest.update(in.getBytes());
			byte[] a = digest.digest();
			int len = a.length;
			StringBuilder sb = new StringBuilder(len << 1);
			for (int i = 0; i < len; i++) {
				sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
				sb.append(Character.forDigit(a[i] & 0x0f, 16));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}




	// функция для генерации случайной строки

	public static String getRandomString() {
		StringBuffer randStr = new StringBuffer();
		for (int i = 0; i < STR_LENGTH; i++) {
			int number = getRandomNumber();
			char ch = mCHAR.charAt(number);
			randStr.append(ch);
		}
		return randStr.toString();
	}

	private static int getRandomNumber() {
		int randomInt = 0;
		randomInt = random.nextInt(mCHAR.length());
		if (randomInt - 1 == -1) {
			return randomInt;
		} else {
			return randomInt - 1;
		}
	}


	public static void showMessage(String aTitle, String aMessage, Context aContext){

		AlertDialog.Builder alert = new AlertDialog.Builder(aContext);			
		alert.setTitle(aTitle);
		alert.setMessage(aMessage);			
		alert.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {										

			}
		});
		alert.show();					

	}


	
	// получение SSID Wi-Fi соединения
	public static String getSsid() {
		
		String ssid;
		
		WifiManager wifiManager = (WifiManager) VLCApplication.getAppContext().getSystemService(Context.WIFI_SERVICE);
		   WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		
		ssid = wifiInfo.getSSID();		
		if (ssid != null){
			int posFirst = ssid.indexOf('"');
			int posLast = ssid.lastIndexOf('"');
			if (posFirst != -1 && posLast != -1){
				// избавляемся от кавычек
				ssid = ssid.substring(posFirst + 1, posLast);
				return ssid;
			}
		}
		   
		return "";
	}
	
	
	

	// получение IP адреса Wi-Fi соединения
	public static String getIpAddr() {

		WifiManager wifiManager = (WifiManager) VLCApplication.getAppContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();

		String ipString = String.format(Locale.getDefault(),
				"%d.%d.%d.%d",
				(ip & 0xff),
				(ip >> 8 & 0xff),
				(ip >> 16 & 0xff),
				(ip >> 24 & 0xff));

		Log.d("IP address",""+ipString);

		return ipString;
	}


	// Проверка активити на нахождение в топе

	public static boolean checkTopActivity(String aCheckActivityClassName, Context context){		
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> taskInfo = am.getRunningTasks(1);

		String topActivity = taskInfo.get(0).topActivity.getClassName();
		if (topActivity.equals(aCheckActivityClassName)) return true;

		return false;

	}






}
