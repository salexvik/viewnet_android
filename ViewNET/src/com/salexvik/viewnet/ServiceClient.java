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
	private Socket socket = null; // �����	

	private String vpnConnectionAddress = ""; 
	private String vpnConnectionControlPort = ""; 
	private String vpnConnectionRtspPort = ""; 

	private String idDevice = ""; 
	private String password = ""; 

	private String deviceAddress = "";  // ����� ��� ���������� � �����������
	private String deviceControlPort = "";  // ���� ��� ���������� � �����������
	private String deviceRtspPort = "";  // ���� ��� ���������� � RTSP ��������

	private String socketData  = ""; // ������ � ������� ��������� �� ������
	private String socketPack  = ""; // ����� �������� �� ������
	private DataOutputStream sndtext = null; // ����� ��� �������� ���������     	

	private Timer timer;
	private TimerTask tTask;

	private int checkMsgCnt = 0;
	private String connectionStatus = "disconnected"; // ������ ����������
	private boolean stopRead = false; // ���� ��� �������� ������ ������ �� ������

	DB db; // ���� ������



	// �������� �������

	public void onCreate() {
		super.onCreate();
		Log.d(CLASS_NAME, "onCreate()");

		// ������� ������ ��� ������ � ����� ������
		db = new DB(this, DB.TABLE_NAME_DEVICE_SETTINGS);

		// ������������ � ��
		db.open();

		// �������� ��������� ����������� �� ���� ������ ��� �������� ����������		
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

		connectAndRead(); // ���������� � ����������� � ������ ������
		startConnectControlTimer(); // ������ ������� �������� ����������

	}	


	public void setIdDevice(String aIdDevice){
		idDevice = aIdDevice;
		// ��� ����� ID ����������, ������� ��������� ���������� �����������, ����� �������� �����
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


	// ����������� � ������� (��������� ��� ������ ����������� ������� ����� ������� �������)

	public IBinder onBind(Intent arg0) {
		Log.d(CLASS_NAME, "onBind()");
		return binder;
	}

	// �������� � �������� ������� ServiceClient

	class ClientBinder extends Binder {
		ServiceClient getService() {
			return ServiceClient.this;
		}
	}

	// ���������� �� ������� (���������, ����� ��������� ������ ���������� �� �������)

	public boolean onUnbind(Intent intent) {
		Log.d(CLASS_NAME, "onUnbind()");
		stopSelf(); // ���������� ������
		return true;
	}

	// ��������������� � ������� (��������� ��� ��������� ����������� ������� �������, ����� ���������� ���� ��������)

	public void onRebind(Intent intent) {
		super.onRebind(intent);
		Log.d(CLASS_NAME, "onRebind()");
	}

	// ����������� �������

	public void onDestroy() {
		super.onDestroy();
		Log.d(CLASS_NAME, "onDestroy()");
		timer.cancel();
		socketClose();
		if (db != null) db.close(); // ��������� ����
	}

	// ������ �������� ����������

	void startConnectControlTimer() {
		timer = new Timer(); // ������� ������ ��� �������� ����������
		if (tTask != null) tTask.cancel();
		tTask = new TimerTask() {
			public void run() {

				if(connectionStatus.equals("connecting")){ // ���� � �������� ����������...
					Log.d(CLASS_NAME,"������ �������� ����������: �������� ����������...");  
					getConnectionStatus(); // ���������� �������� �� �������� �����������
				}

				if(socket == null){ // ���� �� ����������.... 
					Log.d(CLASS_NAME,"������ �������� ����������: socket=null");  
					checkMsgCnt = NUM_CHECK_MSG;
				}

				if(NUM_CHECK_MSG == checkMsgCnt){ // ����������������...
					Log.d(CLASS_NAME,"���������");   		
					socketClose();
					connectAndRead(); // �������� �����������
				}
				else{
					checkMsgCnt++; // ����������� ������� ����������� ���������
					sendServer(XML.stringToTag("id", "checkConnect")); // ���������� ����������� ���������
				}    		
			}
		};
		timer.schedule(tTask, INTERVAL_SEND_CHECK_MSG, INTERVAL_SEND_CHECK_MSG);
	}

	// �������� ��������� � ������� ����������

	public void getConnectionStatus(){		
		// ���������� ���������� � ��������� ����������																		
		Intent intent = new Intent("com.salexvik.viewnet.br");
		intent.putExtra("id", "connectionStatus");
		intent.putExtra("connectionStatus", connectionStatus);
		sendBroadcast(intent);
	}


	// ����� ��� ���������� � �������� � ������ ������

	private void connectAndRead(){

		Thread threadToConneciontAndRead = new Thread(new Runnable(){   // ������� �����
			public void run() {
				try {
					Log.d(CLASS_NAME,"����� ������ ��� ����������");
					stopRead = false;
					connectionStatus = "connecting";
					getConnectionStatus(); // ���������� �������� � ������� ����������� � ����������

					// ���� ���������� ���������	
					if(Other.getSsid().equals(SSID)){
						Log.d(CLASS_NAME,"��������� ����������...");
						deviceAddress = LOCAL_CONNECTION_ADDRESS;
						deviceControlPort = LOCAL_CONNECTION_PORT;	
						deviceRtspPort = LOCAL_CONNECTION_RTSP_PORT;
					}    				
					// ���� ���������� ���������
					else{
						Log.d(CLASS_NAME,"��������� ����������...");
						// ��������� ������� ������ ��� ���������� ����������	
						Log.d(CLASS_NAME,"vpnConnectionAddress=" + vpnConnectionAddress + ",vpnConnectionControlPort=" + vpnConnectionControlPort + ",vpnConnectionRtspPort=" + vpnConnectionRtspPort + ";");
						if (vpnConnectionAddress.equals("") || vpnConnectionControlPort.equals("") || vpnConnectionRtspPort.equals("")){

							// ����������� ������ ��� ���������� ���������� 
							Log.d(CLASS_NAME,"������ ������ ��� ���������� ����������...");
							String dataForVpnConnection = null; // ������ � ������� ��� ���������� ����� VPN					
							dataForVpnConnection = HTTPClient.getDataForVPNConnection("http://" + MAIN_SERVER_ADDRESS + "/query.php?val=" +idDevice); 				   								
							Log.d(CLASS_NAME,"������ ��� VPN ����������: " + dataForVpnConnection);
							// ����������� ��������� �����������
							vpnConnectionAddress = XML.getString(dataForVpnConnection, "vpnAddress");
							vpnConnectionControlPort = XML.getString(dataForVpnConnection, "controlPort");
							vpnConnectionRtspPort = XML.getString(dataForVpnConnection, "rtspPort");
							// �������� ���������� ����������
							if(vpnConnectionAddress.equals("")  || vpnConnectionControlPort.equals("") || vpnConnectionRtspPort.equals("")){
								Log.e(CLASS_NAME,"������ ��� VPN ���������� �� ��������!"); 
								connectionStatus = "disconnected";
								return; // ������� �� ������
							} 

							// ��������� ������ ��� ���������� �����������							
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

					socket = new Socket(); // ������� �����						
					socket.connect(new InetSocketAddress(deviceAddress, Integer.parseInt(deviceControlPort)), CONNECTION_TIMEOUT); // ������������ � ����������					

					if (socket.isConnected()){	// ���� ������������ � ����������...
						socket.setKeepAlive(true); // ������ ���� �������� ���������� �� ������ ����
						sndtext = new DataOutputStream(socket.getOutputStream()); // Stream ��� �������� ��������� �� ������					
						BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); // reader ��� ��������� ��������� � ������� 

						Log.d(CLASS_NAME, "���������� � ����������� �����������, �������� ������ ������...");
						checkMsgCnt = 0; // ���������� ������� ����������� ���������
						
						Log.d(CLASS_NAME, "���������� ���������� ������ ��� �������������");											
						sendServer(XML.stringToTag("id", "initializationData"));

						// ���� ��������� ��������� ������ � ������
						while (!stopRead) {
							try {    							
								char[] socketBuf = new char[1024];
								int bytesRead = reader.read(socketBuf);

								// ���� ������ ���������
								if (bytesRead != -1){
									socketData = String.valueOf(socketBuf, 0, bytesRead);

									Log.d(CLASS_NAME, "������ �� ������:" + socketData);
									checkMsgCnt = 0; // ���������� ������� ����������� ���������


									while(getSocketPacket()){ // ��������� ����� �� �������� ������

										String id = XML.getString(socketPack, "id"); // �������� ������������� �������� ������
										
										if(id.equals("initializationSuccessful")){
											connectionStatus = "connected";
											getConnectionStatus(); // ���������� �������� � ���������� � ������ � �����������
											continue;		
										}

										Intent intent = new Intent("com.salexvik.viewnet.br");
										intent.putExtra("id", "socketData");
										intent.putExtra("inData", socketPack);
										sendBroadcast(intent);

									}   
								}
								else{
									Log.e(CLASS_NAME, "���������� ����������� ���� = -1");
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
					Log.e(CLASS_NAME, "�� ������� ���������� IP-����� �������");
					Log.d(CLASS_NAME, "�������� �������� ���������� �����������");
					resetRemoteConnectionSettings();
				} catch (java.net.ConnectException Host) {
					Log.e(CLASS_NAME, "�� ������� ������������ � ����������");
				} catch (IOException e) {
					Log.e(CLASS_NAME, "���������� ��� ������ ������");					
				}
				finally{
					Log.e(CLASS_NAME, "���������� ������ ���������� � ������ ������");
					socketClose();
					connectionStatus = "disconnected";
					getConnectionStatus(); // ���������� �������� � ��������� ����������
				}

			}
		});
		threadToConneciontAndRead.start(); // ��������� ����� ��� ���������� � �������� � ������ ������
	}



	// ����� ����������� �������� ��� ���������� �����������
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


	// �������� ������ �������

	public void sendServer(String aOutDataXML){	

		if (socket == null) return; 

		String pack;

		// ���� ��� �� ������ �� �������� �����, ��������� � ������������ ������ ID ���������� � ������		
		if (!XML.getString(aOutDataXML, "id").equals("checkConnect")){
			aOutDataXML += XML.stringToTag("idDevice", idDevice);
			aOutDataXML += XML.stringToTag("password", password);
		}

		pack = START_TAG + aOutDataXML + END_TAG;

		try {
			Log.d(CLASS_NAME,"������ � �����: " + pack);
			sndtext.write(pack.getBytes()); // ���������� �����
		} 
		catch (Exception e){
			socketClose();
			Log.e(CLASS_NAME,"������ ��� �������� ������ � �����");
			e.printStackTrace();
		}        	     
	}

	// ��������� ������ �� �������� ������

	public boolean getSocketPacket(){

		int posStartTag = 0;
		int posEndTag = 0;

		// ��������� ����� �������� ������...

		if (!socketData.equals("")){ // ���� ��� ������ �������� � ����� ������ �������...
			saveBuf += socketData; // �������� �������� ������ � ����� ��������
			socketData = "";
		}

		// ��������� ������ � ������ ��������...

		posStartTag = saveBuf.indexOf(START_TAG);
		posEndTag = saveBuf.indexOf(END_TAG);

		// ���� ��������� ����� �����...

		if(posStartTag != -1 && posEndTag != -1){
			if (posStartTag < posEndTag){
				socketPack = saveBuf.substring(posStartTag + START_TAG_SIZE, posEndTag); // ��������� �����
				saveBuf = saveBuf.substring(posEndTag + END_TAG_SIZE); // ������� ����������� ����� � ��� ������ ����� ���
				return true;
			} else saveBuf = saveBuf.substring(posEndTag + END_TAG_SIZE); // ������� ������ � ���������� ����������
		}
		return false;		
	}
	
	
	// ������ ��� ��������� ��������������
	
	public void authFailed(final String aInData, Context acontext){
		
		AlertDialog.Builder alert = new AlertDialog.Builder(acontext);			
		alert.setTitle("��������� ��������������");
		alert.setMessage("������:");
		final EditText input = new EditText(acontext);			
		alert.setView(input);
		alert.setPositiveButton("��", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {										
				Editable value = input.getText();						
				String inputPassword = Other.getMd5(value.toString());

				setPassword(inputPassword); // �������� ������� ���������� ������
				// �������� ����������  ������� �� ����������
				String deniedRequest = XML.getString(aInData, "deniedRequest");	
				sendServer(deniedRequest);
			}
		});
		alert.setNegativeButton("������", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// ���� ��������.
			}
		});
		alert.show();
	}


}