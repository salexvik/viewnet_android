package com.salexvik.viewnet;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class SetAlarmsActivity extends ActionBarActivity  {

	private static final String CLASS_NAME = SetAlarmsActivity.class.getSimpleName();
	Context context;	
	boolean bound = false; // ���� ���������� � ��������

	ServiceClient serviceClient; // ������ �������
	ServiceConnection sConn; // ��������� ��� ����������� ��������� �������

	BroadcastReceiver br; // �������� ����������������� ���������

	Button button_testSms;
	Button button_testGcm;
	Button button_testNotification;
	Button button_setPhones;


 
	// �������� activity

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		connectToService(); // ������������ � �������
		context = SetAlarmsActivity.this;

		setContentView(R.layout.viewnet_set_alarms);
		button_testSms = (Button)findViewById(R.id.button_testSms);
		button_testGcm = (Button)findViewById(R.id.button_testGcm);
		button_testNotification = (Button)findViewById(R.id.button_testNotification);
		button_setPhones = (Button)findViewById(R.id.button_setPhones);

	}


	// �������������� ��������

	public void onResume() {
		super.onResume();	
		broadcastReceiving(); // ������� �������� ����������������� ��������� 
		if(bound){
			serviceClient.getConnectionStatus(); // �������� ������ ����������			
		}
	}

	// ������������ ��������

	public void onPause() {
		super.onPause();	
		unregisterReceiver(br); // ��������� �������� ����������������� ���������
	}

	// ����������� ��������

	public void onDestroy() {
		super.onDestroy();
		// ����������� �� �������
		if (bound){
			unbindService(sConn);
			bound = false;
		}
	}


	// ����������� � �������

	private void connectToService(){
		Intent intent = new Intent(this, ServiceClient.class);
		sConn = new ServiceConnection() {
			public void onServiceConnected(ComponentName name, IBinder binder) {
				Log.d(CLASS_NAME, "onServiceConnected()");
				serviceClient = ((ServiceClient.ClientBinder) binder).getService(); 
				bound = true; 
				serviceClient.getConnectionStatus(); // �������� ������ ����������
			}
			public void onServiceDisconnected(ComponentName name) {
				Log.d(CLASS_NAME, "onServiceDisconnected()");
				bound = false;
			}
		}; 				
		startService(intent); // ��������� ������
		bindService(intent, sConn, 0); // ������������ � �������		
	}





	// ���������� onClick

	public void onClick(View v) {

		switch (v.getId()) {


		case R.id.button_testSms:			
			serviceClient.sendServer(XML.stringToTag("id", "testSms"));
			break;

		case R.id.button_testGcm:			
			serviceClient.sendServer(XML.stringToTag("id", "testGcm"));
			break;

		case R.id.button_testNotification:			
			serviceClient.sendServer(XML.stringToTag("id", "testNotification"));
			break;

		case R.id.button_setPhones:			
			// �������� activity �������� ���������� ������� ��� ��� �����������
			Intent intent = new Intent(context, SetPhonesActivity.class);
			startActivity(intent);
			break;


		}

	}







	// ��������� ������ �� �������

	private void broadcastReceiving(){

		// ������� BroadcastReceiver
		br = new BroadcastReceiver() {
			// �������� ��� ��������� ���������
			public void onReceive(Context context, Intent intent) {

				if(intent.getStringExtra("id").equals("connectionStatus")){

					String connectionStatus = intent.getStringExtra("connectionStatus"); 

					// ����������� �����������
					if(connectionStatus.equals("connecting")){
						getSupportActionBar().setLogo(R.drawable.circle_yellow);
					}

					// ���������� �����������
					if(connectionStatus.equals("connected")){									
						getSupportActionBar().setLogo(R.drawable.circle_green);
					}

					// ��� ����������
					if(connectionStatus.equals("disconnected")){
						getSupportActionBar().setLogo(R.drawable.circle_red);
					}
				}	



				if(intent.getStringExtra("id").equals("socketData")){

					String inData = intent.getStringExtra("inData"); // �������� ������ � ��������� ������� 					
					String id = XML.getString(inData, "id"); // �������� ������������� �������� ������


					// ����������� � ��������� ��������������
					if(id.equals("authenticationFailed")){
						Log.e(CLASS_NAME, "�������������� �� ��������");							
						serviceClient.authFailed(inData, context); 		
					}


				}



			}
		};
		// ������� ������ ��� BroadcastReceiver
		IntentFilter intFilt = new IntentFilter("com.salexvik.viewnet.br");
		// ������������ (��������) BroadcastReceiver
		registerReceiver(br, intFilt);
	}	


}
