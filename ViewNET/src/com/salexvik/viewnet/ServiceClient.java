package com.salexvik.viewnet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.text.Editable;
import android.util.Log;
import android.widget.EditText;


public class ServiceClient extends Service {

	private static final String MAIN_SERVER_ADDRESS = "elektroserver.webhop.net";
	private static final String LOCAL_CONNECTION_ADDRESS = "192.168.1.1";
	private static final String SSID = "ViewNET";
	private static final String LOCAL_CONNECTION_PORT = "8000";
	private static final String LOCAL_CONNECTION_RTSP_PORT = "8554";
	private static final String CLASS_NAME = ServiceClient.class.getSimpleName();
	private static final String START_TAG = "<packet>";
	private static final String END_TAG = "</packet>";
	private static final int START_TAG_SIZE = 8;
	private static final int END_TAG_SIZE = 9;
	private static final int INTERVAL_SEND_CHECK_MSG = 10000;
	private static final int NUM_CHECK_MSG = 3;
	private static final int CONNECTION_TIMEOUT = 3000;

	NotificationManager nm;
	private ClientBinder binder = new ClientBinder();

	private String saveBuf = ""; 	
	private Socket socket = null; // сокет	

	private String vpnConnectionAddress = ""; 
	private String vpnConnectionControlPort = ""; 
	private String vpnConnectionRtspPort = ""; 

	private String idDevice = ""; 
	private String password = ""; 

	private String deviceAddress = "";  // адрес для соединения с устройством
	private String deviceControlPort = "";  // порт для соединения с устройством
	private String deviceRtspPort = "";  // порт для соединения с RTSP сервером

	private String socketData  = ""; // строка с данными принятыми из сокета
	private String socketPack  = ""; // пакет принятый из сокета
	private DataOutputStream sndtext = null; // поток для отправки сообщений     	

	private Timer timer;
	private TimerTask tTask;

	private int checkMsgCnt = 0;
	private String connectionStatus = "disconnected"; // статус соединения
	private boolean stopRead = false; // флаг для останова чтения данных из сокета

	DB db; // база данных



	// создание сервиса

	public void onCreate() {
		super.onCreate();
		Log.d(CLASS_NAME, "onCreate()");

		// создаем объект для работы с базой данных
		db = new DB(this, DB.TABLE_NAME_DEVICE_SETTINGS);

		// подключаемся к БД
		db.open();

		// получаем настройки подключения из базы данных для текущего устройства		
		Cursor c =  db.getSelectionData(DB.TABLE_NAME_DEVICE_SETTINGS, null,
				DB.COLUMN_CURRENT_DEVICE + " = 1", null); 
		if (c.moveToFirst()) {
			idDevice = c.getString(c.getColumnIndex(DB.COLUMN_ID_DEVICE));
			password = c.getString(c.getColumnIndex(DB.COLUMN_PASSWORD));				
			vpnConnectionAddress = c.getString(c.getColumnIndex(DB.COLUMN_VPN_CONNECTION_ADDRESS));
			vpnConnectionControlPort = c.getString(c.getColumnIndex(DB.COLUMN_VPN_CONNECTION_CONTROL_PORT));
			vpnConnectionRtspPort = c.getString(c.getColumnIndex(DB.COLUMN_VPN_CONNECTION_RTSP_PORT));
		}
		c.close();

		connectAndRead(); // соединение с устройством и чтение данных
		startConnectControlTimer(); // запуск таймера контроля соединения

	}	


	public void setIdDevice(String aIdDevice){
		idDevice = aIdDevice;
		// при смене ID устройства, удаляем настройки удаленного подключения, чтобы получить новые
		resetRemoteConnectionSettings();
	}


	public void setPassword(String aPassword){
		password = aPassword;
		ContentValues cv = new ContentValues();
		cv.put(DB.COLUMN_PASSWORD, aPassword);
		db.updateRec(cv, DB.COLUMN_ID_DEVICE + " = ?", new String[] {idDevice});
	}	

	public String getIdDevice(){
		return idDevice;
	}

	public String getPassword(){
		return password;
	}

	public String getDeviceAddress(){
		return deviceAddress;
	}

	public String getDeviceControlPort(){
		return deviceControlPort;
	}

	public String getDeviceRtspPort(){
		return deviceRtspPort;
	}


	public boolean isSocket(){
		if(socket != null){
			return true;
		}
		else{
			return false;
		}
	}


	public void socketClose(){
		try{
			if (socket != null){
				socket.close();	
				socket = null;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}


	// подключение к сервису (возникает при первом подключении клиента после запуска сервиса)

	public IBinder onBind(Intent arg0) {
		Log.d(CLASS_NAME, "onBind()");
		return binder;
	}

	// передача в активити объекта ServiceClient

	class ClientBinder extends Binder {
		ServiceClient getService() {
			return ServiceClient.this;
		}
	}

	// отключение от сервиса (возникает, когда последния клиент отключился от сервиса)

	public boolean onUnbind(Intent intent) {
		Log.d(CLASS_NAME, "onUnbind()");
		stopSelf(); // уничтожаем сервис
		return true;
	}

	// переподключение к сервису (возникает при повторном подключении первого клиента, после отключения всех клиентов)

	public void onRebind(Intent intent) {
		super.onRebind(intent);
		Log.d(CLASS_NAME, "onRebind()");
	}

	// уничтожение сервиса

	public void onDestroy() {
		super.onDestroy();
		Log.d(CLASS_NAME, "onDestroy()");
		timer.cancel();
		socketClose();
		if (db != null) db.close(); // закрываем базу
	}

	// таймер контроля соединения

	void startConnectControlTimer() {
		timer = new Timer(); // создаем таймер для контроля соединения
		if (tTask != null) tTask.cancel();
		tTask = new TimerTask() {
			public void run() {

				if(connectionStatus.equals("connecting")){ // если в процессе соединения...
					Log.d(CLASS_NAME,"Таймер контроля соединения: ожидание соединения...");  
					getConnectionStatus(); // уведомляем активити об ожидании подключения
				}

				if(socket == null){ // если не подключены.... 
					Log.d(CLASS_NAME,"Таймер контроля соединения: socket=null");  
					checkMsgCnt = NUM_CHECK_MSG;
				}

				if(NUM_CHECK_MSG == checkMsgCnt){ // переподключаемся...
					Log.d(CLASS_NAME,"реконнект");   		
					socketClose();
					connectAndRead(); // пытаемся соединиться
				}
				else{
					checkMsgCnt++; // увеличиваем счетчик проверочных сообщений
					sendServer(XML.stringToTag("id", "checkConnect")); // отправляем проверочное сообщение
				}    		
			}
		};
		timer.schedule(tTask, INTERVAL_SEND_CHECK_MSG, INTERVAL_SEND_CHECK_MSG);
	}

	// отправка сообщений о статусе соединения

	public void getConnectionStatus(){		
		// отправляем информацию о состояние соединения																		
		Intent intent = new Intent("com.salexvik.viewnet.br");
		intent.putExtra("id", "connectionStatus");
		intent.putExtra("connectionStatus", connectionStatus);
		sendBroadcast(intent);
	}


	// поток для соединение с сервером и чтения данных

	private void connectAndRead(){

		Thread threadToConneciontAndRead = new Thread(new Runnable(){   // создаем поток
			public void run() {
				try {
					Log.d(CLASS_NAME,"старт потока для соединения");
					stopRead = false;
					connectionStatus = "connecting";
					getConnectionStatus(); // уведомляем активити о попытки подключения к устройству

					// если соединение локальное	
					if(Other.getSsid().equals(SSID)){
						Log.d(CLASS_NAME,"Локальное соединение...");
						deviceAddress = LOCAL_CONNECTION_ADDRESS;
						deviceControlPort = LOCAL_CONNECTION_PORT;	
						deviceRtspPort = LOCAL_CONNECTION_RTSP_PORT;
					}    				
					// если соединение удаленное
					else{
						Log.d(CLASS_NAME,"Удаленное соединение...");
						// проверяем наличие данных для удаленного соединения	
						Log.d(CLASS_NAME,"vpnConnectionAddress=" + vpnConnectionAddress + ",vpnConnectionControlPort=" + vpnConnectionControlPort + ",vpnConnectionRtspPort=" + vpnConnectionRtspPort + ";");
						if (vpnConnectionAddress.equals("") || vpnConnectionControlPort.equals("") || vpnConnectionRtspPort.equals("")){

							// запрашиваем данные для удаленного соединения 
							Log.d(CLASS_NAME,"Запрос данных для удаленного соединения...");
							String dataForVpnConnection = null; // строка с данными для соединения через VPN					
							dataForVpnConnection = HTTPClient.getDataForVPNConnection("http://" + MAIN_SERVER_ADDRESS + "/query.php?val=" +idDevice); 				   								
							Log.d(CLASS_NAME,"Данные для VPN соединения: " + dataForVpnConnection);
							// настраиваем удаленное подключение
							vpnConnectionAddress = XML.getString(dataForVpnConnection, "vpnAddress");
							vpnConnectionControlPort = XML.getString(dataForVpnConnection, "controlPort");
							vpnConnectionRtspPort = XML.getString(dataForVpnConnection, "rtspPort");
							// проверка полученных параметров
							if(vpnConnectionAddress.equals("")  || vpnConnectionControlPort.equals("") || vpnConnectionRtspPort.equals("")){
								Log.e(CLASS_NAME,"Данные для VPN соединения не получены!"); 
								connectionStatus = "disconnected";
								return; // выходим из потока
							} 

							// сохраняем данные для удаленного подключения							
							ContentValues cv = new ContentValues();
							cv.put(DB.COLUMN_VPN_CONNECTION_ADDRESS, vpnConnectionAddress);
							cv.put(DB.COLUMN_VPN_CONNECTION_CONTROL_PORT, vpnConnectionControlPort);
							cv.put(DB.COLUMN_VPN_CONNECTION_RTSP_PORT, vpnConnectionRtspPort);							
							db.updateRec(cv, DB.COLUMN_ID_DEVICE + " = ?", new String[] {idDevice});
						}

						deviceAddress = vpnConnectionAddress;
						deviceControlPort = vpnConnectionControlPort;
						deviceRtspPort = vpnConnectionRtspPort;

					}	

					socket = new Socket(); // создаем сокет						
					socket.connect(new InetSocketAddress(deviceAddress, Integer.parseInt(deviceControlPort)), CONNECTION_TIMEOUT); // подключаемся к устройству					

					if (socket.isConnected()){	// если подключились к устройству...
						socket.setKeepAlive(true); // ставим флаг контроля соединения на уровне ядра
						sndtext = new DataOutputStream(socket.getOutputStream()); // Stream для отправки сообщений на сервер					
						BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); // reader для получения сообщений с сервера 

						Log.d(CLASS_NAME, "Соединение с устройством установлено, начинаем чтение данных...");
						checkMsgCnt = 0; // сбрасываем счетчик проверочных сообщений
						
						Log.d(CLASS_NAME, "Отправляем устройству данные для инициализации");											
						sendServer(XML.stringToTag("id", "initializationData"));

						// цикл постоянно считывает данные с сокета
						while (!stopRead) {
							try {    							
								char[] socketBuf = new char[1024];
								int bytesRead = reader.read(socketBuf);

								// если пришло сообщение
								if (bytesRead != -1){
									socketData = String.valueOf(socketBuf, 0, bytesRead);

									Log.d(CLASS_NAME, "Данные из сокета:" + socketData);
									checkMsgCnt = 0; // сбрасываем счетчик проверочных сообщений


									while(getSocketPacket()){ // извлекаем пакет из принятых данных

										String id = XML.getString(socketPack, "id"); // получаем идентификатор принятых данных
										
										if(id.equals("initializationSuccessful")){
											connectionStatus = "connected";
											getConnectionStatus(); // уведомляем активити о готовности к работе с устройством
											continue;		
										}

										Intent intent = new Intent("com.salexvik.viewnet.br");
										intent.putExtra("id", "socketData");
										intent.putExtra("inData", socketPack);
										sendBroadcast(intent);

									}   
								}
								else{
									Log.e(CLASS_NAME, "Количество прочитанных байт = -1");
									stopRead = true;
								}
							}
							finally{
							}
						}
					}
					connectionStatus = "disconnected";
				}
				catch (UnknownHostException UnknownHost) {
					Log.e(CLASS_NAME, "Не удалось определить IP-адрес сервера");
					Log.d(CLASS_NAME, "Удаление настроек удаленного подключения");
					resetRemoteConnectionSettings();
				} catch (java.net.ConnectException Host) {
					Log.e(CLASS_NAME, "Не удалось подключиться к устройству");
				} catch (IOException e) {
					Log.e(CLASS_NAME, "Исключение при работе сокета");					
				}
				finally{
					Log.e(CLASS_NAME, "Завершение потока соединения и чтения данных");
					socketClose();
					connectionStatus = "disconnected";
					getConnectionStatus(); // уведомляем активити о состоянии соединения
				}

			}
		});
		threadToConneciontAndRead.start(); // запускаем поток для соединения с сервером и чтения данных
	}



	// сброс сохраненных настроек для удаленного подключения
	private void resetRemoteConnectionSettings(){
		vpnConnectionAddress = "";
		vpnConnectionControlPort = "";
		vpnConnectionRtspPort = "";
		ContentValues cv = new ContentValues();
		cv.put(DB.COLUMN_VPN_CONNECTION_ADDRESS, vpnConnectionAddress);
		cv.put(DB.COLUMN_VPN_CONNECTION_CONTROL_PORT, vpnConnectionControlPort);
		cv.put(DB.COLUMN_VPN_CONNECTION_RTSP_PORT, vpnConnectionRtspPort);							
		db.updateRec(cv, DB.COLUMN_ID_DEVICE + " = ?", new String[] {idDevice});
	}


	// Отправка данных серверу

	public void sendServer(String aOutDataXML){	

		if (socket == null) return; 

		String pack;

		// если это не запрос на проверку связи, добавляем к отправляемым данным ID устройства и пароль		
		if (!XML.getString(aOutDataXML, "id").equals("checkConnect")){
			aOutDataXML += XML.stringToTag("idDevice", idDevice);
			aOutDataXML += XML.stringToTag("password", password);
		}

		pack = START_TAG + aOutDataXML + END_TAG;

		try {
			Log.d(CLASS_NAME,"Данные в сокет: " + pack);
			sndtext.write(pack.getBytes()); // отправляем пакет
		} 
		catch (Exception e){
			socketClose();
			Log.e(CLASS_NAME,"Ошибка при отправки данных в сокет");
			e.printStackTrace();
		}        	     
	}

	// выделение пакета из принятых данных

	public boolean getSocketPacket(){

		int posStartTag = 0;
		int posEndTag = 0;

		// обработка вновь принятых данных...

		if (!socketData.equals("")){ // если это первая итерация в цикле вызова функции...
			saveBuf += socketData; // помещаем принятые данные в буфер хранения
			socketData = "";
		}

		// обработка данных в буфере хранения...

		posStartTag = saveBuf.indexOf(START_TAG);
		posEndTag = saveBuf.indexOf(END_TAG);

		// если обнаружен целый пакет...

		if(posStartTag != -1 && posEndTag != -1){
			if (posStartTag < posEndTag){
				socketPack = saveBuf.substring(posStartTag + START_TAG_SIZE, posEndTag); // извлекаем пакет
				saveBuf = saveBuf.substring(posEndTag + END_TAG_SIZE); // удаляем извлеченный пакет и все данные перед ним
				return true;
			} else saveBuf = saveBuf.substring(posEndTag + END_TAG_SIZE); // удаляем данные с нарушенной структурой
		}
		return false;		
	}
	
	
	// диалог при неудачной аутентификации
	
	public void authFailed(final String aInData, Context acontext){
		
		AlertDialog.Builder alert = new AlertDialog.Builder(acontext);			
		alert.setTitle("Требуется аутентификация");
		alert.setMessage("Пароль:");
		final EditText input = new EditText(acontext);			
		alert.setView(input);
		alert.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {										
				Editable value = input.getText();						
				String inputPassword = Other.getMd5(value.toString());

				setPassword(inputPassword); // передаем сервису измененный пароль
				// повторно отправляем  команду на устройство
				String deniedRequest = XML.getString(aInData, "deniedRequest");	
				sendServer(deniedRequest);
			}
		});
		alert.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Если отменили.
			}
		});
		alert.show();
	}


}